package com.conest.app.ui.onboarding

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conest.app.data.auth.AuthRepository
import com.conest.app.data.model.Diet
import com.conest.app.data.model.Drinking
import com.conest.app.data.model.Furnishing
import com.conest.app.data.model.OccupationType
import com.conest.app.data.model.RoomDetails
import com.conest.app.data.model.RoomType
import com.conest.app.data.model.UserProfile
import com.conest.app.data.model.UserType
import com.conest.app.data.profile.ProfileError
import com.conest.app.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { Intent, Basics, Place, Room, About }

data class OnboardingDraft(
    val userType: UserType? = null,
    val photo: Uri? = null,
    val name: String = "",
    val age: String = "",
    val occupationType: OccupationType? = null,
    val occupation: String = "",
    val city: String = "",
    val localities: List<String> = emptyList(),
    val budgetMin: Int = 8_000,
    val budgetMax: Int = 20_000,
    val moveIn: String = "",
    val roomType: RoomType? = null,
    val furnishing: Furnishing? = null,
    val rent: Int = 15_000,
    val roomPhotos: List<Uri> = emptyList(),
    /** Already-saved photos (edit mode): kept unless replaced or removed. */
    val existingPhotoUrl: String? = null,
    val existingRoomPhotos: List<String> = emptyList(),
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val diet: Diet? = null,
    val drinking: Drinking? = null,
)

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Intent,
    val draft: OnboardingDraft = OnboardingDraft(),
    val saving: Boolean = false,
    val error: String? = null,
    /** Editing an existing profile rather than creating one. */
    val editing: Boolean = false,
) {
    val steps: List<OnboardingStep>
        get() = OnboardingStep.entries.filter { it != OnboardingStep.Room || draft.userType == UserType.HasRoom }
    val stepNumber: Int get() = steps.indexOf(step) + 1
    val isLast: Boolean get() = step == steps.last()

    /** What still stands between the person and the next step, in plain words; null when clear. */
    val blocker: String?
        get() = with(draft) {
            when (step) {
                OnboardingStep.Intent -> if (userType == null) "Choose one to continue" else null
                OnboardingStep.Basics -> when {
                    photo == null && existingPhotoUrl == null -> "Add a photo of yourself"
                    name.isBlank() -> "Add your name"
                    age.toIntOrNull() == null -> "Add your age"
                    age.toInt() !in 18..99 -> "You need to be 18 or over to use CoNest"
                    occupationType == null -> "Tell us whether you study or work"
                    else -> null
                }
                OnboardingStep.Place -> when {
                    city.isBlank() -> "Choose a city"
                    localities.isEmpty() -> if (userType == UserType.HasRoom) "Choose your neighbourhood" else "Pick at least one neighbourhood"
                    moveIn.isBlank() -> if (userType == UserType.HasRoom) "Say when the room is free" else "Say when you'd like to move"
                    else -> null
                }
                OnboardingStep.Room -> when {
                    roomType == null -> "Choose the kind of room"
                    furnishing == null -> "Say how it's furnished"
                    else -> null
                }
                OnboardingStep.About -> when {
                    bio.trim().length < MIN_BIO -> "Write a little more about yourself (${MIN_BIO - bio.trim().length} more characters)"
                    else -> null
                }
            }
        }

    companion object {
        const val MIN_BIO = 40
        const val MAX_BIO = 300
        const val MAX_INTERESTS = 6
        const val MAX_ROOM_PHOTOS = 4
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val editing = savedState.get<String>("mode") == "edit"
    private val _state = MutableStateFlow(OnboardingUiState(editing = editing))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    /** The saved profile being edited; keeps fields this flow doesn't touch (quiz, pause). */
    private var original: UserProfile? = null

    init {
        viewModelScope.launch {
            val user = auth.currentUser.first() ?: return@launch
            if (editing) {
                profiles.getProfile(user.uid)?.let { p ->
                    original = p
                    _state.update { it.copy(draft = p.toDraft()) }
                }
            } else {
                // Google accounts arrive with a name; save the person typing it again.
                val name = user.displayName.orEmpty()
                if (name.isNotBlank()) edit { if (it.name.isBlank()) it.copy(name = name) else it }
            }
        }
    }

    fun edit(transform: (OnboardingDraft) -> OnboardingDraft) =
        _state.update { it.copy(draft = transform(it.draft), error = null) }

    fun toggleLocality(value: String) = edit { d ->
        val single = d.userType == UserType.HasRoom
        when {
            value in d.localities -> d.copy(localities = d.localities - value)
            single -> d.copy(localities = listOf(value))
            else -> d.copy(localities = d.localities + value)
        }
    }

    fun toggleInterest(value: String) = edit { d ->
        when {
            value in d.interests -> d.copy(interests = d.interests - value)
            d.interests.size >= OnboardingUiState.MAX_INTERESTS -> d
            else -> d.copy(interests = d.interests + value)
        }
    }

    fun selectCity(city: String) = edit { if (it.city == city) it else it.copy(city = city, localities = emptyList()) }

    /** @return true if the step moved back; false means we're on the first step. */
    fun back(): Boolean {
        val s = _state.value
        val i = s.steps.indexOf(s.step)
        if (i <= 0 || s.saving) return false
        _state.update { it.copy(step = s.steps[i - 1], error = null) }
        return true
    }

    /** Advances, or on the last step saves the profile and calls [onSaved]. */
    fun next(onSaved: () -> Unit) {
        val s = _state.value
        if (s.blocker != null || s.saving) return
        if (!s.isLast) {
            _state.update { it.copy(step = s.steps[s.steps.indexOf(s.step) + 1]) }
            return
        }
        save(onSaved)
    }

    private fun save(onSaved: () -> Unit) = viewModelScope.launch {
        val user = auth.currentUser.first() ?: return@launch
        val d = _state.value.draft
        _state.update { it.copy(saving = true, error = null) }

        val hasRoom = d.userType == UserType.HasRoom
        val profile = UserProfile(
            uid = user.uid,
            name = d.name,
            age = d.age.toInt(),
            occupationType = d.occupationType,
            occupation = d.occupation,
            city = d.city,
            preferredLocalities = d.localities,
            budgetMin = if (hasRoom) d.rent else d.budgetMin,
            budgetMax = if (hasRoom) d.rent else d.budgetMax,
            moveIn = d.moveIn,
            userType = d.userType,
            bio = d.bio.take(OnboardingUiState.MAX_BIO),
            interests = d.interests,
            diet = d.diet,
            drinking = d.drinking,
            photoUrl = d.existingPhotoUrl,
            room = if (hasRoom) RoomDetails(d.roomType, d.furnishing, d.localities.firstOrNull().orEmpty(), d.rent, d.existingRoomPhotos) else null,
            profileComplete = true,
            quizComplete = original?.quizComplete ?: false,
            discoverable = original?.discoverable ?: true,
        )
        profiles.saveProfile(profile, d.photo, if (hasRoom) d.roomPhotos else emptyList(), isNew = !editing)
            .onSuccess {
                _state.update { it.copy(saving = false) }
                onSaved()
            }
            .onFailure { e ->
                _state.update { it.copy(saving = false, error = (e as? ProfileError)?.userMessage ?: "Something went wrong. Please try again.") }
            }
    }
}

private fun UserProfile.toDraft() = OnboardingDraft(
    userType = userType,
    name = name,
    age = if (age > 0) age.toString() else "",
    occupationType = occupationType,
    occupation = occupation,
    city = city,
    localities = preferredLocalities,
    budgetMin = if (userType == UserType.HasRoom || budgetMin <= 0) 8_000 else budgetMin,
    budgetMax = if (userType == UserType.HasRoom || budgetMax <= 0) 20_000 else budgetMax,
    moveIn = moveIn,
    roomType = room?.type,
    furnishing = room?.furnishing,
    rent = room?.rent?.takeIf { it > 0 } ?: 15_000,
    bio = bio,
    interests = interests,
    diet = diet,
    drinking = drinking,
    existingPhotoUrl = photoUrl,
    existingRoomPhotos = room?.photoUrls.orEmpty(),
)
