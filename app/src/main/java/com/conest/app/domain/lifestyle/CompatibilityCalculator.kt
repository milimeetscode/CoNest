package com.conest.app.domain.lifestyle

import kotlin.math.abs
import kotlin.math.roundToInt

data class Compatibility(
    /** 0–100, weighted per PRD §5.6. An estimate, never a guarantee. */
    val score: Int,
    val categoryScores: Map<LifestyleCategory, Int>,
    /** Categories where the two answers sit at opposite ends of a deal-breaker question. */
    val dealBreakers: List<LifestyleCategory>,
)

object CompatibilityCalculator {

    /**
     * Returns null unless both people answered every weighted question — an incomplete quiz
     * must not produce a misleading score (PRD §5.4).
     */
    fun calculate(a: LifestyleAnswers, b: LifestyleAnswers): Compatibility? {
        val weighted = LifestyleCategory.entries.filter { it.weight > 0 }
        if (weighted.any { it !in a.values || it !in b.values }) return null

        val perCategory = weighted.associateWith { c ->
            val span = LifestyleQuestions.forCategory(c).options.size - 1
            val distance = abs(a.values.getValue(c) - b.values.getValue(c)).toFloat()
            (100f * (1f - distance / span)).roundToInt()
        }
        val totalWeight = weighted.sumOf { it.weight }
        val score = (weighted.sumOf { perCategory.getValue(it) * it.weight }.toFloat() / totalWeight).roundToInt()

        val dealBreakers = LifestyleCategory.entries.filter { c ->
            if (!c.dealBreaker) return@filter false
            val x = a.values[c] ?: return@filter false
            val y = b.values[c] ?: return@filter false
            val last = LifestyleQuestions.forCategory(c).options.lastIndex
            (x == 0 && y == last) || (x == last && y == 0)
        }
        return Compatibility(score, perCategory, dealBreakers)
    }
}
