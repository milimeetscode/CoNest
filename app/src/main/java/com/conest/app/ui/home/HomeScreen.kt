package com.conest.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
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
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.OutlineAction
import com.conest.app.ui.components.Wordmark
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
import com.conest.app.ui.theme.Straw
import java.time.LocalTime

/**
 * Returning users land here: their profile, retrieved from Firestore, laid out the way others
 * will see it. Discovery and matches slot in around this in Phase 4.
 */
@Composable
fun HomeScreen(
    onRetakeQuiz: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Gutter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Wordmark(size = 21f)
                Spacer(Modifier.weight(1f))
                Text(
                    "SIGN OUT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink,
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = viewModel::signOut)
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
            Hairline()

            val profile = state.profile
            when {
                state.loading -> Spacer(Modifier.height(200.dp))
                profile == null -> Text(
                    "We couldn't load your profile. Check your connection.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedInk,
                    modifier = Modifier.padding(top = 40.dp),
                )
                else -> ProfileSpread(profile, state.answers, onRetakeQuiz)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSpread(profile: UserProfile, answers: LifestyleAnswers?, onRetakeQuiz: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val firstName = profile.name.trim().substringBefore(' ')

    Spacer(Modifier.height(30.dp))
    Kicker("Your profile")
    Spacer(Modifier.height(14.dp))
    Text("$greeting,", style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp), color = Ink)
    Text("$firstName.", style = MaterialTheme.typography.displayMedium.copy(fontSize = 38.sp, fontStyle = FontStyle.Italic), color = com.conest.app.ui.theme.Terracotta)
    Spacer(Modifier.height(12.dp))
    Text(
        "This is how you'll appear to people in ${profile.city}. Discover opens in the next build.",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedInk,
    )

    Spacer(Modifier.height(28.dp))

    // Portrait plate with a figure caption, like the welcome page's nest.
    Row(verticalAlignment = Alignment.Bottom) {
        AsyncImage(
            model = profile.photoUrl,
            contentDescription = "Your profile photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .weight(1.15f)
                .aspectRatio(4f / 5f)
                .background(Oatmeal),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                profile.name,
                fontFamily = NewsreaderDisplay,
                fontWeight = FontWeight(400),
                fontSize = 26.sp,
                lineHeight = 28.sp,
                color = Ink,
            )
            Text(
                "${profile.age}",
                fontFamily = NewsreaderDisplay,
                fontStyle = FontStyle.Italic,
                fontSize = 26.sp,
                color = MutedInk,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                listOfNotNull(profile.occupation.ifBlank { null }, profile.occupationType?.label).first(),
                fontFamily = NewsreaderText,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = MutedInk,
            )
            Spacer(Modifier.height(14.dp))
            IntentTag(profile.userType)
        }
    }

    Spacer(Modifier.height(30.dp))
    if (profile.userType == UserType.HasRoom) {
        FactRow("The place", "${profile.preferredLocalities.joinToString()}, ${profile.city}")
    } else {
        FactRow("Looking in", "${profile.preferredLocalities.joinToString()} · ${profile.city}")
    }
    FactRow(
        if (profile.userType == UserType.HasRoom) "Rent" else "Budget",
        if (profile.budgetMin == profile.budgetMax) "${rupeesShort(profile.budgetMin)} / month"
        else "${rupeesShort(profile.budgetMin)} – ${rupeesShort(profile.budgetMax)} / month",
    )
    FactRow(if (profile.userType == UserType.HasRoom) "Room free" else "Moving", moveInLabel(profile.moveIn))
    profile.room?.let { room ->
        FactRow("The room", listOfNotNull(room.type?.label, room.furnishing?.label).joinToString(" · "))
    }
    listOfNotNull(profile.diet?.label, profile.drinking?.let { "Drinks: ${it.label.lowercase()}" })
        .takeIf { it.isNotEmpty() }
        ?.let { FactRow("Habits", it.joinToString(" · ")) }
    Hairline()

    if (profile.bio.isNotBlank()) {
        Spacer(Modifier.height(30.dp))
        Kicker("In your words")
        Spacer(Modifier.height(12.dp))
        Text(
            "“${profile.bio.trim()}”",
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 19.sp,
            lineHeight = 27.sp,
            color = Ink,
        )
    }

    val photos = profile.room?.photoUrls.orEmpty()
    if (photos.isNotEmpty()) {
        Spacer(Modifier.height(30.dp))
        Kicker("The room")
        Spacer(Modifier.height(12.dp))
        photos.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Room photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(4f / 3f)
                            .background(Oatmeal),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    val lifestyle = answers?.summary().orEmpty()
    if (lifestyle.isNotEmpty()) {
        Spacer(Modifier.height(30.dp))
        Kicker("How you live")
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lifestyle.forEach { Tag(it) }
        }
    }

    if (profile.interests.isNotEmpty()) {
        Spacer(Modifier.height(28.dp))
        Kicker("Things you love")
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            profile.interests.forEach { Tag(it) }
        }
    }

    Spacer(Modifier.height(34.dp))
    OutlineAction("Retake the lifestyle questions", onRetakeQuiz)
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun IntentTag(type: UserType?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(8.dp)
                .height(8.dp)
                .background(if (type == UserType.HasRoom) Moss else com.conest.app.ui.theme.Terracotta),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (type == UserType.HasRoom) "HAS A ROOM" else "LOOKING FOR A ROOM",
            style = MaterialTheme.typography.labelSmall,
            color = Ink,
        )
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Hairline()
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk, modifier = Modifier.width(110.dp).padding(top = 3.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        Modifier
            .background(Oatmeal.copy(alpha = 0.6f), MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = Forest)
    }
}

