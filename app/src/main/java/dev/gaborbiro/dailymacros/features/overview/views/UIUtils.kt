package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color

fun layeredColors(
    progress0to1: Float,
    base: Color,
    onBackground: Color,
): Pair<Color, Color> {
    return when {
        progress0to1 < 1f -> {
            // First layer: macro color vs background
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
 * Custom easing that mimics an overshoot interpolator (like Android Views).
 */
val OvershootInterpolatorEasing = Easing { fraction ->
    val tension = 2.0f
    val f = fraction - 1.0f
    f * f * ((tension + 1) * f + tension) + 1.0f
}