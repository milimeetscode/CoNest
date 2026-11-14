package com.conest.app.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.profile.ProfileError
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.domain.lifestyle.LifestyleAnswers
import com.conest.app.domain.lifestyle.LifestyleQuestions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizUiState(
    val index: Int = 0,
    val answers: LifestyleAnswers = LifestyleAnswers(),
    val saving: Boolean = false,
    val error: String? = null,
    /** True when retaking from the profile, rather than during first-time setup. */
    val editing: Boolean = false,
) {
    val question get() = LifestyleQuestions.all[index]
    val total get() = LifestyleQuestions.all.size
    val selected: Int? get() = answers.values[question.category]
    val isLast get() = index == total - 1
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizUiState(editing = savedState.get<String>("mode") == "edit"))
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    init {
        // Retaking (or resuming after a lost connection): start from what's already saved.
        viewModelScope.launch {
            val uid = auth.currentUser.first()?.uid ?: return@launch
            profiles.getLifestyleAnswers(uid)?.let { saved ->
                _state.update { if (it.answers.values.isEmpty()) it.copy(answers = saved) else it }
            }
        }
    }

    fun select(option: Int) = _state.update {
        it.copy(answers = LifestyleAnswers(it.answers.values + (it.question.category to option)), error = null)
    }

    fun next() = _state.update { if (!it.isLast && it.selected != null) it.copy(index = it.index + 1) else it }

    /** @return false when already on the first question. */
    fun back(): Boolean {
        if (_state.value.index == 0 || _state.value.saving) return false
        _state.update { it.copy(index = it.index - 1, error = null) }
        return true
    }

    fun finish(onSaved: () -> Unit) {
        val s = _state.value
        if (!s.answers.isComplete || s.saving) return
        viewModelScope.launch {
            val uid = auth.currentUser.first()?.uid ?: return@launch
            _state.update { it.copy(saving = true, error = null) }
            profiles.saveLifestyleAnswers(uid, s.answers)
                .onSuccess { _state.update { it.copy(saving = false) }; onSaved() }
                .onFailure { e ->
                    _state.update { it.copy(saving = false, error = (e as? ProfileError)?.userMessage ?: "Couldn't save your answers. Please try again.") }
                }
        }
    }
}
