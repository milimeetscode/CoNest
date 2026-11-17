package com.conest.app.data.social

import com.conest.app.data.model.UserProfile
import com.conest.app.data.profile.ProfileRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

class ChatError(val userMessage: String, cause: Throwable? = null) : Exception(userMessage, cause)

interface MatchRepository {
    /** All of the viewer's matches, live, most recent activity first. */
    fun observeMatches(me: String): Flow<List<Match>>

    fun observeMatch(me: String, matchId: String): Flow<Match?>

    /** The conversation, oldest first, live. Includes the viewer's not-yet-sent messages. */
    fun observeMessages(matchId: String): Flow<List<ChatMessage>>

    suspend fun send(me: String, matchId: String, text: String): Result<Unit>

    suspend fun markRead(me: String, matchId: String)
}

@Singleton
class FirestoreMatchRepository @Inject constructor(
    private val profiles: ProfileRepository,
) : MatchRepository {

    private val db by lazy { FirebaseFirestore.getInstance() }
    private val profileCache = mutableMapOf<String, UserProfile?>()
    private val cacheLock = Mutex()

    override fun observeMatches(me: String): Flow<List<Match>> = channelFlow {
        val reg = db.collection(MATCHES).whereArrayContains("userIds", me).addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            val docs = snap?.documents ?: return@addSnapshotListener
            launch {
                val matches = docs.map { it.toMatch(me, profileFor(it.otherUid(me))) }
                    .sortedByDescending { it.sortTime ?: Date(Long.MAX_VALUE) } // brand-new (pending) first
                send(matches)
            }
        }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override fun observeMatch(me: String, matchId: String): Flow<Match?> = channelFlow {
        val reg = db.collection(MATCHES).document(matchId).addSnapshotListener { snap, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            if (snap == null) return@addSnapshotListener
            launch { send(if (snap.exists()) snap.toMatch(me, profileFor(snap.otherUid(me))) else null) }
        }
        awaitClose { reg.remove() }
    }.distinctUntilChanged()

    override fun observeMessages(matchId: String): Flow<List<ChatMessage>> = callbackFlow {
        val reg = db.collection(MATCHES).document(matchId).collection(MESSAGES)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(300)
            .addSnapshotListener { snap, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val msgs = snap?.documents?.map { d ->
                    ChatMessage(
                        id = d.id,
                        senderId = d.getString("senderId").orEmpty(),
                        text = d.getString("text").orEmpty(),
                        // Pending server timestamps get a local estimate so new messages sort correctly.
                        sentAt = d.getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate(),
                        pending = d.metadata.hasPendingWrites(),
                    )
                }?.sortedBy { it.sentAt ?: Date(Long.MAX_VALUE) } ?: return@addSnapshotListener
                trySend(msgs)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun send(me: String, matchId: String, text: String): Result<Unit> {
        val clean = text.trim()
        if (clean.isEmpty()) return Result.success(Unit)
        return runCatching {
            val match = db.collection(MATCHES).document(matchId)
            val batch = db.batch()
            batch.set(
                match.collection(MESSAGES).document(),
                mapOf("senderId" to me, "text" to clean.take(MAX_LENGTH), "type" to "text", "createdAt" to FieldValue.serverTimestamp()),
            )
            batch.update(
                match,
                mapOf(
                    "lastMessage" to clean.take(140),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastSenderId" to me,
                    "lastRead.$me" to FieldValue.serverTimestamp(),
                ),
            )
            // Not awaited for delivery: Firestore applies it locally at once and syncs in the
            // background, so a message typed offline still appears and sends when back online.
            batch.commit()
            Unit
        }.recoverCatching { throw ChatError("Your message couldn't be sent. Please try again.", it) }
    }

    override suspend fun markRead(me: String, matchId: String) {
        runCatching {
            db.collection(MATCHES).document(matchId).update("lastRead.$me", FieldValue.serverTimestamp())
        }
    }

    private suspend fun profileFor(uid: String): UserProfile? {
        cacheLock.withLock { if (profileCache.containsKey(uid)) return profileCache[uid] }
        val p = profiles.getProfile(uid)
        cacheLock.withLock { profileCache[uid] = p }
        return p
    }

    private companion object {
        const val MATCHES = "matches"
        const val MESSAGES = "messages"
        const val MAX_LENGTH = 2000
    }
}

private fun DocumentSnapshot.otherUid(me: String): String =
    (get("userIds") as? List<*>)?.filterIsInstance<String>()?.firstOrNull { it != me }.orEmpty()

@Suppress("UNCHECKED_CAST")
private fun DocumentSnapshot.toMatch(me: String, other: UserProfile?): Match {
    val lastMessageAt = getTimestamp("lastMessageAt", ServerTimestampBehavior.ESTIMATE)?.toDate()
    val lastSender = getString("lastSenderId")
    val myRead = ((get("lastRead", ServerTimestampBehavior.ESTIMATE) as? Map<String, Any?>)?.get(me) as? Timestamp)?.toDate()
    val status = if (getString("status") == "blocked") MatchStatus.Blocked else MatchStatus.Active
    return Match(
        id = id,
        otherUid = otherUid(me),
        other = other,
        status = status,
        blockedByMe = status == MatchStatus.Blocked && getString("blockedBy") == me,
        createdAt = getTimestamp("createdAt", ServerTimestampBehavior.ESTIMATE)?.toDate(),
        lastMessage = getString("lastMessage"),
        lastMessageAt = lastMessageAt,
        lastSenderId = lastSender,
        unread = lastMessageAt != null && lastSender != me && (myRead == null || myRead.before(lastMessageAt)),
    )
}
