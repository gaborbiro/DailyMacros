package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import kotlin.math.floor

fun layeredColors(
    progress0to1: Float,
    base: Color,
    onBackground: Color,
): Pair<Color, Color> {
    return when {
        progress0to1 < 1f -> {
            // First layer: nutrient color vs background
            base to onBackground.copy(alpha = .09f)
        }

        progress0to1 < 2f -> {
            // Second layer: red vs base
            Color(0xFFE53935) to base
        }

        progress0to1 < 3f -> {
            // Third layer: brighter red vs strong red
            Color(0xFFFF1744) to Color(0xFFE53935)
        }

        else -> {
            // Fourth layer: vivid red vs brighter red
            Color(0xFFFF5252) to Color(0xFFFF1744)
        }
    }
}

/**
 * "!" once a nutrient hits 2x its limit, "!!" at 3x, capped at "!!!" from 4x up. Simply being
 * over (1x-2x) is already conveyed by the existing red color, so no mark yet at that point.
 */
fun severityMarks(progress0to1: Float): String {
    val markCount = (floor(progress0to1).toInt() - 1).coerceIn(0, 3)
    return "!".repeat(markCount)
}

/**
 * Custom easing that mimics an overshoot interpolator (like Android Views).
 */
val OvershootInterpolatorEasing = Easing { fraction ->
    val tension = 2.0f
    val f = fraction - 1.0f
    f * f * ((tension + 1) * f + tension) + 1.0f
}