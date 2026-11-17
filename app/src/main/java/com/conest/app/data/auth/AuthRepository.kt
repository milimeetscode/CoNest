package com.conest.app.data.auth

import android.content.Context
import kotlinx.coroutines.flow.Flow

data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

/**
 * A failure already phrased for people. [userMessage] is null when there's nothing to say —
 * e.g. the person dismissed the Google account sheet themselves.
 */
class AuthError(val userMessage: String?, cause: Throwable? = null) : Exception(userMessage, cause)

interface AuthRepository {
    /** False until app/google-services.json is added; nothing else here works without it. */
    val isConfigured: Boolean

    /** Emits the signed-in user, or null. Persists across launches (PRD §5.1). */
    val currentUser: Flow<AuthUser?>

    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun logIn(email: String, password: String): Result<Unit>

    /** Needs an Activity context — the Credential Manager sheet is shown over it. */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun signOut()

    /**
     * Firebase only lets an account delete itself shortly after signing in. Check this first,
     * so we never wipe someone's data and then fail to remove the account itself.
     */
    fun signedInRecently(): Boolean

    /** Deletes the Firebase Auth account. Call after the person's data is removed. */
    suspend fun deleteAccount(): Result<Unit>
}
