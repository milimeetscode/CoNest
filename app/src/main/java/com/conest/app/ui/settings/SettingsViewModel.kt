package com.conest.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthError
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.profile.ProfileError
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.data.social.BlockedPerson
import com.conest.app.data.social.SafetyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val email: String = "",
    val discoverable: Boolean = true,
    val blocked: List<BlockedPerson> = emptyList(),
    val confirmingDelete: Boolean = false,
    val deleting: Boolean = false,
    val needsFreshLogin: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val safety: SafetyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val user = auth.currentUser.filterNotNull().first()
            _state.update { it.copy(email = user.email.orEmpty()) }
            launch { profiles.observeProfile(user.uid).collect { p -> if (p != null) _state.update { it.copy(discoverable = p.discoverable) } } }
            _state.update { it.copy(blocked = safety.blockedByMe(user.uid)) }
        }
    }

    fun setDiscoverable(on: Boolean) = viewModelScope.launch {
        val uid = auth.currentUser.filterNotNull().first().uid
        _state.update { it.copy(discoverable = on, error = null) }
        profiles.setDiscoverable(uid, on).onFailure { e ->
            _state.update { it.copy(discoverable = !on, error = (e as? ProfileError)?.userMessage) }
        }
    }

    fun unblock(uid: String) = viewModelScope.launch {
        val me = auth.currentUser.filterNotNull().first().uid
        safety.unblock(me, uid).onSuccess { _state.update { s -> s.copy(blocked = s.blocked.filterNot { it.uid == uid }) } }
    }

    fun signOut() = auth.signOut()

    fun askDelete() = _state.update {
        // Firebase only allows deleting an account soon after signing in; check before we touch any data.
        if (auth.signedInRecently()) it.copy(confirmingDelete = true, needsFreshLogin = false, error = null)
        else it.copy(needsFreshLogin = true, confirmingDelete = false, error = null)
    }

    fun cancelDelete() = _state.update { it.copy(confirmingDelete = false, needsFreshLogin = false) }

    fun deleteAccount() = viewModelScope.launch {
        val uid = auth.currentUser.filterNotNull().first().uid
        _state.update { it.copy(deleting = true, error = null) }
        val data = profiles.deleteAccountData(uid)
        if (data.isFailure) {
            _state.update { it.copy(deleting = false, error = (data.exceptionOrNull() as? ProfileError)?.userMessage) }
            return@launch
        }
        auth.deleteAccount().onFailure { e ->
            _state.update { it.copy(deleting = false, error = (e as? AuthError)?.userMessage) }
        }
        // On success the auth state clears and the app returns to the welcome screen.
    }
}
