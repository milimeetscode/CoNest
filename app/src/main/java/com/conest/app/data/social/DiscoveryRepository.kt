package com.conest.app.data.social

import com.conest.app.data.model.UserProfile
import com.conest.app.data.model.UserType
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.data.profile.toProfile
import com.conest.app.domain.lifestyle.CompatibilityCalculator
import com.conest.app.domain.lifestyle.LifestyleAnswers
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class DiscoveryPage(val candidates: List<Candidate>, val exhausted: Boolean)

interface DiscoveryRepository {
    /** Forget paging and exclusions — call when the viewer or their city changes. */
    fun reset()

    /**
     * The next batch of people the viewer hasn't seen, best lifestyle fit first.
     * Excludes: themselves, anyone already liked/passed/hidden, blocks in either direction,
     * paused profiles, and (for people with a room) other people with rooms.
     */
    suspend fun nextPage(me: UserProfile, myAnswers: LifestyleAnswers?): DiscoveryPage

    /**
     * Records a like or pass. On a like, tries to open the match; Firestore rules only allow
     * that when the other person has already liked back, so a refusal simply means "not yet".
     */
    suspend fun swipe(me: String, other: String, like: Boolean): Result<SwipeOutcome>
}

@Singleton
class FirestoreDiscoveryRepository @Inject constructor(
    private val profiles: ProfileRepository,
) : DiscoveryRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val lock = Mutex()

    private var forUid: String? = null
    private var excluded: MutableSet<String>? = null
    private var cursor: DocumentSnapshot? = null
    private var exhausted = false

    override fun reset() {
        forUid = null; excluded = null; cursor = null; exhausted = false
    }

    override suspend fun nextPage(me: UserProfile, myAnswers: LifestyleAnswers?): DiscoveryPage = lock.withLock {
        if (forUid != me.uid) { reset(); forUid = me.uid }
        val skip = excluded ?: loadExclusions(me.uid).also { excluded = it }

        val found = mutableListOf<UserProfile>()
        var rounds = 0
        // Keep paging until we have a handful to show, or run out of people.
        while (!exhausted && found.size < MIN_BATCH && rounds < MAX_ROUNDS) {
            rounds++
            var query = db.collection("users")
                .whereEqualTo("city", me.city)
                .whereEqualTo("profileComplete", true)
                .whereEqualTo("quizComplete", true)
                .limit(PAGE_SIZE.toLong())
            cursor?.let { query = query.startAfter(it) }
            val snap = query.get().await()
            if (snap.isEmpty) { exhausted = true; break }
            cursor = snap.documents.last()
            if (snap.size() < PAGE_SIZE) exhausted = true

            found += snap.documents.map { it.toProfile() }.filter { p ->
                p.uid != me.uid && p.uid !in skip && p.discoverable && fitsIntent(me, p)
            }
        }

        val candidates = coroutineScope {
            found.map { p ->
                async {
                    val answers = profiles.getLifestyleAnswers(p.uid)
                    val score = if (myAnswers != null && answers != null) CompatibilityCalculator.calculate(myAnswers, answers) else null
                    Candidate(p, answers, score)
                }
            }.awaitAll()
        }.sortedByDescending { it.compatibility?.score ?: -1 }

        candidates.forEach { skip += it.profile.uid } // never hand out the same person twice
        DiscoveryPage(candidates, exhausted)
    }

    override suspend fun swipe(me: String, other: String, like: Boolean): Result<SwipeOutcome> = runCatching {
        db.collection("swipes").document("${me}_$other").set(
            mapOf(
                "fromUserId" to me,
                "toUserId" to other,
                "action" to if (like) "like" else "pass",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        excluded?.add(other)

        if (!like) return@runCatching SwipeOutcome(null)

        val id = matchIdFor(me, other)
        val (a, b) = if (me < other) me to other else other to me
        try {
            db.collection("matches").document(id).set(
                mapOf(
                    "userIds" to listOf(a, b),
                    "status" to "active",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            SwipeOutcome(id)
        } catch (e: FirebaseFirestoreException) {
            // Rules refuse the match unless both likes exist — that's the normal "not mutual yet".
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) SwipeOutcome(null) else throw e
        }
    }

    private suspend fun loadExclusions(me: String): MutableSet<String> = coroutineScope {
        val swiped = async { db.collection("swipes").whereEqualTo("fromUserId", me).get().await().documents.mapNotNull { it.getString("toUserId") } }
        val iBlocked = async { db.collection("blocks").whereEqualTo("blockerId", me).get().await().documents.mapNotNull { it.getString("blockedId") } }
        val blockedMe = async { db.collection("blocks").whereEqualTo("blockedId", me).get().await().documents.mapNotNull { it.getString("blockerId") } }
        (swiped.await() + iBlocked.await() + blockedMe.await()).toMutableSet()
    }

    /** Room-havers are looking for someone to move in; seekers can meet both kinds. */
    private fun fitsIntent(me: UserProfile, other: UserProfile): Boolean =
        !(me.userType == UserType.HasRoom && other.userType == UserType.HasRoom)

    private companion object {
        const val PAGE_SIZE = 25
        const val MIN_BATCH = 6
        const val MAX_ROUNDS = 6
    }
}
