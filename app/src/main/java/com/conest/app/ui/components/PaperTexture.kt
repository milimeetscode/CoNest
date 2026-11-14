package com.conest.app.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.conest.app.ui.theme.Bark
import com.conest.app.ui.theme.Straw
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Handmade-paper ground: a fine tooth of grain plus a scattering of pressed fibres.
 * Deliberately faint — it should be felt more than seen. Built once per size and cached.
 */
fun Modifier.paperTexture(base: Color, seed: Int = 3): Modifier = drawWithCache {
    val rnd = Random(seed)
    val dp = density
    val area = size.width * size.height

    fun scatter(perDp2: Float, cap: Int) =
        List((area / (perDp2 * dp * dp)).toInt().coerceAtMost(cap)) {
            Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
        }

    val darkGrain = scatter(70f, 40_000)
    val lightGrain = scatter(110f, 30_000)
    val fibres = List((area / (5200f * dp * dp)).toInt().coerceAtMost(260)) {
        val start = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
        val ang = rnd.nextFloat() * 6.28f
        val len = (4f + rnd.nextFloat() * 16f) * dp
        val bend = (rnd.nextFloat() - 0.5f) * 6f * dp
        val end = start + Offset(cos(ang) * len, sin(ang) * len)
        val ctrl = (start + end) / 2f + Offset(-sin(ang) * bend, cos(ang) * bend)
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
        }
        path to (rnd.nextFloat() * 0.5f + 0.25f)
    }
    val fibreStroke = Stroke(width = 0.6f * dp, cap = StrokeCap.Round)

    onDrawBehind {
        drawRect(base)
        drawPoints(darkGrain, PointMode.Points, Bark.copy(alpha = 0.10f), strokeWidth = 0.9f * dp, cap = StrokeCap.Round)
        drawPoints(lightGrain, PointMode.Points, Color.White.copy(alpha = 0.35f), strokeWidth = 1.1f * dp, cap = StrokeCap.Round)
        fibres.forEach { (path, a) -> drawPath(path, Straw.copy(alpha = a * 0.6f), style = fibreStroke) }
    }
}
