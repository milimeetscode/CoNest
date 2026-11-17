package com.conest.app.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.model.UserProfile
import com.conest.app.data.model.UserType
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.data.social.ChatError
import com.conest.app.data.social.ChatMessage
import com.conest.app.data.social.Match
import com.conest.app.data.social.MatchRepository
import com.conest.app.data.social.MatchStatus
import com.conest.app.data.social.ReportReason
import com.conest.app.data.social.SafetyError
import com.conest.app.data.social.SafetyRepository
import com.conest.app.ui.components.SafetyDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val me: String = "",
    val match: Match? = null,
    val messages: List<ChatMessage> = emptyList(),
    val loaded: Boolean = false,
    val draft: String = "",
    val sendError: String? = null,
    val starters: List<String> = emptyList(),
    val dialog: SafetyDialog = SafetyDialog.None,
    val busy: Boolean = false,
    val safetyError: String? = null,
    val reported: Boolean = false,
) {
    val other: UserProfile? get() = match?.other
    val closed: Boolean get() = match?.status == MatchStatus.Blocked || (match != null && match.other == null)
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val matches: MatchRepository,
    private val safety: SafetyRepository,
) : ViewModel() {

    val matchId: String = checkNotNull(savedState["matchId"])
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val me = auth.currentUser.filterNotNull().first().uid
            _state.update { it.copy(me = me) }
            launch {
                matches.observeMatch(me, matchId).catch { }.collect { m ->
                    _state.update { s ->
                        s.copy(match = m, starters = if (s.starters.isEmpty() && m?.other != null) startersFor(me, m.other) else s.starters)
                    }
                }
            }
            launch {
                matches.observeMessages(matchId).catch { }.collect { msgs ->
                    val before = _state.value.messages
                    _state.update { it.copy(messages = msgs, loaded = true) }
                    // Opening the chat, or a new message from them arriving, counts as read.
                    if (before.isEmpty() || msgs.lastOrNull()?.let { it.senderId != me && it.id != before.lastOrNull()?.id } == true) {
                        matches.markRead(me, matchId)
                    }
                }
            }
        }
    }

    fun onDraftChange(text: String) = _state.update { it.copy(draft = text.take(2000), sendError = null) }

    fun useStarter(text: String) = _state.update { it.copy(draft = text) }

    fun send() {
        val s = _state.value
        val text = s.draft.trim()
        if (text.isEmpty() || s.closed) return
        _state.update { it.copy(draft = "") }
        viewModelScope.launch {
            matches.send(s.me, matchId, text).onFailure { e ->
                _state.update { it.copy(draft = text, sendError = (e as? ChatError)?.userMessage) }
            }
        }
    }

    fun openDialog(d: SafetyDialog) = _state.update { it.copy(dialog = d, safetyError = null) }
    fun closeDialog() = _state.update { if (it.busy) it else it.copy(dialog = SafetyDialog.None) }

    fun block() = viewModelScope.launch {
        val s = _state.value
        val other = s.match?.otherUid ?: return@launch
        _state.update { it.copy(busy = true) }
        safety.block(s.me, other)
            .onSuccess { _state.update { it.copy(busy = false, dialog = SafetyDialog.None) } }
            .onFailure { e -> _state.update { it.copy(busy = false, safetyError = (e as? SafetyError)?.userMessage) } }
    }

    fun unblock() = viewModelScope.launch {
        val s = _state.value
        val other = s.match?.otherUid ?: return@launch
        safety.unblock(s.me, other)
    }

    fun report(reason: ReportReason, details: String, alsoBlock: Boolean) = viewModelScope.launch {
        val s = _state.value
        val other = s.match?.otherUid ?: return@launch
        _state.update { it.copy(busy = true, safetyError = null) }
        val r = safety.report(s.me, other, reason, details)
        if (r.isFailure) {
            _state.update { it.copy(busy = false, safetyError = (r.exceptionOrNull() as? SafetyError)?.userMessage) }
            return@launch
        }
        if (alsoBlock) safety.block(s.me, other)
        _state.update { it.copy(busy = false, dialog = SafetyDialog.None, reported = true) }
    }

    /** A few easy openers, one of them specific to this person where we can. */
    private suspend fun startersFor(me: String, other: UserProfile): List<String> {
        val mine = profiles.getProfile(me)
        val sharedInterest = other.interests.firstOrNull { it in mine?.interests.orEmpty() }
        return buildList {
            if (other.userType == UserType.HasRoom) add("Is the room still available? I'd love to see it.")
            else add("When are you hoping to move?")
            if (sharedInterest != null) add("I see you're into ${sharedInterest.lowercase()} too!")
            add("What does a usual weekday look like for you?")
            add("How do you like to split bills and chores?")
        }.take(3)
    }
}
