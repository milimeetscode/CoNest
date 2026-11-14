package com.conest.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.conest.app.data.model.Diet
import com.conest.app.data.model.Drinking
import com.conest.app.data.model.Furnishing
import com.conest.app.data.model.OccupationType
import com.conest.app.data.model.RoomType
import com.conest.app.data.model.UserType
import com.conest.app.ui.components.ChipGroup
import com.conest.app.ui.components.ChoiceCard
import com.conest.app.ui.components.FieldLabel
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.LineArrow
import com.conest.app.ui.components.PhotoSlot
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.SplitHeadline
import com.conest.app.ui.components.StepProgress
import com.conest.app.ui.components.UnderlineField
import com.conest.app.ui.components.Wordmark
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta

/**
 * New-profile setup: intent → basics → place → room (room-havers only) → about.
 * One ViewModel holds the whole draft; it's written to Firestore once, at the end.
 */
@Composable
fun OnboardingScreen(
    onProfileSaved: () -> Unit,
    onExit: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val goBack = { if (!viewModel.back()) onExit() }
    BackHandler(onBack = goBack)

    StepFrame(
        stepNumber = state.stepNumber,
        stepCount = state.steps.size,
        onBack = goBack,
        blocker = state.blocker,
        error = state.error,
        actionLabel = if (state.isLast) "Save profile" else "Continue",
        loading = state.saving,
        onAction = { viewModel.next(onProfileSaved) },
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (fadeIn(tween(260, 60)) + slideInHorizontally(tween(320)) { (if (forward) 1 else -1) * it / 14 }) togetherWith fadeOut(tween(120))
            },
            label = "step",
        ) { step ->
            Column {
                when (step) {
                    OnboardingStep.Intent -> IntentStep(state.draft.userType) { t -> viewModel.edit { it.copy(userType = t) } }
                    OnboardingStep.Basics -> BasicsStep(state.draft, viewModel::edit)
                    OnboardingStep.Place -> PlaceStep(state.draft, viewModel::edit, viewModel::selectCity, viewModel::toggleLocality)
                    OnboardingStep.Room -> RoomStep(state.draft, viewModel::edit)
                    OnboardingStep.About -> AboutStep(state.draft, viewModel::edit, viewModel::toggleInterest)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Frame
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepFrame(
    stepNumber: Int,
    stepCount: Int,
    onBack: () -> Unit,
    blocker: String?,
    error: String?,
    actionLabel: String,
    loading: Boolean,
    onAction: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Box(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = Gutter, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) {
                    LineArrow(Modifier.size(width = 22.dp, height = 12.dp), color = Ink, pointsLeft = true)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "STEP %02d / %02d".format(stepNumber, stepCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedInk,
                )
            }
            StepProgress(stepNumber, stepCount, Modifier.padding(horizontal = Gutter, vertical = 10.dp))

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(scroll)
                    .padding(horizontal = Gutter),
            ) {
                Spacer(Modifier.height(26.dp))
                content()
                Spacer(Modifier.height(28.dp))
            }

            // Hidden while typing so the keyboard keeps the field in view.
            val typing = WindowInsets.isImeVisible
            AnimatedVisibility(!typing, enter = fadeIn(), exit = fadeOut(tween(90))) {
                Column(
                    Modifier
                        .background(Linen.copy(alpha = 0.92f))
                        .padding(horizontal = Gutter)
                        .padding(top = 12.dp, bottom = 16.dp),
                ) {
                    if (error != null) {
                        Note(error)
                        Spacer(Modifier.height(12.dp))
                    } else if (blocker != null) {
                        Text(blocker, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = MutedInk)
                        Spacer(Modifier.height(10.dp))
                    }
                    PrimaryAction(
                        text = actionLabel,
                        loading = loading,
                        loadingText = "Saving your profile",
                        onClick = onAction,
                        modifier = Modifier.background(Linen.copy(alpha = 0f)),
                        dimmed = blocker != null,
                    )
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(Oatmeal.copy(alpha = 0.55f)),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Terracotta),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
    }
}

@Composable
private fun StepIntro(kicker: String, first: String, second: String, body: String) {
    Kicker(kicker)
    Spacer(Modifier.height(16.dp))
    SplitHeadline(first, second)
    Spacer(Modifier.height(12.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MutedInk, modifier = Modifier.widthIn(max = 340.dp))
    Spacer(Modifier.height(30.dp))
}

@Composable
private fun Gap(height: Int = 28) = Spacer(Modifier.height(height.dp))

// ---------------------------------------------------------------------------------------------
// Steps
// ---------------------------------------------------------------------------------------------

@Composable
private fun IntentStep(selected: UserType?, onSelect: (UserType) -> Unit) {
    StepIntro(
        "Your situation", "What brings you", "to CoNest?",
        "This shapes who you'll see first. You can change it later.",
    )
    ChoiceCard(
        marker = "a.",
        title = "I'm looking for a room",
        detail = "Move into a shared flat, or find someone to flat-hunt with.",
        selected = selected == UserType.NeedsRoom,
        onClick = { onSelect(UserType.NeedsRoom) },
    )
    Spacer(Modifier.height(12.dp))
    ChoiceCard(
        marker = "b.",
        title = "I have a room to share",
        detail = "You've got a place, and want the right person in it.",
        selected = selected == UserType.HasRoom,
        onClick = { onSelect(UserType.HasRoom) },
    )
}

@Composable
private fun BasicsStep(draft: OnboardingDraft, edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    val focus = LocalFocusManager.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) edit { it.copy(photo = uri) }
    }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    StepIntro(
        "About you", "First, the", "basics.",
        "A clear photo of your face helps people feel they already know you a little.",
    )

    Row(verticalAlignment = Alignment.Bottom) {
        PhotoSlot(
            model = draft.photo,
            onClick = pick,
            modifier = Modifier.width(150.dp),
            emptyLabel = "Add a photo",
            onClickLabel = if (draft.photo == null) "Add a photo" else "Change photo",
        )
        Spacer(Modifier.width(18.dp))
        Column(Modifier.padding(bottom = 4.dp)) {
            Text(
                "Just you, in good light. No group shots, no sunglasses.",
                fontFamily = NewsreaderText,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MutedInk,
            )
            if (draft.photo != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "CHANGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = pick)
                        .padding(vertical = 8.dp),
                )
            }
        }
    }

    Gap(32)
    UnderlineField(
        label = "Your name",
        value = draft.name,
        onValueChange = { v -> edit { it.copy(name = v.take(40)) } },
        placeholder = "What should people call you?",
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
    )
    Gap(24)
    UnderlineField(
        label = "Age",
        value = draft.age,
        onValueChange = { v -> edit { it.copy(age = v.filter(Char::isDigit).take(2)) } },
        placeholder = "e.g. 26",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.width(140.dp),
    )
    Gap(30)
    FieldLabel("Right now, you're a")
    Spacer(Modifier.height(12.dp))
    ChipGroup(
        options = OccupationType.entries.map { it.label },
        selected = { it == draft.occupationType?.label },
        onToggle = { label -> edit { d -> d.copy(occupationType = OccupationType.entries.first { it.label == label }) } },
    )
    AnimatedVisibility(draft.occupationType != null) {
        Column {
            Gap(24)
            UnderlineField(
                label = if (draft.occupationType == OccupationType.Student) "What do you study? (optional)" else "What do you do? (optional)",
                value = draft.occupation,
                onValueChange = { v -> edit { it.copy(occupation = v.take(60)) } },
                placeholder = if (draft.occupationType == OccupationType.Student) "e.g. M.Des at NID" else "e.g. Product designer at a fintech",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceStep(
    draft: OnboardingDraft,
    edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    selectCity: (String) -> Unit,
    toggleLocality: (String) -> Unit,
) {
    val hasRoom = draft.userType == UserType.HasRoom
    val focus = LocalFocusManager.current

    if (hasRoom) {
        StepIntro("The place", "Where's", "your place?", "People search by neighbourhood, so be as close as you're comfortable with.")
    } else {
        StepIntro("Where & when", "Where are", "you looking?", "Pick the neighbourhoods you'd happily call home. More choices, more people.")
    }

    FieldLabel("City")
    Spacer(Modifier.height(12.dp))
    ChipGroup(CityLocalities.keys.toList(), { it == draft.city }, selectCity)

    AnimatedVisibility(draft.city.isNotBlank()) {
        Column {
            Gap(30)
            FieldLabel("Neighbourhood", hint = if (hasRoom) "Choose one" else "Pick a few")
            Spacer(Modifier.height(12.dp))
            val suggestions = CityLocalities[draft.city].orEmpty()
            val custom = draft.localities.filter { it !in suggestions }
            ChipGroup(suggestions + custom, { it in draft.localities }, toggleLocality)

            var other by rememberSaveable(draft.city) { mutableStateOf("") }
            Spacer(Modifier.height(14.dp))
            UnderlineField(
                label = "Somewhere else?",
                value = other,
                onValueChange = { other = it.take(30) },
                placeholder = "Type a neighbourhood",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    val v = other.trim()
                    if (v.isNotEmpty() && v !in draft.localities) toggleLocality(v)
                    other = ""
                    focus.clearFocus()
                }),
                trailing = {
                    if (other.isNotBlank()) {
                        Text(
                            "ADD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Ink,
                            modifier = Modifier
                                .clickable(role = Role.Button) {
                                    val v = other.trim()
                                    if (v !in draft.localities) toggleLocality(v)
                                    other = ""
                                }
                                .padding(start = 12.dp, top = 14.dp, bottom = 14.dp),
                        )
                    }
                },
            )
        }
    }

    if (!hasRoom) {
        Gap(34)
        FieldLabel("Monthly budget", hint = "${rupeesShort(draft.budgetMin)} – ${rupeesShort(draft.budgetMax)}")
        Spacer(Modifier.height(6.dp))
        RangeSlider(
            value = draft.budgetMin.toFloat()..draft.budgetMax.toFloat(),
            onValueChange = { r ->
                edit { it.copy(budgetMin = r.start.snap(), budgetMax = r.endInclusive.snap()) }
            },
            valueRange = BUDGET_MIN.toFloat()..BUDGET_MAX.toFloat(),
            colors = sliderColors(),
            startThumb = { RuleThumb() },
            endThumb = { RuleThumb() },
            track = { st -> RuleTrack(st.activeRangeStart.fractionIn(st.valueRange), st.activeRangeEnd.fractionIn(st.valueRange)) },
        )
        Text("Rent for your room, before bills.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = MutedInk)
    }

    Gap(34)
    FieldLabel(if (hasRoom) "The room is free from" else "You'd like to move")
    Spacer(Modifier.height(12.dp))
    val options = remember { moveInOptions() }
    ChipGroup(
        options = options.map { it.label },
        selected = { label -> options.first { it.label == label }.key == draft.moveIn },
        onToggle = { label -> edit { d -> d.copy(moveIn = options.first { it.label == label }.key) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomStep(draft: OnboardingDraft, edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(OnboardingUiState.MAX_ROOM_PHOTOS),
    ) { uris ->
        if (uris.isNotEmpty()) edit { d -> d.copy(roomPhotos = (d.roomPhotos + uris).distinct().take(OnboardingUiState.MAX_ROOM_PHOTOS)) }
    }

    StepIntro("The room", "Tell us about", "the room.", "The basics people ask first. You can add more detail in conversation.")

    FieldLabel("Kind of room")
    Spacer(Modifier.height(12.dp))
    ChipGroup(RoomType.entries.map { it.label }, { it == draft.roomType?.label }, { l -> edit { d -> d.copy(roomType = RoomType.entries.first { it.label == l }) } })

    Gap(28)
    FieldLabel("Furnishing")
    Spacer(Modifier.height(12.dp))
    ChipGroup(Furnishing.entries.map { it.label }, { it == draft.furnishing?.label }, { l -> edit { d -> d.copy(furnishing = Furnishing.entries.first { it.label == l }) } })

    Gap(32)
    FieldLabel("Monthly rent", hint = rupeesShort(draft.rent))
    Spacer(Modifier.height(6.dp))
    Slider(
        value = draft.rent.toFloat(),
        onValueChange = { v -> edit { it.copy(rent = v.snap()) } },
        valueRange = BUDGET_MIN.toFloat()..BUDGET_MAX.toFloat(),
        colors = sliderColors(),
        thumb = { RuleThumb() },
        track = { st -> RuleTrack(0f, st.value.fractionIn(st.valueRange)) },
    )
    Text("For this room alone, before bills.", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = MutedInk)

    Gap(32)
    FieldLabel("Photos of the room", hint = "${draft.roomPhotos.size} / ${OnboardingUiState.MAX_ROOM_PHOTOS}")
    Spacer(Modifier.height(12.dp))
    val slots = (draft.roomPhotos.map<android.net.Uri, android.net.Uri?> { it } + listOf(null))
        .take(OnboardingUiState.MAX_ROOM_PHOTOS)
    slots.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { uri ->
                PhotoSlot(
                    model = uri,
                    aspect = 4f / 3f,
                    emptyLabel = "Add photos",
                    onClickLabel = if (uri == null) "Add room photos" else "Remove this photo",
                    onClick = {
                        if (uri == null) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            edit { d -> d.copy(roomPhotos = d.roomPhotos - uri) }
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
    Text(
        if (draft.roomPhotos.isEmpty()) "Optional, but rooms with photos get far more interest." else "Tap a photo to remove it.",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = MutedInk,
    )
}

@Composable
private fun AboutStep(
    draft: OnboardingDraft,
    edit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    toggleInterest: (String) -> Unit,
) {
    StepIntro(
        "In your words", "A little about", "you.",
        "What's a good weekend? What are you like before coffee? A house rule you swear by?",
    )
    UnderlineField(
        label = "Bio",
        value = draft.bio,
        onValueChange = { v -> edit { it.copy(bio = v.take(OnboardingUiState.MAX_BIO)) } },
        placeholder = "Start anywhere…",
        singleLine = false,
        minLines = 4,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        supporting = {
            Text(
                "${draft.bio.length} / ${OnboardingUiState.MAX_BIO}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = MutedInk,
            )
        },
    )

    Gap(32)
    FieldLabel("Things you love", hint = "${draft.interests.size} / ${OnboardingUiState.MAX_INTERESTS}")
    Spacer(Modifier.height(12.dp))
    ChipGroup(
        options = Interests,
        selected = { it in draft.interests },
        onToggle = toggleInterest,
        enabled = { it in draft.interests || draft.interests.size < OnboardingUiState.MAX_INTERESTS },
    )

    Gap(30)
    FieldLabel("Food at home")
    Spacer(Modifier.height(12.dp))
    ChipGroup(Diet.entries.map { it.label }, { it == draft.diet?.label }, { l ->
        edit { d -> Diet.entries.first { it.label == l }.let { v -> d.copy(diet = if (d.diet == v) null else v) } }
    })

    Gap(28)
    FieldLabel("Drinking")
    Spacer(Modifier.height(12.dp))
    ChipGroup(Drinking.entries.map { it.label }, { it == draft.drinking?.label }, { l ->
        edit { d -> Drinking.entries.first { it.label == l }.let { v -> d.copy(drinking = if (d.drinking == v) null else v) } }
    })
}

// ---------------------------------------------------------------------------------------------

private fun Float.snap(): Int = (Math.round(this / BUDGET_STEP) * BUDGET_STEP)

private fun Float.fractionIn(range: ClosedFloatingPointRange<Float>) =
    ((this - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

/** A short upright rule for the thumb — like a mark on a ruler, not a floating knob. */
@Composable
private fun RuleThumb() {
    Box(
        Modifier
            .size(width = 3.dp, height = 24.dp)
            .background(Forest),
    )
}

/** Hairline track: straw for the whole span, forest for the chosen part. */
@Composable
private fun RuleTrack(start: Float, end: Float) {
    androidx.compose.foundation.Canvas(
        Modifier
            .fillMaxWidth()
            .height(24.dp),
    ) {
        val y = size.height / 2
        val w = 2.dp.toPx()
        drawLine(Straw, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), w)
        drawLine(Forest, androidx.compose.ui.geometry.Offset(size.width * start, y), androidx.compose.ui.geometry.Offset(size.width * end, y), w)
    }
}

@Composable
private fun sliderColors() = SliderDefaults.colors(
    thumbColor = Forest,
    activeTrackColor = Forest,
    inactiveTrackColor = Straw,
    activeTickColor = Forest,
    inactiveTickColor = Straw,
)
