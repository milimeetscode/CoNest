package com.conest.app.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.conest.app.data.model.UserProfile
import com.conest.app.data.model.UserType
import com.conest.app.domain.lifestyle.LifestyleAnswers
import com.conest.app.domain.lifestyle.LifestyleCategory
import com.conest.app.domain.lifestyle.LifestyleQuestions
import com.conest.app.ui.components.CompatibilityBreakdown
import com.conest.app.ui.components.CompatibilityScore
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.LineArrow
import com.conest.app.ui.components.OutlineAction
import com.conest.app.ui.components.OverflowMenu
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.SafetyDialog
import com.conest.app.ui.components.SafetyDialogHost
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.onboarding.moveInLabel
import com.conest.app.ui.onboarding.rupeesShort
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.Moss
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Oatmeal

/** Where the profile was opened from decides the actions at the bottom. */
enum class PersonContext { Discover, Match }

/**
 * Someone's full profile: the portrait, the lifestyle fit with its breakdown, the practical
 * facts, their own words, the room, and what you share.
 */
@Composable
fun PersonScreen(
    context: PersonContext,
    onBack: () -> Unit,
    onLike: (String) -> Unit = {},
    onPass: (String) -> Unit = {},
    onMessage: () -> Unit = {},
    onBlocked: (String) -> Unit = {},
    viewModel: PersonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val name = profile?.name?.substringBefore(' ') ?: "this person"

    LaunchedEffect(state.doneMessage) {
        if (state.doneMessage != null) {
            if (state.blocked) onBlocked(viewModel.uid) else onBack()
        }
    }

    Box(Modifier.fillMaxSize().paperTexture(Linen)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) { LineArrow(Modifier.size(width = 22.dp, height = 12.dp), color = Ink, pointsLeft = true) }
                Spacer(Modifier.weight(1f))
                if (profile != null) {
                    OverflowMenu(
                        buildList {
                            if (context == PersonContext.Discover) add("Hide $name" to { onPass(viewModel.uid) })
                            add("Report $name" to { viewModel.openDialog(SafetyDialog.Report) })
                            add("Block $name" to { viewModel.openDialog(SafetyDialog.Block) })
                        },
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Gutter),
            ) {
                when {
                    state.loading -> Spacer(Modifier.height(240.dp))
                    profile == null -> Text(
                        "This profile is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedInk,
                        modifier = Modifier.padding(top = 40.dp),
                    )
                    else -> ProfileBody(profile, state.answers, state.myInterests, state.compatibility)
                }
                Spacer(Modifier.height(28.dp))
            }

            if (profile != null) {
                Column(
                    Modifier
                        .background(Linen)
                        .navigationBarsPadding()
                        .padding(horizontal = Gutter, vertical = 12.dp),
                ) {
                    when (context) {
                        PersonContext.Discover -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlineAction("Pass", { onPass(viewModel.uid) }, Modifier.weight(1f))
                            PrimaryAction("Interested", { onLike(viewModel.uid) }, Modifier.weight(1.4f))
                        }
                        PersonContext.Match -> PrimaryAction("Message $name", onMessage)
                    }
                }
            }
        }
    }

    SafetyDialogHost(
        dialog = state.dialog,
        name = name,
        busy = state.busy,
        error = state.error,
        onBlock = viewModel::block,
        onReport = viewModel::report,
        onDismiss = viewModel::closeDialog,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileBody(p: UserProfile, answers: LifestyleAnswers?, myInterests: List<String>, compatibility: com.conest.app.domain.lifestyle.Compatibility?) {
    Spacer(Modifier.height(8.dp))
    AsyncImage(
        model = p.photoUrl,
        contentDescription = "${p.name}'s photo",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(Oatmeal),
    )
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(p.name, fontFamily = NewsreaderDisplay, fontWeight = FontWeight(400), fontSize = 34.sp, lineHeight = 36.sp, color = Ink)
            Text("${p.age}", fontFamily = NewsreaderDisplay, fontStyle = FontStyle.Italic, fontSize = 26.sp, color = MutedInk)
            Spacer(Modifier.height(6.dp))
            Text(
                p.occupation.ifBlank { p.occupationType?.label.orEmpty() },
                fontFamily = NewsreaderText,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
                color = MutedInk,
            )
        }
        CompatibilityScore(compatibility, size = 46f)
    }

    Spacer(Modifier.height(26.dp))
    val hasRoom = p.userType == UserType.HasRoom
    Fact(if (hasRoom) "The place" else "Looking in", "${p.preferredLocalities.joinToString()} · ${p.city}")
    Fact(
        if (hasRoom) "Rent" else "Budget",
        if (p.budgetMin == p.budgetMax) "${rupeesShort(p.budgetMin)} / month" else "${rupeesShort(p.budgetMin)} – ${rupeesShort(p.budgetMax)} / month",
    )
    Fact(if (hasRoom) "Room free" else "Moving", moveInLabel(p.moveIn))
    p.room?.let { r -> Fact("The room", listOfNotNull(r.type?.label, r.furnishing?.label).joinToString(" · ")) }
    listOfNotNull(p.diet?.label, p.drinking?.let { "Drinks: ${it.label.lowercase()}" }).takeIf { it.isNotEmpty() }
        ?.let { Fact("Habits", it.joinToString(" · ")) }
    Hairline()

    if (p.bio.isNotBlank()) {
        Spacer(Modifier.height(28.dp))
        Kicker("In their words")
        Spacer(Modifier.height(12.dp))
        Text("“${p.bio.trim()}”", fontFamily = NewsreaderText, fontStyle = FontStyle.Italic, fontSize = 19.sp, lineHeight = 27.sp, color = Ink)
    }

    if (compatibility != null) {
        Spacer(Modifier.height(30.dp))
        Kicker("How you'd live together")
        Spacer(Modifier.height(12.dp))
        CompatibilityBreakdown(compatibility)
        Spacer(Modifier.height(6.dp))
        Text(
            "An estimate from your answers, not a guarantee. Talk it through.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
            color = MutedInk,
        )
    }

    if (answers != null && answers.values.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        Kicker("How they live")
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LifestyleCategory.entries.forEach { c ->
                answers.values[c]?.let { Tag(LifestyleQuestions.forCategory(c).options[it].summary) }
            }
        }
    }

    val photos = p.room?.photoUrls.orEmpty()
    if (photos.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        Kicker("The room")
        Spacer(Modifier.height(12.dp))
        photos.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = "Room photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(Oatmeal),
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    if (p.interests.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        Kicker("Things they love")
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            p.interests.forEach { Tag(it, shared = it in myInterests) }
        }
        if (p.interests.any { it in myInterests }) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Moss))
                Spacer(Modifier.width(8.dp))
                Text("You both love these", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp), color = MutedInk)
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Hairline()
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk, modifier = Modifier.width(110.dp).padding(top = 3.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Tag(text: String, shared: Boolean = false) {
    Row(
        Modifier
            .background(Oatmeal.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shared) {
            Box(Modifier.size(6.dp).background(Moss))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Forest)
    }
}

