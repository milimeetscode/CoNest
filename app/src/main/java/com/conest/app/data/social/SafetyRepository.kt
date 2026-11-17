package com.conest.app.data.social

import com.conest.app.data.profile.ProfileRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

class SafetyError(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

/** Report, block and unblock (PRD §5.9). */
interface SafetyRepository {
    suspend fun report(me: String, other: String, reason: ReportReason, details: String): Result<Unit>

    /** Hides each person from the other's Discover and closes any conversation between them. */
    suspend fun block(me: String, other: String): Result<Unit>

    suspend fun unblock(me: String, other: String): Result<Unit>

    suspend fun blockedByMe(me: String): List<BlockedPerson>
}

@Singleton
class FirestoreSafetyRepository @Inject constructor(
    private val profiles: ProfileRepository,
    private val discovery: DiscoveryRepository,
) : SafetyRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }

    override suspend fun report(me: String, other: String, reason: ReportReason, details: String): Result<Unit> = runCatching {
        db.collection("reports").add(
            mapOf(
                "reportedBy" to me,
                "reportedUserId" to other,
                "reason" to reason.key,
                "description" to details.trim().take(1000),
                "status" to "open",
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        Unit
    }.recoverCatching { throw SafetyError("Your report didn't go through. Please try again.", it) }

    override suspend fun block(me: String, other: String): Result<Unit> = runCatching {
        val matchRef = db.collection("matches").document(matchIdFor(me, other))
        // Readable only to participants: a refusal here just means there's no match to close.
        val match = runCatching { matchRef.get().await() }.getOrNull()

        db.runBatch { b ->
            b.set(
                db.collection("blocks").document("${me}_$other"),
                mapOf("blockerId" to me, "blockedId" to other, "createdAt" to FieldValue.serverTimestamp()),
            )
            if (match?.exists() == true && match.getString("status") != "blocked") {
                b.update(matchRef, mapOf("status" to "blocked", "blockedBy" to me))
            }
        }.await()
        discovery.reset() // re-read exclusions so they disappear from Discover straight away
        Unit
    }.recoverCatching { throw SafetyError("Couldn't block right now. Please try again.", it) }

    override suspend fun unblock(me: String, other: String): Result<Unit> = runCatching {
        val matchRef = db.collection("matches").document(matchIdFor(me, other))
        val match = runCatching { matchRef.get().await() }.getOrNull()
        db.runBatch { b ->
            b.delete(db.collection("blocks").document("${me}_$other"))
            if (match?.exists() == true && match.getString("blockedBy") == me) {
                b.update(matchRef, mapOf("status" to "active", "blockedBy" to FieldValue.delete()))
            }
        }.await()
        discovery.reset()
        Unit
    }.recoverCatching { throw SafetyError("Couldn't unblock right now. Please try again.", it) }

    override suspend fun blockedByMe(me: String): List<BlockedPerson> = coroutineScope {
        val uids = runCatching {
            db.collection("blocks").whereEqualTo("blockerId", me).get().await().documents.mapNotNull { it.getString("blockedId") }
        }.getOrDefault(emptyList())
        uids.map { uid -> async { BlockedPerson(uid, profiles.getProfile(uid)) } }.awaitAll()
    }
}
