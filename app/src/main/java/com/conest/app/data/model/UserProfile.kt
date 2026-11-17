package com.conest.app.data.model

/** Room intent — the two sides of the CoNest marketplace (PRD §1.4). */
enum class UserType(val key: String) {
    NeedsRoom("needs_room"),
    HasRoom("has_room");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class OccupationType(val key: String, val label: String) {
    Student("student", "Student"),
    Professional("professional", "Working professional");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class Diet(val key: String, val label: String) {
    Vegetarian("veg", "Vegetarian"),
    Eggetarian("egg", "Eggetarian"),
    NonVeg("nonveg", "Non-vegetarian"),
    Vegan("vegan", "Vegan");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class Drinking(val key: String, val label: String) {
    Never("never", "Never"),
    Socially("social", "Socially"),
    Regularly("regular", "Regularly");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class RoomType(val key: String, val label: String) {
    Private("private", "Private room"),
    Shared("shared", "Shared room");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key }
    }
}

enum class Furnishing(val key: String, val label: String) {
    Furnished("furnished", "Furnished"),
    Semi("semi", "Semi-furnished"),
    Unfurnished("unfurnished", "Unfurnished");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key }
    }
}

data class RoomDetails(
    val type: RoomType? = null,
    val furnishing: Furnishing? = null,
    val locality: String = "",
    val rent: Int = 0,
    val photoUrls: List<String> = emptyList(),
)

/**
 * Public profile, stored at `users/{uid}`. Readable by any signed-in user (discovery), so it
 * deliberately holds no email or phone — those stay in Firebase Auth.
 */
data class UserProfile(
    val uid: String,
    val name: String = "",
    val age: Int = 0,
    val photoUrl: String? = null,
    val occupationType: OccupationType? = null,
    val occupation: String = "",
    val city: String = "",
    val preferredLocalities: List<String> = emptyList(),
    val budgetMin: Int = 0,
    val budgetMax: Int = 0,
    /** "asap", "flexible" or a month as "yyyy-MM". */
    val moveIn: String = "",
    val userType: UserType? = null,
    val bio: String = "",
    val interests: List<String> = emptyList(),
    val diet: Diet? = null,
    val drinking: Drinking? = null,
    val room: RoomDetails? = null,
    val profileComplete: Boolean = false,
    val quizComplete: Boolean = false,
    /** False when the person has paused their profile — they're hidden from Discover. */
    val discoverable: Boolean = true,
)

/** Where a signed-in user is in setup. Drives the post-auth routing. */
enum class SetupStatus { NeedsProfile, NeedsQuiz, Complete }

val UserProfile?.setupStatus: SetupStatus
    get() = when {
        this == null || !profileComplete -> SetupStatus.NeedsProfile
        !quizComplete -> SetupStatus.NeedsQuiz
        else -> SetupStatus.Complete
    }
