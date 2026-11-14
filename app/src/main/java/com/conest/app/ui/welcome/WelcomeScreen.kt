package com.conest.app.ui.welcome

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.TextLink
import com.conest.app.ui.components.Wordmark
import com.conest.app.ui.components.WovenNest
import com.conest.app.ui.components.rise
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.CoNestTheme
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.ForestPressed
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta


/**
 * First impression. Laid out like the opening spread of a magazine: masthead, a single
 * illustrated plate (the nest, with its figure caption), the headline, and one clear action.
 */
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    var revealed by rememberSaveable { mutableStateOf(!animate) }
    LaunchedEffect(Unit) { revealed = true }

    Box(
        modifier
            .fillMaxSize()
            .paperTexture(Linen),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Masthead(Modifier.rise(revealed, 0))

            // The plate: nest + figure caption share this flexible space.
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                WovenNest(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp)
                        .semantics { contentDescription = "Illustration of a woven baya weaver's nest hanging from a twig" },
                    animate = animate,
                )
                FigureCaption(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = Gutter, bottom = 20.dp)
                        .rise(revealed, 1400),
                )
            }

            Column(Modifier.padding(horizontal = Gutter)) {
                Headline(Modifier.rise(revealed, 250))
                Spacer(Modifier.height(18.dp))
                Text(
                    text = "Flatmates matched on how you actually live: your mornings, your late nights, the state of the kitchen sink.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedInk,
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .rise(revealed, 450),
                )
                Spacer(Modifier.height(28.dp))
                PrimaryAction(
                    text = "Get started",
                    onClick = onGetStarted,
                    modifier = Modifier.rise(revealed, 600),
                )
                TextLink("Already have a nest?", "Log in", onLogIn, Modifier.fillMaxWidth().rise(revealed, 700))
            }

            Footer(Modifier.rise(revealed, 850))
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun Masthead(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = Gutter)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Wordmark()
            Text(
                text = "FLATMATES, BY LIFESTYLE",
                style = MaterialTheme.typography.labelSmall,
                color = MutedInk,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Hairline()
    }
}

@Composable
private fun FigureCaption(modifier: Modifier = Modifier) {
    Column(modifier.width(150.dp)) {
        Box(
            Modifier
                .width(20.dp)
                .height(1.dp)
                .background(Terracotta),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = NewsreaderText, fontWeight = FontWeight(500), color = Ink)) {
                    append("Fig. 1  ")
                }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("Ploceus philippinus") }
                append(", the baya weaver, builds its nest one strand at a time.")
            },
            fontFamily = NewsreaderText,
            fontSize = 12.5.sp,
            lineHeight = 16.5.sp,
            color = MutedInk,
        )
    }
}

@Composable
private fun Headline(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Scale so the longest line ("Feel at home.") never wraps on narrow phones.
        val size = minOf(56f, maxWidth.value / 5.5f)
        val style = MaterialTheme.typography.displayLarge.copy(
            fontSize = size.sp,
            lineHeight = (size * 0.98f).sp,
        )
        Column {
            Text("Find your", style = style, color = Ink)
            Text("people.", style = style, color = Ink)
            Text(
                "Feel at home.",
                style = style.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight(300)),
                color = Terracotta,
            )
        }
    }
}

@Composable
private fun Footer(modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = Gutter)) {
        Hairline()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("Hyderabad", "Bengaluru", "Pune", "Kolkata").forEach {
                Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Preview(name = "Welcome", widthDp = 393, heightDp = 852, showBackground = true)
@Composable
private fun WelcomePreview() {
    CoNestTheme { WelcomeScreen(onGetStarted = {}, onLogIn = {}, animate = false) }
}

@Preview(name = "Welcome — compact", widthDp = 360, heightDp = 690, showBackground = true)
@Composable
private fun WelcomeCompactPreview() {
    CoNestTheme { WelcomeScreen(onGetStarted = {}, onLogIn = {}, animate = false) }
}
