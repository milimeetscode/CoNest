package com.conest.app.ui.discover

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.conest.app.data.model.UserType
import com.conest.app.data.social.Candidate
import com.conest.app.domain.lifestyle.LifestyleQuestions
import com.conest.app.ui.components.CompatibilityScore
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.OutlineAction
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.SplitHeadline
import com.conest.app.ui.components.Wordmark
import com.conest.app.ui.components.WovenNest
import com.conest.app.ui.components.displayName
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.onboarding.moveInLabel
import com.conest.app.ui.onboarding.rupeesShort
import com.conest.app.ui.theme.Bark
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Discover: one person at a time, lifestyle first. Drag right for interested, left to pass,
 * or use the buttons; tap the card to read the full profile before deciding.
 */
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onOpenProfile: (uid: String) -> Unit,
    onOpenChat: (matchId: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Gutter)
                    .padding(top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Wordmark(size = 21f)
                Spacer(Modifier.weight(1f))
                Text(
                    (state.me?.city ?: "").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedInk,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Box(Modifier.padding(horizontal = Gutter)) { Hairline() }

            if (state.me?.discoverable == false) {
                Text(
                    "Your profile is paused, so others can't see you. You can still browse.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    modifier = Modifier
                        .padding(horizontal = Gutter, vertical = 10.dp)
                        .fillMaxWidth()
                        .background(Oatmeal.copy(alpha = 0.55f))
                        .padding(12.dp),
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.deck.isEmpty() -> LoadingDeck()
                    state.deck.isEmpty() -> EmptyDeck(city = state.me?.city.orEmpty(), error = state.error, onRetry = viewModel::refresh)
                    else -> Deck(
                        deck = state.deck,
                        onLike = viewModel::like,
                        onPass = viewModel::pass,
                        onOpen = onOpenProfile,
                    )
                }
            }
            if (state.error != null && state.deck.isNotEmpty()) {
                Text(
                    state.error.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Terracotta,
                    modifier = Modifier
                        .padding(horizontal = Gutter, vertical = 6.dp)
                        .clickable { viewModel.clearError() },
                )
            }
        }

        AnimatedVisibility(state.match != null, enter = fadeIn(tween(400)), exit = fadeOut(tween(200))) {
            state.match?.let { m ->
                BackHandler { viewModel.dismissMatch() }
                MatchMomentOverlay(
                    moment = m,
                    onSayHello = { viewModel.dismissMatch(); onOpenChat(m.matchId) },
                    onKeepExploring = viewModel::dismissMatch,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Deck
// ---------------------------------------------------------------------------------------------

@Composable
private fun Deck(
    deck: List<Candidate>,
    onLike: (String) -> Unit,
    onPass: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val top = deck.first()
    // One offset per top card; keyed so the next card starts centred.
    val offset = remember(top.profile.uid) { Animatable(0f) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Gutter)
            .padding(top = 16.dp),
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val threshold = widthPx * 0.28f

        fun fling(like: Boolean) = scope.launch {
            offset.animateTo(if (like) widthPx * 1.4f else -widthPx * 1.4f, tween(260))
            if (like) onLike(top.profile.uid) else onPass(top.profile.uid)
        }

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // The next card waits underneath, slightly inset — a stack of pages.
                deck.getOrNull(1)?.let { next ->
                    key(next.profile.uid) {
                        ProfileCard(
                            next,
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val p = (abs(offset.value) / threshold).coerceIn(0f, 1f)
                                    val s = 0.95f + 0.05f * p
                                    scaleX = s; scaleY = s
                                    translationY = (1f - p) * 14.dp.toPx()
                                },
                        )
                    }
                }
                key(top.profile.uid) {
                    ProfileCard(
                        top,
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offset.value
                                rotationZ = (offset.value / widthPx) * 7f
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.2f)
                            }
                            .pointerInput(top.profile.uid) {
                                detectDragGestures(
                                    onDragEnd = {
                                        when {
                                            offset.value > threshold -> fling(true)
                                            offset.value < -threshold -> fling(false)
                                            else -> scope.launch { offset.animateTo(0f, spring(dampingRatio = 0.7f)) }
                                        }
                                    },
                                    onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                                ) { change, drag ->
                                    change.consume()
                                    scope.launch { offset.snapTo(offset.value + drag.x) }
                                }
                            }
                            .clickable(onClickLabel = "Read ${top.profile.name}'s profile") { onOpen(top.profile.uid) }
                            .semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction("Interested") { fling(true); true },
                                    CustomAccessibilityAction("Pass") { fling(false); true },
                                )
                            },
                        dragProgress = { (offset.value / threshold).coerceIn(-1f, 1f) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlineAction("Pass", { fling(false) }, Modifier.weight(1f))
                PrimaryAction("Interested", { fling(true) }, Modifier.weight(1.4f))
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * The profile card: portrait, name and fit score, then the practical facts and what the two
 * of you have in common — so the decision isn't made on the photo alone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileCard(candidate: Candidate, modifier: Modifier = Modifier, dragProgress: () -> Float = { 0f }) {
    val p = candidate.profile
    val shared = candidate.compatibility?.categoryScores
        ?.filter { it.value == 100 }?.keys
        ?.mapNotNull { c -> candidate.answers?.values?.get(c)?.let { LifestyleQuestions.forCategory(c).options[it].summary } }
        .orEmpty()
        .take(4)

    Column(
        modifier
            .background(Linen, MaterialTheme.shapes.small)
            .border(1.dp, Straw, MaterialTheme.shapes.small)
            .clipToBounds(),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Oatmeal)) {
            AsyncImage(
                model = p.photoUrl,
                contentDescription = "${p.name}'s photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Decision stamps fade in with the drag, like a pencil mark in the margin.
            Stamp("Interested", Forest, Modifier.align(Alignment.TopStart).graphicsLayer { alpha = dragProgress().coerceAtLeast(0f) })
            Stamp("Pass", Bark, Modifier.align(Alignment.TopEnd).graphicsLayer { alpha = (-dragProgress()).coerceAtLeast(0f) })
        }

        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            p.name.substringBefore(' '),
                            fontFamily = NewsreaderDisplay,
                            fontWeight = FontWeight(400),
                            fontSize = 30.sp,
                            lineHeight = 32.sp,
                            color = Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${p.age}", fontFamily = NewsreaderDisplay, fontStyle = FontStyle.Italic, fontSize = 24.sp, color = MutedInk)
                    }
                    Text(
                        p.occupation.ifBlank { p.occupationType?.label.orEmpty() },
                        fontFamily = NewsreaderText,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = MutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                CompatibilityScore(candidate.compatibility, size = 38f)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                factLine(candidate),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (shared.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("YOU'RE BOTH", style = MaterialTheme.typography.labelSmall, color = MutedInk)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), maxLines = 2) {
                    shared.forEach { Tag(it) }
                }
            }
            candidate.compatibility?.dealBreakers?.firstOrNull()?.let { c ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "You differ on ${c.displayName.lowercase()}.",
                    fontFamily = NewsreaderText,
                    fontStyle = FontStyle.Italic,
                    fontSize = 14.sp,
                    color = Terracotta,
                )
            }
        }
    }
}

private fun factLine(c: Candidate): String {
    val p = c.profile
    val where = p.preferredLocalities.take(2).joinToString(", ").ifBlank { p.city }
    val money = if (p.userType == UserType.HasRoom) "Room ${rupeesShort(p.budgetMin)}" else "${rupeesShort(p.budgetMin)}–${rupeesShort(p.budgetMax)}"
    val intent = if (p.userType == UserType.HasRoom) "Has a room" else "Looking"
    return "$intent · $where · $money · ${moveInLabel(p.moveIn)}"
}

@Composable
private fun Stamp(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight(600)),
        color = color,
        modifier = modifier
            .padding(16.dp)
            .background(Linen.copy(alpha = 0.92f))
            .border(1.5.dp, color)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun Tag(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = Forest,
        modifier = Modifier
            .background(Oatmeal.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun LoadingDeck() {
    Column(
        Modifier.fillMaxSize().padding(Gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Finding people who live like you…",
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            color = MutedInk,
        )
    }
}

@Composable
private fun EmptyDeck(city: String, error: String?, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        WovenNest(Modifier.weight(1f).fillMaxWidth(), animate = false)
        Column(Modifier.padding(horizontal = Gutter).padding(bottom = 24.dp)) {
            Kicker(if (error != null) "Hmm" else "That's everyone for now")
            Spacer(Modifier.height(14.dp))
            if (error != null) {
                SplitHeadline("We couldn't", "load anyone.", maxSize = 38f)
                Spacer(Modifier.height(10.dp))
                Text(error, style = MaterialTheme.typography.bodyLarge, color = MutedInk)
            } else {
                SplitHeadline("You've met", "everyone here.", maxSize = 38f)
                Spacer(Modifier.height(10.dp))
                Text(
                    "New people join ${city.ifBlank { "your city" }} every day. Check back soon.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedInk,
                    modifier = Modifier.widthIn(max = 340.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            OutlineAction(if (error != null) "Try again" else "Look again", onRetry)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Match moment
// ---------------------------------------------------------------------------------------------

@Composable
private fun MatchMomentOverlay(moment: MatchMoment, onSayHello: () -> Unit, onKeepExploring: () -> Unit) {
    val first = moment.other.name.substringBefore(' ')
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(Unit) { tilt.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 120f)) }

    Column(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen)
            .clickable(enabled = false) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Gutter),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // Two prints laid on the table, tilting towards each other.
            Row(horizontalArrangement = Arrangement.spacedBy((-28).dp)) {
                Print(moment.myPhoto, Modifier.graphicsLayer { rotationZ = -6f * tilt.value; translationY = 12.dp.toPx() * (1 - tilt.value) })
                Print(moment.other.photoUrl, Modifier.graphicsLayer { rotationZ = 5f * tilt.value; translationY = -12.dp.toPx() * (1 - tilt.value) })
            }
        }
        Kicker("It's mutual")
        Spacer(Modifier.height(16.dp))
        SplitHeadline("You and $first", "both said yes.", maxSize = 44f)
        Spacer(Modifier.height(12.dp))
        Text(
            "Start simple: when you're hoping to move, or what a usual weekday looks like.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
        )
        Spacer(Modifier.height(28.dp))
        PrimaryAction("Say hello", onSayHello)
        Spacer(Modifier.height(10.dp))
        OutlineAction("Keep exploring", onKeepExploring)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Print(model: Any?, modifier: Modifier) {
    Box(
        modifier
            .width(150.dp)
            .background(Linen)
            .border(1.dp, Straw)
            .padding(8.dp)
            .padding(bottom = 22.dp),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .background(Oatmeal),
        )
    }
}

