package dev.gaborbiro.dailymacros.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color


val LocalExtraColorScheme = staticCompositionLocalOf<ExtraColorScheme> {
    error("ExtraColorScheme not provided")
}

val darkExtraColorScheme = ExtraColorScheme(
    calorieColor = Color(0xFFCDE9FF),
    proteinColor = Color(0xFFCFF9E8),
    fatColor = Color(0xFFFFF3B8),
    carbsColor = Color(0xFFFFE0C7),
    saltColor = Color(0xFFE8D9FF),
    fibreColor = Color(0xFFFFD8E2),
)

val lightExtraColorScheme = ExtraColorScheme(
    calorieColor = Color(0xFF48B7FF),
    proteinColor = Color(0xFF71FFC5),
    fatColor = Color(0xFFFFEC73),
    carbsColor = Color(0xFFFFB178),
    saltColor = Color(0xFFBA91FF),
    fibreColor = Color(0xFFFF6F92),
)

data class ExtraColorScheme(
    val calorieColor: Color,
    val proteinColor: Color,
    val fatColor: Color,
    val carbsColor: Color,
    val saltColor: Color,
    val fibreColor: Color,
)