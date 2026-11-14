package com.conest.app

import com.conest.app.domain.lifestyle.CompatibilityCalculator
import com.conest.app.domain.lifestyle.LifestyleAnswers
import com.conest.app.domain.lifestyle.LifestyleCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityCalculatorTest {

    private fun answers(vararg pairs: Pair<LifestyleCategory, Int>) = LifestyleAnswers(mapOf(*pairs))
    private fun all(value: Int) = LifestyleAnswers(LifestyleCategory.entries.associateWith { value })

    @Test fun identicalAnswersScore100() {
        val c = CompatibilityCalculator.calculate(all(1), all(1))!!
        assertEquals(100, c.score)
        assertTrue(c.dealBreakers.isEmpty())
    }

    @Test fun incompleteAnswersGiveNoScore() {
        val partial = answers(LifestyleCategory.Cleanliness to 0)
        assertNull(CompatibilityCalculator.calculate(partial, all(0)))
    }

    @Test fun oppositeEndsScoreZero() {
        // index 0 vs each question's last option on every weighted category
        val a = all(0)
        val b = LifestyleAnswers(LifestyleCategory.entries.associateWith { c ->
            com.conest.app.domain.lifestyle.LifestyleQuestions.forCategory(c).options.lastIndex
        })
        val c = CompatibilityCalculator.calculate(a, b)!!
        assertEquals(0, c.score)
        assertEquals(listOf(LifestyleCategory.Pets, LifestyleCategory.Smoking), c.dealBreakers)
    }

    @Test fun cleanlinessCarriesTheMostWeight() {
        val base = all(0)
        val offClean = LifestyleAnswers(base.values + (LifestyleCategory.Cleanliness to 3))
        val offFood = LifestyleAnswers(base.values + (LifestyleCategory.Food to 2))
        val clean = CompatibilityCalculator.calculate(base, offClean)!!.score
        val food = CompatibilityCalculator.calculate(base, offFood)!!.score
        assertEquals(80, clean)
        assertEquals(95, food)
    }
}
