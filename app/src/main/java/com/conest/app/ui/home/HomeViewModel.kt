package com.conest.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.model.UserProfile
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.domain.lifestyle.LifestyleAnswers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val answers: LifestyleAnswers? = null,
)

/** The returning-user landing: retrieves the signed-in person's profile and lifestyle answers. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = auth.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            profiles.observeProfile(user.uid).map { profile ->
                // Answers change rarely; re-read alongside each profile update (e.g. after a retake).
                HomeUiState(loading = false, profile = profile, answers = profiles.getLifestyleAnswers(user.uid))
            }
        }
        .catch { emit(HomeUiState(loading = false)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun signOut() = auth.signOut()
}
