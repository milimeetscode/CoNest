package com.conest.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.model.UserProfile
import com.conest.app.data.profile.ProfileRepository
import com.conest.app.data.social.Candidate
import com.conest.app.data.social.DiscoveryRepository
import com.conest.app.domain.lifestyle.LifestyleAnswers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shown full-screen when a like turns out to be mutual. */
data class MatchMoment(val matchId: String, val other: UserProfile, val myPhoto: String?)

data class DiscoverUiState(
    val loading: Boolean = true,
    val me: UserProfile? = null,
    val deck: List<Candidate> = emptyList(),
    val exhausted: Boolean = false,
    val error: String? = null,
    val match: MatchMoment? = null,
)

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val discovery: DiscoveryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    private var myAnswers: LifestyleAnswers? = null
    private var loadingMore = false

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val uid = auth.currentUser.filterNotNull().first().uid
        val me = profiles.observeProfile(uid).filterNotNull().first()
        myAnswers = profiles.getLifestyleAnswers(uid)
        discovery.reset()
        _state.update { it.copy(me = me, deck = emptyList(), exhausted = false) }
        loadMore()
    }

    private suspend fun loadMore() {
        val me = _state.value.me ?: return
        if (loadingMore || _state.value.exhausted) return
        loadingMore = true
        runCatching { discovery.nextPage(me, myAnswers) }
            .onSuccess { page ->
                _state.update { s -> s.copy(loading = false, deck = s.deck + page.candidates, exhausted = page.exhausted) }
            }
            .onFailure {
                _state.update { s -> s.copy(loading = false, error = "Couldn't load people right now. Check your connection.") }
            }
        loadingMore = false
    }

    fun like(uid: String) = decide(uid, like = true)
    fun pass(uid: String) = decide(uid, like = false)

    /** Drop someone from the deck without recording a swipe (e.g. after blocking them). */
    fun remove(uid: String) = _state.update { s -> s.copy(deck = s.deck.filterNot { it.profile.uid == uid }) }

    fun dismissMatch() = _state.update { it.copy(match = null) }
    fun clearError() = _state.update { it.copy(error = null) }

    private fun decide(uid: String, like: Boolean) {
        val me = _state.value.me ?: return
        val card = _state.value.deck.firstOrNull { it.profile.uid == uid } ?: return
        // Optimistic: the card leaves at once; we put it back if the write fails.
        _state.update { s -> s.copy(deck = s.deck - card) }
        viewModelScope.launch {
            discovery.swipe(me.uid, uid, like)
                .onSuccess { outcome ->
                    outcome.matchId?.let { id -> _state.update { it.copy(match = MatchMoment(id, card.profile, me.photoUrl)) } }
                }
                .onFailure {
                    _state.update { s -> s.copy(deck = listOf(card) + s.deck, error = "That didn't save. Check your connection and try again.") }
                }
            if (_state.value.deck.size < 3) loadMore()
        }
    }
}
