package com.conest.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.ForestPressed
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Straw

/** Page gutter shared by every screen. */
val Gutter = 24.dp

/** "Co" set upright, "Nest" in italic — the two halves of the name, woven together. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, size: Float = 23f) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight(500))) { append("Co") }
            withStyle(SpanStyle(fontStyle = FontStyle.Italic, fontWeight = FontWeight(400))) { append("Nest") }
        },
        fontFamily = NewsreaderDisplay,
        fontSize = size.sp,
        letterSpacing = (-0.01).em,
        color = Forest,
        modifier = modifier,
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier, color: Color = Straw, thickness: Dp = 1.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}

/**
 * The one solid action on a screen: near-square forest slab, label left, drawn arrow right.
 * While [loading], the arrow gives way to a slow weaving dash and taps are ignored.
 */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    loadingText: String = text,
    /** Visibly not ready (e.g. a step is incomplete) but still tappable, so it never feels broken. */
    dimmed: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val nudge by animateFloatAsState(if (pressed && !loading) 1f else 0f, tween(180), label = "nudge")

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .background(
                (if (pressed || loading) ForestPressed else Forest).copy(alpha = if (dimmed && !loading) 0.42f else 1f),
                MaterialTheme.shapes.extraSmall,
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Linen),
                enabled = !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "label",
            modifier = Modifier.weight(1f),
        ) { busy ->
            Text(
                if (busy) loadingText else text,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
                color = Linen,
            )
        }
        if (loading) {
            WeavingDots(Modifier.size(width = 26.dp, height = 12.dp))
        } else {
            LineArrow(
                Modifier
                    .size(width = 26.dp, height = 12.dp)
                    .graphicsLayer { translationX = nudge * 5.dp.toPx() },
            )
        }
    }
}

/** Secondary action: hairline-bordered, linen ground, optional leading mark. */
@Composable
fun OutlineAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .background(if (pressed) Oatmeal.copy(alpha = 0.6f) else Color.Transparent, MaterialTheme.shapes.extraSmall)
            .border(1.dp, Ink.copy(alpha = 0.55f), MaterialTheme.shapes.extraSmall)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = Forest),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = Ink)
    }
}

/** A drawn hairline arrow — lighter than an icon glyph, matches the editorial rules. */
@Composable
fun LineArrow(modifier: Modifier = Modifier, color: Color = Linen, pointsLeft: Boolean = false) {
    Canvas(modifier) {
        val stroke = 1.3.dp.toPx()
        val midY = size.height / 2
        val head = size.height * 0.45f
        val tipX = if (pointsLeft) 0f else size.width
        val backX = if (pointsLeft) head else size.width - head
        drawLine(color, Offset(0f, midY), Offset(size.width, midY), stroke, StrokeCap.Round)
        drawLine(color, Offset(backX, midY - head), Offset(tipX, midY), stroke, StrokeCap.Round)
        drawLine(color, Offset(backX, midY + head), Offset(tipX, midY), stroke, StrokeCap.Round)
    }
}

/** Three short strands that lift in turn — the loading state, in the nest's own vocabulary. */
@Composable
private fun WeavingDots(modifier: Modifier = Modifier) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "weaving")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(tween(1100, easing = androidx.compose.animation.core.LinearEasing)),
        label = "phase",
    )
    Canvas(modifier) {
        val stroke = 1.6.dp.toPx()
        val seg = size.width / 3.6f
        for (i in 0 until 3) {
            val local = ((phase * 3f - i) % 3f + 3f) % 3f
            val lift = if (local < 1f) kotlin.math.sin(local * Math.PI).toFloat() else 0f
            val x = i * seg * 1.3f
            val y = size.height / 2 - lift * size.height * 0.35f
            drawLine(Linen.copy(alpha = 0.55f + 0.45f * lift), Offset(x, y), Offset(x + seg, y), stroke, StrokeCap.Round)
        }
    }
}

/** "Prompt  Link" — muted lead-in, underlined ink action. Full-width 52dp touch target. */
@Composable
fun TextLink(
    prompt: String,
    action: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
) {
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button, onClickLabel = action, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
    ) {
        Text(
            text = buildAnnotatedString {
                if (prompt.isNotEmpty()) append("$prompt  ")
                withStyle(SpanStyle(color = Ink, fontWeight = FontWeight(600), textDecoration = TextDecoration.Underline)) {
                    append(action)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MutedInk,
        )
    }
}

/** Staggered entrance: fade in while rising a few dp. Read in the layer, so no recomposition. */
fun Modifier.rise(visible: Boolean, delayMillis: Int): Modifier = composed {
    val p by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700, delayMillis = delayMillis),
        label = "rise",
    )
    Modifier.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 10.dp.toPx()
    }
}
