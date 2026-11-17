package com.conest.app.ui.matches

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.social.Match
import com.conest.app.data.social.MatchRepository
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

data class MatchesUiState(
    val loading: Boolean = true,
    val me: String = "",
    /** Matches nobody has written to yet — shown as a row of portraits. */
    val fresh: List<Match> = emptyList(),
    val conversations: List<Match> = emptyList(),
    val failed: Boolean = false,
) {
    val unreadCount: Int get() = conversations.count { it.unread }
}

/** Also scoped to the tab host, where it drives the unread mark on the Matches tab. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MatchesViewModel @Inject constructor(
    auth: AuthRepository,
    matches: MatchRepository,
) : ViewModel() {

    val state: StateFlow<MatchesUiState> = auth.currentUser
        .filterNotNull()
        .flatMapLatest { user ->
            matches.observeMatches(user.uid).map { all ->
                MatchesUiState(
                    loading = false,
                    me = user.uid,
                    fresh = all.filter { !it.hasConversation && it.other != null },
                    conversations = all.filter { it.hasConversation || it.other == null },
                )
            }
        }
        .catch { emit(MatchesUiState(loading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MatchesUiState())
}
