package com.conest.app.data.social

import com.conest.app.data.model.UserProfile
import com.conest.app.domain.lifestyle.Compatibility
import com.conest.app.domain.lifestyle.LifestyleAnswers
import java.util.Date

/** A person shown in Discover, with how well their lifestyle lines up with the viewer's. */
data class Candidate(
    val profile: UserProfile,
    val answers: LifestyleAnswers?,
    /** Null when either quiz is incomplete — we never show a misleading score. */
    val compatibility: Compatibility?,
)

/** Result of liking someone: [matchId] is set only when the like was mutual. */
data class SwipeOutcome(val matchId: String?)

enum class MatchStatus { Active, Blocked }

/**
 * A mutual match, which is also the conversation container (`matches/{id}/messages`).
 * [other] is the other participant's profile, or null if their account was deleted.
 */
data class Match(
    val id: String,
    val otherUid: String,
    val other: UserProfile?,
    val status: MatchStatus,
    val blockedByMe: Boolean,
    val createdAt: Date?,
    val lastMessage: String?,
    val lastMessageAt: Date?,
    val lastSenderId: String?,
    val unread: Boolean,
) {
    val hasConversation: Boolean get() = lastMessageAt != null
    val sortTime: Date? get() = lastMessageAt ?: createdAt
}

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val sentAt: Date?,
    /** Written locally, not yet confirmed by the server. */
    val pending: Boolean,
)

enum class ReportReason(val label: String, val key: String) {
    Fake("Fake profile or photos", "fake"),
    Harassment("Harassment or abuse", "harassment"),
    Inappropriate("Inappropriate content", "inappropriate"),
    Scam("Scam or asking for money", "scam"),
    Other("Something else", "other"),
}

/** Someone the viewer has blocked — for the list in Settings. */
data class BlockedPerson(val uid: String, val profile: UserProfile?)

/** Deterministic match/chat id: the two uids, sorted, joined. Prevents duplicate matches. */
fun matchIdFor(a: String, b: String): String = if (a < b) "${a}_$b" else "${b}_$a"
