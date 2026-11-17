package com.conest.app.ui.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.OutlineAction
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.SplitHeadline
import com.conest.app.ui.components.Wordmark
import com.conest.app.ui.components.WovenNest
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk

/** While auth and the profile resolve. Fades the wordmark in late so quick loads show nothing. */
@Composable
fun LoadingScreen() {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }
    val a by animateFloatAsState(if (show) 1f else 0f, tween(600, delayMillis = 400), label = "a")
    Box(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen),
        contentAlignment = Alignment.Center,
    ) {
        Wordmark(Modifier.alpha(a), size = 30f)
    }
}

/** Shown until app/google-services.json exists — a developer-facing state, stated plainly. */
@Composable
fun NotConfiguredScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Gutter),
        verticalArrangement = Arrangement.Center,
    ) {
        Kicker("Setup needed")
        Spacer(Modifier.height(16.dp))
        SplitHeadline("Firebase isn't", "connected yet.")
        Spacer(Modifier.height(16.dp))
        Text(
            "Add the Firebase config file and rebuild:",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
        )
        Spacer(Modifier.height(10.dp))
        Text("app/google-services.json", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = Ink)
    }
}

@Composable
fun ProfileUnavailableScreen(onRetry: () -> Unit, onSignOut: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Gutter),
        verticalArrangement = Arrangement.Center,
    ) {
        Kicker("Hmm")
        Spacer(Modifier.height(16.dp))
        SplitHeadline("We couldn't reach", "your profile.")
        Spacer(Modifier.height(14.dp))
        Text(
            "It may be your connection. Your account is fine.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
        )
        Spacer(Modifier.height(30.dp))
        PrimaryAction("Try again", onRetry)
        Spacer(Modifier.height(12.dp))
        OutlineAction("Sign out", onSignOut)
    }
}

/** End of first-time setup — the nest from the welcome page returns, now complete. */
@Composable
fun SetupDoneScreen(onContinue: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        WovenNest(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        Column(Modifier.padding(horizontal = Gutter)) {
            Kicker("All set")
            Spacer(Modifier.height(16.dp))
            SplitHeadline("Your nest", "is ready.", maxSize = 48f)
            Spacer(Modifier.height(14.dp))
            Text(
                "Now meet people who live the way you do. The best fits come first.",
                style = MaterialTheme.typography.bodyLarge,
                color = MutedInk,
            )
            Spacer(Modifier.height(30.dp))
            PrimaryAction("Start exploring", onContinue)
            Spacer(Modifier.height(20.dp))
        }
    }
}
