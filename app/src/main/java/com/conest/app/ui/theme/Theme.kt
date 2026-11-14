package com.conest.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// CoNest is a single, warm, paper-toned identity. Dynamic colour is intentionally off —
// the palette *is* the brand. A dedicated dark palette can come later.
private val CoNestColors = lightColorScheme(
    primary = Forest,
    onPrimary = Linen,
    secondary = Moss,
    onSecondary = Linen,
    tertiary = Terracotta,
    onTertiary = Linen,
    background = Linen,
    onBackground = Ink,
    surface = Linen,
    onSurface = Ink,
    surfaceVariant = Oatmeal,
    onSurfaceVariant = MutedInk,
    outline = Bark,
    outlineVariant = Straw,
)

// Restrained corners: nearly square, never pills.
private val CoNestShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(3.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(6.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun CoNestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoNestColors,
        typography = Typography,
        shapes = CoNestShapes,
        content = content,
    )
}
