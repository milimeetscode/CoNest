package com.conest.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.conest.app.ui.theme.Bark
import com.conest.app.ui.theme.DeepBark
import com.conest.app.ui.theme.DryGrass
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.Moss
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Straw
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A hanging baya-weaver nest, woven strand by strand.
 *
 * Each strand is a curve wrapped around a gourd-shaped body (chamber + entrance tube), so the
 * crossing diagonals read as weave rather than scribble. Geometry is seeded: the nest is the
 * same on every launch — an illustration, not noise.
 *
 * @param animate when true the nest weaves itself in, then sways gently from its twig.
 */
@Composable
fun WovenNest(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    seed: Int = 11,
) {
    val weave = remember { Animatable(if (animate) 0f else 1f) }
    LaunchedEffect(animate) {
        if (animate) weave.animateTo(1f, tween(2600, easing = CubicBezierEasing(0.33f, 0f, 0.2f, 1f)))
    }
    val sway = if (animate) {
        rememberInfiniteTransition(label = "sway").animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(5600, easing = SineEasing), RepeatMode.Reverse),
            label = "sway",
        )
    } else null

    val cache = remember(seed) { NestCache(seed) }

    Canvas(modifier) {
        val g = cache.geometryFor(size, density)
        val w = weave.value // read in draw phase → redraws only, no recomposition
        drawTwig(g, (w / 0.28f).coerceIn(0f, 1f))

        rotate(degrees = (sway?.value ?: 0f) * 0.9f, pivot = g.anchor) {
            // Body shadow keeps gaps between strands reading as depth, not holes.
            val shadow = ((w - 0.08f) / 0.3f).coerceIn(0f, 1f)
            if (shadow > 0f) drawPath(g.silhouette, NestShadow.copy(alpha = 0.78f * shadow))
            g.strands.forEach { drawStrand(it, w) }
            g.strays.forEach { drawStrand(it, w) }
        }
        drawLeaves(g, ((w - 0.55f) / 0.35f).coerceIn(0f, 1f))
    }
}

private val NestShadow = Color(0xFF9A8262)
private val SineEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

// ---------------------------------------------------------------------------------------------
// Geometry
// ---------------------------------------------------------------------------------------------

private class Strand(
    val pts: FloatArray, // x0,y0,x1,y1…
    val color: Color,
    val width: Float,
    val start: Float, // weave progress at which this strand begins
    val duration: Float,
) {
    val full: Path = Path().apply {
        moveTo(pts[0], pts[1])
        var i = 2
        while (i < pts.size) {
            lineTo(pts[i], pts[i + 1]); i += 2
        }
    }
}

private class Leaf(
    val base: Offset,
    val angle: Float,
    val length: Float,
    val width: Float,
    val color: Color,
    val delay: Float,
)

private class TwigSegment(val a: Offset, val b: Offset, val width: Float, val t: Float)

private class NestGeometry(
    val anchor: Offset,
    val silhouette: Path,
    val strands: List<Strand>,
    val strays: List<Strand>,
    val twig: List<TwigSegment>,
    val leaves: List<Leaf>,
)

private class NestCache(private val seed: Int) {
    private var lastSize = Size.Zero
    private var geometry: NestGeometry? = null

    fun geometryFor(size: Size, density: Float): NestGeometry {
        geometry?.let { if (size == lastSize) return it }
        return buildNest(size, density, Random(seed)).also {
            geometry = it
            lastSize = size
        }
    }
}

/** Radius of the nest body (in units of Rn) at normalised height v ∈ [0,1]. */
private val radiusProfile = floatArrayOf(
    0.00f, 0.10f,
    0.05f, 0.24f,
    0.16f, 0.68f,
    0.29f, 0.95f,
    0.41f, 0.93f,
    0.52f, 0.72f,
    0.61f, 0.45f,
    0.71f, 0.34f,
    0.87f, 0.30f,
    1.00f, 0.35f,
)

/** Horizontal drift of the body's centre line — the tube curls slightly, as real ones do. */
private val centreProfile = floatArrayOf(
    0.00f, 0.00f,
    0.50f, 0.00f,
    0.78f, -0.05f,
    1.00f, -0.13f,
)

private fun profile(table: FloatArray, v: Float): Float {
    val x = v.coerceIn(0f, 1f)
    var i = 0
    while (i < table.size - 4 && table[i + 2] < x) i += 2
    val v0 = table[i]
    val r0 = table[i + 1]
    val v1 = table[i + 2]
    val r1 = table[i + 3]
    val t = ((x - v0) / (v1 - v0)).coerceIn(0f, 1f)
    val s = (1f - cos(t * PI.toFloat())) / 2f // cosine ease → smooth silhouette, no kinks
    return r0 + (r1 - r0) * s
}

private const val STRANDS = 980

private fun Random.range(a: Float, b: Float) = a + nextFloat() * (b - a)

private fun lerp(a: Offset, b: Offset, t: Float) = Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun quad(a: Offset, c: Offset, b: Offset, n: Int = 12): FloatArray {
    val out = FloatArray(n * 2)
    for (k in 0 until n) {
        val t = k / (n - 1f)
        val u = 1 - t
        out[k * 2] = u * u * a.x + 2 * u * t * c.x + t * t * b.x
        out[k * 2 + 1] = u * u * a.y + 2 * u * t * c.y + t * t * b.y
    }
    return out
}

private class RawStrand(val pts: FloatArray, val layer: Int, val color: Color, val width: Float, val sortKey: Float)

private fun buildNest(size: Size, dp: Float, rnd: Random): NestGeometry {
    val w = size.width
    val h = size.height
    val halfPi = PI.toFloat() / 2f

    // --- twig: a sagging quadratic from beyond the right edge toward the left ----------------
    val p0 = Offset(w + 12 * dp, h * 0.07f)
    val p1 = Offset(w * 0.62f, h * 0.17f)
    val p2 = Offset(w * 0.14f, h * 0.05f)
    fun twigAt(t: Float): Offset {
        val u = 1 - t
        return Offset(
            u * u * p0.x + 2 * u * t * p1.x + t * t * p2.x,
            u * u * p0.y + 2 * u * t * p1.y + t * t * p2.y,
        )
    }

    val twig = mutableListOf<TwigSegment>()
    val steps = 40
    for (i in 0 until steps) {
        val t0 = i / steps.toFloat()
        val t1 = (i + 1) / steps.toFloat()
        twig += TwigSegment(twigAt(t0), twigAt(t1), (5.2f - 4.0f * t0) * dp, t0)
    }
    // a short offshoot reaching up-left
    val forkT = 0.72f
    val fork = twigAt(forkT)
    val forkEnd = fork + Offset(-26 * dp, -30 * dp)
    for (i in 0 until 10) {
        val t0 = i / 10f
        val t1 = (i + 1) / 10f
        twig += TwigSegment(lerp(fork, forkEnd, t0), lerp(fork, forkEnd, t1), (2.0f - 1.3f * t0) * dp, forkT + 0.1f * t0)
    }

    // the nest hangs from the twig a little right of centre
    val targetX = w * 0.6f
    var anchorT = 0.5f
    var best = Float.MAX_VALUE
    for (i in 0..200) {
        val t = i / 200f
        val dx = abs(twigAt(t).x - targetX)
        if (dx < best) {
            best = dx; anchorT = t
        }
    }
    val anchor = twigAt(anchorT) + Offset(0f, 1.5f * dp)
    val hn = (h - anchor.y) * 0.86f
    val rn = minOf(hn * 0.27f, w * 0.31f)
    val persp = 0.2f

    fun bodyX(theta: Float, v: Float) =
        anchor.x + rn * (profile(centreProfile, v) + profile(radiusProfile, v) * sin(theta))

    fun bodyY(theta: Float, v: Float) =
        anchor.y + hn * v + rn * profile(radiusProfile, v) * persp * cos(theta)

    // --- silhouette --------------------------------------------------------------------------
    val silhouette = Path().apply {
        val n = 80
        moveTo(bodyX(-halfPi, 0f), bodyY(-halfPi, 0f))
        for (i in 1..n) {
            val v = i / n.toFloat(); lineTo(bodyX(-halfPi, v), bodyY(-halfPi, v))
        }
        for (i in 0..20) {
            val th = -halfPi + 2 * halfPi * i / 20f; lineTo(bodyX(th, 1f), bodyY(th, 1f))
        }
        for (i in n downTo 0) {
            val v = i / n.toFloat(); lineTo(bodyX(halfPi, v), bodyY(halfPi, v))
        }
        close()
    }

    // --- woven strands -----------------------------------------------------------------------
    val raws = ArrayList<RawStrand>(STRANDS)
    repeat(STRANDS) {
        val r = rnd.nextFloat()
        val v0 = r * r * 0.25f + r * 0.75f // slight bias toward the chamber
        val thA = rnd.range(-1.72f, 1.15f)
        val thB = minOf(thA + rnd.range(0.55f, 2.3f), 1.68f)
        val dir = if (rnd.nextBoolean()) 1f else -1f
        val slope = dir * rnd.range(0.012f, 0.065f) // ± diagonals cross → weave
        val wobFreq = rnd.range(3f, 7f)
        val wobPhase = rnd.range(0f, 6.28f)
        val wobAmp = rnd.range(0.002f, 0.008f)
        val n = 16
        val pts = FloatArray(n * 2)
        for (k in 0 until n) {
            val th = thA + (thB - thA) * k / (n - 1)
            val v = (v0 + slope * (th - thA) + wobAmp * sin(th * wobFreq + wobPhase)).coerceIn(0f, 1.01f)
            pts[k * 2] = bodyX(th, v)
            pts[k * 2 + 1] = bodyY(th, v)
        }
        val centre = (thA + thB) / 2

        // light from the upper left: the right flank and the tube sit in shadow
        val roll = rnd.nextFloat()
        var layer = if (roll < 0.30f) 0 else if (roll < 0.72f) 1 else 2
        if (centre > 0.55f && layer == 2 && rnd.nextFloat() < 0.6f) layer = 1
        if (centre > 0.9f && layer == 1 && rnd.nextFloat() < 0.4f) layer = 0
        if (centre < -0.4f && layer == 0 && rnd.nextFloat() < 0.5f) layer = 1
        if (v0 > 0.6f && layer == 2 && rnd.nextFloat() < 0.35f) layer = 1

        // the odd green strand — baya weavers build with fresh grass, which dries in place
        val freshGrass = rnd.nextFloat() < 0.045f
        val base = when {
            freshGrass -> Moss
            layer == 0 -> if (rnd.nextBoolean()) DeepBark else Color(0xFF8C7454)
            layer == 1 -> if (rnd.nextFloat() < 0.55f) DryGrass else Bark
            else -> rnd.nextFloat().let { if (it < 0.55f) Straw else if (it < 0.9f) Oatmeal else Linen }
        }
        val width = rnd.range(0.6f, 1.6f) * dp * (if (layer == 2) 0.85f else 1f)
        raws += RawStrand(
            pts = pts,
            layer = if (freshGrass) 1 else layer,
            color = base.copy(alpha = rnd.range(0.72f, 1f)),
            width = width,
            sortKey = pts[1] + rnd.range(-40f, 40f) * dp,
        )
    }
    // dark first, light on top; within a layer roughly top-down, so the weave "grows"
    val sorted = raws.sortedWith(compareBy<RawStrand> { it.layer }.thenBy { it.sortKey })
    val strands = sorted.mapIndexed { i, s ->
        Strand(s.pts, s.color, s.width, start = 0.06f + 0.66f * i / sorted.size, duration = 0.2f)
    }

    // --- loose ends: binding over the twig, flyaway fibres, grass trailing from the tube -----
    val strays = mutableListOf<Strand>()
    val strayColors = listOf(DryGrass, Straw, Bark, Straw, DeepBark)
    fun strayColor(limit: Int = strayColors.size) = strayColors[rnd.nextInt(limit)]

    repeat(34) { // binding strands looping up over the twig
        val from = Offset(anchor.x + rnd.range(-0.3f, 0.3f) * rn, anchor.y + hn * rnd.range(0.015f, 0.08f))
        val to = Offset(anchor.x + rnd.range(-0.55f, 0.55f) * rn, anchor.y - rnd.range(2f, 7f) * dp)
        val ctrl = Offset((from.x + to.x) / 2 + rnd.range(-12f, 12f) * dp, to.y - rnd.range(4f, 12f) * dp)
        strays += Strand(quad(from, ctrl, to), strayColor().copy(alpha = 0.95f), rnd.range(0.8f, 1.6f) * dp, rnd.range(0.62f, 0.8f), 0.16f)
    }
    repeat(70) { // flyaways along the flanks
        val v = rnd.range(0.06f, 0.95f)
        val side = if (rnd.nextBoolean()) 1f else -1f
        val th = side * rnd.range(1.35f, 1.6f)
        val a = Offset(bodyX(th, v), bodyY(th, v))
        val len = rnd.range(5f, 26f) * dp
        val ang = rnd.range(-0.9f, 0.9f) + if (side > 0) 0f else PI.toFloat()
        val b = a + Offset(cos(ang) * len, sin(ang) * len + len * 0.3f)
        val c = (a + b) / 2f + Offset(rnd.range(-5f, 5f) * dp, rnd.range(-5f, 5f) * dp)
        strays += Strand(quad(a, c, b, 8), strayColor().copy(alpha = rnd.range(0.55f, 0.95f)), rnd.range(0.5f, 1.1f) * dp, rnd.range(0.7f, 0.9f), 0.12f)
    }
    repeat(16) { // grass trailing from the entrance tube
        val th = rnd.range(-1.3f, 1.3f)
        val a = Offset(bodyX(th, 0.985f), bodyY(th, 0.985f))
        val room = (h - a.y).coerceAtLeast(16f * dp)
        val len = rnd.range(0.3f, 0.9f) * room
        val b = a + Offset(rnd.range(-16f, 10f) * dp, len)
        val c = Offset(a.x + rnd.range(-14f, 14f) * dp, a.y + len * 0.5f)
        strays += Strand(quad(a, c, b), strayColor(3).copy(alpha = rnd.range(0.6f, 0.95f)), rnd.range(0.6f, 1.3f) * dp, rnd.range(0.76f, 0.88f), 0.14f)
    }

    // --- leaves: a few slender ones, one gone dry --------------------------------------------
    val leaves = listOf(
        Leaf(forkEnd, -2.2f, 30 * dp, 7 * dp, Moss, 0.0f),
        Leaf(twigAt(0.86f), 2.5f, 26 * dp, 6 * dp, Moss.copy(alpha = 0.9f), 0.15f),
        Leaf(twigAt(0.16f), -1.25f, 22 * dp, 5.5f * dp, Bark, 0.3f),
        Leaf(twigAt(0.97f), -2.9f, 20 * dp, 5 * dp, Moss.copy(alpha = 0.85f), 0.45f),
    )

    return NestGeometry(anchor, silhouette, strands, strays, twig, leaves)
}

// ---------------------------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------------------------

private fun DrawScope.drawStrand(s: Strand, weave: Float) {
    val p = ((weave - s.start) / s.duration).coerceIn(0f, 1f)
    if (p <= 0f) return
    val style = Stroke(width = s.width, cap = StrokeCap.Round)
    if (p >= 1f) {
        drawPath(s.full, s.color, style = style); return
    }

    val segs = s.pts.size / 2 - 1
    val reach = p * segs
    val whole = reach.toInt()
    val path = Path().apply {
        moveTo(s.pts[0], s.pts[1])
        for (k in 1..whole) lineTo(s.pts[k * 2], s.pts[k * 2 + 1])
        if (whole < segs) {
            val f = reach - whole
            val x0 = s.pts[whole * 2]
            val y0 = s.pts[whole * 2 + 1]
            val x1 = s.pts[whole * 2 + 2]
            val y1 = s.pts[whole * 2 + 3]
            lineTo(x0 + (x1 - x0) * f, y0 + (y1 - y0) * f)
        }
    }
    drawPath(path, s.color, style = style)
}

private fun DrawScope.drawTwig(g: NestGeometry, p: Float) {
    if (p <= 0f) return
    g.twig.forEach { seg ->
        if (seg.t <= p) drawLine(DeepBark, seg.a, seg.b, strokeWidth = seg.width, cap = StrokeCap.Round)
    }
    // a thin highlight along the top edge of the main branch
    g.twig.forEach { seg ->
        if (seg.t > p || seg.width < 2.4f * density) return@forEach
        val lift = Offset(0f, -seg.width * 0.28f)
        drawLine(Bark.copy(alpha = 0.55f), seg.a + lift, seg.b + lift, strokeWidth = seg.width * 0.25f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawLeaves(g: NestGeometry, p: Float) {
    if (p <= 0f) return
    g.leaves.forEach { leaf ->
        val lp = ((p - leaf.delay) / (1f - leaf.delay)).coerceIn(0f, 1f)
        if (lp <= 0f) return@forEach
        val dir = Offset(cos(leaf.angle), sin(leaf.angle))
        val nrm = Offset(-dir.y, dir.x)
        val tip = leaf.base + dir * leaf.length
        val mid = leaf.base + dir * (leaf.length * 0.45f)
        val c1 = mid + nrm * leaf.width
        val c2 = mid - nrm * (leaf.width * 0.8f)
        val shape = Path().apply {
            moveTo(leaf.base.x, leaf.base.y)
            quadraticTo(c1.x, c1.y, tip.x, tip.y)
            quadraticTo(c2.x, c2.y, leaf.base.x, leaf.base.y)
            close()
        }
        scale(lp, lp, pivot = leaf.base) {
            drawPath(shape, leaf.color)
            drawLine(
                Forest.copy(alpha = 0.35f), leaf.base, leaf.base + dir * (leaf.length * 0.85f),
                strokeWidth = 0.7f * density, cap = StrokeCap.Round,
            )
        }
    }
}
