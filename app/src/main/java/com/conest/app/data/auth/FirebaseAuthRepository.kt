package com.conest.app.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : AuthRepository {

    override val isConfigured: Boolean = FirebaseApp.getApps(appContext).isNotEmpty()

    // Only touched when configured — FirebaseAuth.getInstance() throws otherwise.
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override val currentUser: Flow<AuthUser?> =
        if (!isConfigured) {
            flowOf(null)
        } else {
            callbackFlow {
                val listener = FirebaseAuth.AuthStateListener { fa ->
                    trySend(fa.currentUser?.let { AuthUser(it.uid, it.email, it.displayName, it.photoUrl?.toString()) })
                }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }.distinctUntilChanged()
        }

    override suspend fun signUp(email: String, password: String) = guarded {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun logIn(email: String, password: String) = guarded {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun sendPasswordReset(email: String) = guarded {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = guarded {
        // Generated into resources by the google-services plugin; looked up by name so the
        // project still compiles before Firebase is configured.
        val clientIdRes = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        if (clientIdRes == 0) {
            throw AuthError("Google sign-in isn't set up yet. Enable it in the Firebase console, then download google-services.json again.")
        }
        val option = GetSignInWithGoogleOption.Builder(appContext.getString(clientIdRes)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthError(null, e)
        } catch (e: NoCredentialException) {
            throw AuthError("No Google account found on this device. Add one in Settings and try again.", e)
        } catch (e: GetCredentialException) {
            throw AuthError("Google sign-in didn't go through. Please try again.", e)
        }

        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthError("Google sign-in returned something unexpected. Please try again.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun signOut() {
        if (!isConfigured) return
        auth.signOut()
        // Also forget the Google account choice so the next sign-in shows the picker again.
        scope.launch { runCatching { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) } }
    }

    override fun signedInRecently(): Boolean {
        if (!isConfigured) return false
        val last = auth.currentUser?.metadata?.lastSignInTimestamp ?: return false
        return System.currentTimeMillis() - last < RECENT_LOGIN_WINDOW_MS
    }

    override suspend fun deleteAccount(): Result<Unit> = guarded {
        try {
            auth.currentUser?.delete()?.await()
        } catch (e: com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
            throw AuthError("For your security, log out and back in, then delete your account.", e)
        }
        scope.launch { runCatching { CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest()) } }
    }

    private suspend fun guarded(block: suspend () -> Unit): Result<Unit> {
        if (!isConfigured) return Result.failure(AuthError("CoNest isn't connected to Firebase yet."))
        return try {
            block()
            Result.success(Unit)
        } catch (e: AuthError) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AuthError(friendly(e), e))
        }
    }

    private fun friendly(e: Exception): String = when {
        // Project-side misconfiguration (Authentication never enabled in the console). Not the
        // user's fault, so say so plainly rather than "try again".
        e.message.orEmpty().contains("CONFIGURATION_NOT_FOUND") ->
            "Sign-in isn't switched on for CoNest yet. (Enable Authentication in the Firebase console.)"
        else -> friendlyByType(e)
    }

    private companion object {
        const val RECENT_LOGIN_WINDOW_MS = 4 * 60 * 1000L
    }

    private fun friendlyByType(e: Exception): String = when (e) {
        is FirebaseAuthUserCollisionException -> "There's already an account with that email. Try logging in instead."
        is FirebaseAuthWeakPasswordException -> "That password is too easy to guess. Try something longer."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "That email and password don't match. Try again, or reset your password."
        is FirebaseNetworkException -> "You seem to be offline. Check your connection and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Give it a minute and try again."
        is FirebaseAuthException -> when (e.errorCode) {
            "ERROR_OPERATION_NOT_ALLOWED" -> "This sign-in method isn't enabled for CoNest yet."
            else -> "Something went wrong signing you in. Please try again."
        }
        else -> "Something went wrong. Please try again."
    }
}
