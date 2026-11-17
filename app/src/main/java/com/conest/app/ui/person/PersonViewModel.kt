package com.conest.app.ui.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.model.UserProfile
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.data.social.ReportReason
import com.conest.app.data.social.SafetyError
import com.conest.app.data.social.SafetyRepository
import com.conest.app.domain.lifestyle.Compatibility
import com.conest.app.domain.lifestyle.CompatibilityCalculator
import com.conest.app.domain.lifestyle.LifestyleAnswers
import com.conest.app.ui.components.SafetyDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonUiState(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val answers: LifestyleAnswers? = null,
    val myInterests: List<String> = emptyList(),
    val compatibility: Compatibility? = null,
    val dialog: SafetyDialog = SafetyDialog.None,
    val busy: Boolean = false,
    val error: String? = null,
    /** Set once a report or block succeeds, so the screen can leave and say so. */
    val doneMessage: String? = null,
    val blocked: Boolean = false,
)

@HiltViewModel
class PersonViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val safety: SafetyRepository,
) : ViewModel() {

    val uid: String = checkNotNull(savedState["uid"])
    private val _state = MutableStateFlow(PersonUiState())
    val state: StateFlow<PersonUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val me = auth.currentUser.filterNotNull().first().uid
            val other = async { profiles.getProfile(uid) }
            val theirs = async { profiles.getLifestyleAnswers(uid) }
            val mine = async { profiles.getLifestyleAnswers(me) }
            val myProfile = async { profiles.getProfile(me) }
            val a = mine.await()
            val b = theirs.await()
            _state.update {
                it.copy(
                    loading = false,
                    profile = other.await(),
                    answers = b,
                    myInterests = myProfile.await()?.interests.orEmpty(),
                    compatibility = if (a != null && b != null) CompatibilityCalculator.calculate(a, b) else null,
                )
            }
        }
    }

    fun openDialog(d: SafetyDialog) = _state.update { it.copy(dialog = d, error = null) }
    fun closeDialog() = _state.update { if (it.busy) it else it.copy(dialog = SafetyDialog.None, error = null) }

    fun block() = viewModelScope.launch {
        val me = auth.currentUser.filterNotNull().first().uid
        _state.update { it.copy(busy = true, error = null) }
        safety.block(me, uid)
            .onSuccess { _state.update { it.copy(busy = false, dialog = SafetyDialog.None, blocked = true, doneMessage = "Blocked") } }
            .onFailure { e -> _state.update { it.copy(busy = false, error = (e as? SafetyError)?.userMessage) } }
    }

    fun report(reason: ReportReason, details: String, alsoBlock: Boolean) = viewModelScope.launch {
        val me = auth.currentUser.filterNotNull().first().uid
        _state.update { it.copy(busy = true, error = null) }
        val reported = safety.report(me, uid, reason, details)
        if (reported.isFailure) {
            _state.update { it.copy(busy = false, error = (reported.exceptionOrNull() as? SafetyError)?.userMessage) }
            return@launch
        }
        val blocked = alsoBlock && safety.block(me, uid).isSuccess
        _state.update {
            it.copy(
                busy = false,
                dialog = SafetyDialog.None,
                blocked = blocked,
                doneMessage = if (blocked) "Reported and blocked" else "Thanks — we'll review this",
            )
        }
    }
}
