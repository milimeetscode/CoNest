package com.conest.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.conest.app.R

@OptIn(ExperimentalTextApi::class)
private fun newsreader(weight: Int, italic: Boolean, opsz: Float) = Font(
    resId = if (italic) R.font.newsreader_italic else R.font.newsreader,
    weight = FontWeight(weight),
    style = if (italic) FontStyle.Italic else FontStyle.Normal,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", opsz),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun hanken(weight: Int) = Font(
    resId = R.font.hanken_grotesk,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Newsreader cut at its 72pt optical size — tighter, higher-contrast, for display headlines. */
val NewsreaderDisplay = FontFamily(
    newsreader(300, italic = false, opsz = 72f),
    newsreader(400, italic = false, opsz = 72f),
    newsreader(500, italic = false, opsz = 72f),
    newsreader(300, italic = true, opsz = 72f),
    newsreader(400, italic = true, opsz = 72f),
)

/** Newsreader at text optical size — for captions, pull quotes, small serif moments. */
val NewsreaderText = FontFamily(
    newsreader(400, italic = false, opsz = 12f),
    newsreader(500, italic = false, opsz = 12f),
    newsreader(400, italic = true, opsz = 12f),
)

val HankenGrotesk = FontFamily(
    hanken(400),
    hanken(500),
    hanken(600),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = NewsreaderDisplay,
        fontWeight = FontWeight(300),
        fontSize = 54.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.02).em,
    ),
    displayMedium = TextStyle(
        fontFamily = NewsreaderDisplay,
        fontWeight = FontWeight(300),
        fontSize = 42.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.015).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = NewsreaderDisplay,
        fontWeight = FontWeight(400),
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.01).em,
    ),
    titleLarge = TextStyle(
        fontFamily = NewsreaderDisplay,
        fontWeight = FontWeight(500),
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight(400),
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.005.em,
    ),
    bodyMedium = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight(400),
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight(500),
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.em,
    ),
    labelSmall = TextStyle(
        fontFamily = HankenGrotesk,
        fontWeight = FontWeight(500),
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.16.em,
    ),
)
