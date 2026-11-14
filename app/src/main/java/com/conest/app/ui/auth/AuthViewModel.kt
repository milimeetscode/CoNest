package com.conest.app.ui.auth

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthError
import com.conest.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

/**
 * Talks to [AuthRepository] only. Success needs no navigation here: the root observes the
 * auth state and routes the newly signed-in user to onboarding or home.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null, notice = null) }

    fun clearMessages() = _state.update { it.copy(error = null, notice = null) }

    fun submit(mode: AuthMode, email: String, password: String) = run {
        if (mode == AuthMode.SignUp) auth.signUp(email, password) else auth.logIn(email, password)
    }

    fun google(activityContext: Context) = run { auth.signInWithGoogle(activityContext) }

    fun resetPassword() {
        val email = _state.value.email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.update { it.copy(error = "Enter your email above first, and we'll send you a reset link.", notice = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null, notice = null) }
            val result = auth.sendPasswordReset(email)
            // Worded so it doesn't reveal whether the email has an account (Firebase's email
            // enumeration protection also answers success for unknown addresses).
            _state.update {
                if (result.isSuccess) {
                    it.copy(isSubmitting = false, notice = "If there's an account for $email, a reset link is on its way.")
                } else {
                    it.copy(isSubmitting = false, error = (result.exceptionOrNull() as? AuthError)?.userMessage)
                }
            }
        }
    }

    private fun run(block: suspend () -> Result<Unit>) {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null, notice = null) }
            val result = block()
            _state.update {
                it.copy(isSubmitting = false, error = (result.exceptionOrNull() as? AuthError)?.userMessage)
            }
        }
    }
}
