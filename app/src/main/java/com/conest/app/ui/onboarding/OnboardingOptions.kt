package com.conest.app.ui.onboarding

import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** Launch cities (PRD §1.3) and a starting set of neighbourhoods people actually ask for. */
val CityLocalities: Map<String, List<String>> = linkedMapOf(
    "Hyderabad" to listOf("Gachibowli", "Madhapur", "Kondapur", "HITEC City", "Banjara Hills", "Jubilee Hills", "Kukatpally", "Manikonda", "Begumpet", "Secunderabad"),
    "Bengaluru" to listOf("Koramangala", "Indiranagar", "HSR Layout", "Whitefield", "Bellandur", "Electronic City", "Jayanagar", "BTM Layout", "Marathahalli", "Hebbal"),
    "Pune" to listOf("Kothrud", "Baner", "Hinjewadi", "Viman Nagar", "Koregaon Park", "Wakad", "Kharadi", "Aundh", "Hadapsar", "Magarpatta"),
    "Kolkata" to listOf("Salt Lake", "New Town", "Ballygunge", "Park Street", "Gariahat", "Behala", "Tollygunge", "Jadavpur", "Rajarhat", "Dum Dum"),
)

val Interests = listOf(
    "Cooking", "Reading", "Fitness", "Music", "Films", "Gaming", "Travel", "Art & design",
    "Running", "Yoga", "Cricket", "Football", "Board games", "Photography", "Plants", "Coffee",
    "Theatre", "Hiking", "Podcasts", "Volunteering",
)

data class MoveInOption(val key: String, val label: String)

/** "As soon as possible", the next three months by name, then "Flexible". */
fun moveInOptions(now: YearMonth = YearMonth.now()): List<MoveInOption> = buildList {
    add(MoveInOption("asap", "As soon as possible"))
    for (i in 1..3) {
        val m = now.plusMonths(i.toLong())
        val name = m.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        add(MoveInOption(m.toString(), if (m.year == now.year) name else "$name ${m.year}"))
    }
    add(MoveInOption("flexible", "I'm flexible"))
}

/** Human label for a stored move-in key — used on the profile. */
fun moveInLabel(key: String): String = when (key) {
    "asap" -> "As soon as possible"
    "flexible" -> "Flexible"
    "" -> "—"
    else -> runCatching {
        val m = YearMonth.parse(key)
        "${m.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${m.year}"
    }.getOrDefault(key)
}

/** ₹12,000 → "₹12k"; keeps slider labels short. */
fun rupeesShort(amount: Int): String = if (amount >= 1000 && amount % 1000 == 0) "₹${amount / 1000}k" else "₹%,d".format(amount)

const val BUDGET_MIN = 3_000
const val BUDGET_MAX = 60_000
const val BUDGET_STEP = 1_000
