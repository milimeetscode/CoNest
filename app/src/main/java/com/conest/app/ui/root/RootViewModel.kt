package com.conest.app.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.auth.AuthUser
import com.conest.app.data.model.UserProfile
import com.conest.app.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface RootState {
    data object Loading : RootState
    data object NotConfigured : RootState
    data object SignedOut : RootState
    data class SignedIn(val user: AuthUser, val profile: UserProfile?) : RootState
    data class ProfileUnavailable(val user: AuthUser) : RootState
}

/**
 * Decides where the app starts: signed out → welcome; signed in → their profile is fetched
 * first, then onboarding, the quiz, or home depending on how far they got.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RootViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    val state: StateFlow<RootState> =
        if (!auth.isConfigured) {
            MutableStateFlow(RootState.NotConfigured)
        } else {
            retry.flatMapLatest { auth.currentUser }
                .flatMapLatest { user ->
                    if (user == null) {
                        flowOf(RootState.SignedOut)
                    } else {
                        profiles.observeProfile(user.uid)
                            .map<UserProfile?, RootState> { RootState.SignedIn(user, it) }
                            .catch { emit(RootState.ProfileUnavailable(user)) }
                    }
                }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootState.Loading)
        }

    fun retry() { retry.value++ }
    fun signOut() = auth.signOut()
}
