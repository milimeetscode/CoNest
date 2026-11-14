package com.conest.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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

/** Small-caps label with the terracotta rule — the section marker used across the app. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(20.dp)
                .height(1.dp)
                .background(Terracotta),
        )
        Spacer(Modifier.width(10.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk)
    }
}

/** A plain small-caps field label, for groups of chips or cards. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, hint: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk)
        if (hint != null) {
            Spacer(Modifier.weight(1f))
            Text(hint, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = MutedInk)
        }
    }
}

/**
 * Two-line editorial headline: an upright line and an italic terracotta line.
 * Scales with width so neither line wraps on a narrow phone.
 */
@Composable
fun SplitHeadline(first: String, second: String, modifier: Modifier = Modifier, maxSize: Float = 40f) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        val longest = maxOf(first.length, second.length).coerceAtLeast(8)
        val size = minOf(maxSize, maxWidth.value / (longest * 0.47f))
        val style = MaterialTheme.typography.displayMedium.copy(fontSize = size.sp, lineHeight = (size * 1.04f).sp)
        Column {
            Text(first, style = style, color = Ink)
            Text(second, style = style.copy(fontStyle = FontStyle.Italic), color = Terracotta)
        }
    }
}

/**
 * The selection card used for intent and quiz answers: a hairline frame with a lettered
 * marker, serif title and one line of explanation. Selected → forest frame, oatmeal wash.
 */
@Composable
fun ChoiceCard(
    marker: String,
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val frame by animateColorAsState(if (selected) Forest else Straw, label = "frame")
    val wash by animateColorAsState(if (selected) Oatmeal.copy(alpha = 0.55f) else Linen.copy(alpha = 0f), label = "wash")
    val weight by animateDpAsState(if (selected) 1.5.dp else 1.dp, label = "weight")

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(wash, MaterialTheme.shapes.extraSmall)
            .border(weight, frame, MaterialTheme.shapes.extraSmall)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            marker,
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = if (selected) Forest else Bark,
            modifier = Modifier.width(26.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = NewsreaderDisplay, fontWeight = FontWeight(500), fontSize = 20.sp, lineHeight = 24.sp, color = Ink)
            if (detail.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
        }
        Spacer(Modifier.width(12.dp))
        // a small square that fills in — a tick-box, not a radio circle
        Box(
            Modifier
                .size(10.dp)
                .border(1.dp, if (selected) Forest else Straw)
                .background(if (selected) Forest else Linen.copy(alpha = 0f)),
        )
    }
}

/** Near-square chip. Selected chips fill with forest; the rest are hairline outlines. */
@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val bg by animateColorAsState(if (selected) Forest else Linen.copy(alpha = 0f), label = "bg")
    val fg by animateColorAsState(if (selected) Linen else if (enabled) Ink else MutedInk.copy(alpha = 0.5f), label = "fg")
    Box(
        modifier
            .heightIn(min = 40.dp)
            .background(bg, MaterialTheme.shapes.extraSmall)
            .border(1.dp, if (selected) Forest else Straw, MaterialTheme.shapes.extraSmall)
            .toggleable(value = selected, enabled = enabled || selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = fg)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    options: List<String>,
    selected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: (String) -> Boolean = { true },
) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { SelectChip(it, selected(it), { onToggle(it) }, enabled = enabled(it)) }
    }
}

/** Segmented progress: done segments forest, current terracotta, upcoming straw. */
@Composable
fun StepProgress(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val color by animateColorAsState(
                when {
                    i < current - 1 -> Forest
                    i == current - 1 -> Terracotta
                    else -> Straw
                },
                label = "seg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(2.dp)
                    .background(color),
            )
        }
    }
}

/**
 * Photo slot: a dashed straw frame with a drawn plus until filled, then the image edge to edge.
 * [model] can be a local Uri or a remote URL — anything Coil loads.
 */
@Composable
fun PhotoSlot(
    model: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspect: Float = 4f / 5f,
    emptyLabel: String = "Add a photo",
    onClickLabel: String = emptyLabel,
) {
    Box(
        modifier
            .aspectRatio(aspect)
            .background(Oatmeal.copy(alpha = 0.35f), MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val inset = 1.dp.toPx()
                drawRect(
                    Bark.copy(alpha = 0.6f),
                    topLeft = Offset(inset, inset),
                    size = size.copy(width = size.width - 2 * inset, height = size.height - 2 * inset),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(18.dp)) {
                    val s = 1.3.dp.toPx()
                    drawLine(Ink, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), s, StrokeCap.Round)
                    drawLine(Ink, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), s, StrokeCap.Round)
                }
                Spacer(Modifier.height(10.dp))
                Text(emptyLabel, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
        }
    }
}
