package com.conest.app.domain.lifestyle

/**
 * The lifestyle questionnaire (PRD §5.4). Every question's options sit on a single spectrum,
 * ordered, so similarity is simply distance along it. Weights follow PRD §5.6; pets and smoking
 * carry no weight but can raise a deal-breaker.
 */
enum class LifestyleCategory(val key: String, val weight: Int, val dealBreaker: Boolean = false) {
    Cleanliness("cleanliness", 20),
    SleepSchedule("sleepSchedule", 15),
    SocialHabits("socialHabits", 15),
    Noise("noisePreference", 15),
    WorkRoutine("workRoutine", 10),
    SharedResponsibilities("sharedResponsibilities", 10),
    Privacy("privacyPreference", 10),
    Food("foodHabits", 5),
    Pets("pets", 0, dealBreaker = true),
    Smoking("smoking", 0, dealBreaker = true),
}

data class LifestyleOption(val title: String, val detail: String, val summary: String)

data class LifestyleQuestion(
    val category: LifestyleCategory,
    val kicker: String,
    val prompt: String,
    val why: String,
    val options: List<LifestyleOption>,
)

object LifestyleQuestions {
    val all: List<LifestyleQuestion> = listOf(
        LifestyleQuestion(
            LifestyleCategory.Cleanliness, "The state of things",
            "How tidy do you like home to be?",
            "Different standards here cause more friction than anything else.",
            listOf(
                LifestyleOption("Spotless", "Everything has a place, and it goes back there daily.", "Spotless"),
                LifestyleOption("Tidy, mostly", "A quick reset every few days keeps me happy.", "Tidy, mostly"),
                LifestyleOption("Lived-in", "Some clutter is fine, as long as it's clean.", "Lived-in"),
                LifestyleOption("Relaxed", "I'll get to it. Probably on Sunday.", "Relaxed about mess"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.SleepSchedule, "Mornings & nights",
            "When does your day usually begin?",
            "Shared walls make sleep schedules everyone's business.",
            listOf(
                LifestyleOption("Before 6:30", "Mornings are my best hours.", "Early riser"),
                LifestyleOption("6:30 to 8:30", "A fairly regular start.", "Regular hours"),
                LifestyleOption("8:30 to 10:30", "Slow mornings, later evenings.", "Late starter"),
                LifestyleOption("After 10:30", "Night owl. Don't call before noon.", "Night owl"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.SocialHabits, "Company",
            "How often would you have friends over?",
            "Guests change how a home feels, for everyone in it.",
            listOf(
                LifestyleOption("Rarely", "Home is my quiet place.", "Rarely hosts"),
                LifestyleOption("Now and then", "A dinner or two a month.", "Hosts now and then"),
                LifestyleOption("Most weekends", "Friends drop by fairly often.", "Weekend host"),
                LifestyleOption("Often", "The door's usually open.", "Always hosting"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.Noise, "Sound",
            "What does home sound like to you?",
            "One person's background music is another's distraction.",
            listOf(
                LifestyleOption("Quiet", "Headphones on for almost everything.", "Quiet at home"),
                LifestyleOption("Soft background", "Some music or TV, kept low.", "Some background noise"),
                LifestyleOption("Lively", "Music, calls, cooking, the works.", "Lively home"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.WorkRoutine, "Weekdays",
            "Where do you spend your working day?",
            "It decides who's home, and when the flat needs to be quiet.",
            listOf(
                LifestyleOption("At home", "I work or study from home most days.", "Works from home"),
                LifestyleOption("A bit of both", "Hybrid, a few days out.", "Hybrid"),
                LifestyleOption("Out all day", "Office or campus, home by evening.", "Out all day"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.SharedResponsibilities, "Chores",
            "How should the housework get done?",
            "Agreeing on this early saves a lot of quiet resentment.",
            listOf(
                LifestyleOption("A fixed rota", "Clear turns, fair and simple.", "Prefers a chore rota"),
                LifestyleOption("As we go", "Whoever's free picks it up.", "Chores as we go"),
                LifestyleOption("Hire help", "Split the cost of a cleaner or cook.", "Prefers hired help"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.Privacy, "Togetherness",
            "How much time together feels right?",
            "Some people want a flatmate, others want a friend. Both are fine.",
            listOf(
                LifestyleOption("Mostly my own space", "Friendly, but separate lives.", "Values privacy"),
                LifestyleOption("Some shared evenings", "A meal or a film now and then.", "Some shared time"),
                LifestyleOption("Flatmates as friends", "Meals, plans, weekends together.", "Flatmates as friends"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.Food, "The kitchen",
            "What happens in your kitchen?",
            "A busy kitchen needs a little more coordination.",
            listOf(
                LifestyleOption("I cook most meals", "The stove sees me daily.", "Cooks most meals"),
                LifestyleOption("Some of each", "A bit of cooking, a bit of ordering.", "Cooks sometimes"),
                LifestyleOption("Mostly ordering in", "The kitchen is mostly for tea.", "Mostly orders in"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.Pets, "Pets",
            "How do you feel about pets at home?",
            "We'll treat a clear mismatch here as a deal-breaker.",
            listOf(
                LifestyleOption("I have one, or want one", "A home isn't a home without one.", "Pet person"),
                LifestyleOption("Fine either way", "Happy with or without.", "Fine with pets"),
                LifestyleOption("I'd rather not", "Allergies, or just a preference.", "Prefers no pets"),
            ),
        ),
        LifestyleQuestion(
            LifestyleCategory.Smoking, "Smoking",
            "And smoking at home?",
            "Another one we'll flag clearly if it doesn't line up.",
            listOf(
                LifestyleOption("I smoke", "Happy to keep it to the balcony.", "Smoker"),
                LifestyleOption("Outside is fine", "Balcony or terrace only.", "Okay with smoking outside"),
                LifestyleOption("Not at all", "A smoke-free home, please.", "Smoke-free home"),
            ),
        ),
    )

    fun forCategory(category: LifestyleCategory) = all.first { it.category == category }
}

/** Answers keyed by category; value is the selected option's index on that question's spectrum. */
data class LifestyleAnswers(val values: Map<LifestyleCategory, Int> = emptyMap()) {
    val isComplete: Boolean get() = LifestyleCategory.entries.all { values.containsKey(it) }

    /** Short human phrases for profile cards, e.g. "Tidy, mostly · Early riser". */
    fun summary(categories: List<LifestyleCategory> = LifestyleCategory.entries): List<String> =
        categories.mapNotNull { c -> values[c]?.let { LifestyleQuestions.forCategory(c).options.getOrNull(it)?.summary } }
}
