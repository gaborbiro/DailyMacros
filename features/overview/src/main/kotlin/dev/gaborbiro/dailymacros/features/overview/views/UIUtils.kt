package dev.gaborbiro.dailymacros.features.overview.views

import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Color
import kotlin.math.floor

private val ALERT_RED = Color(0xFFE53935)

/**
 * Under target: the nutrient's own color. At or past it: a single fixed alert red, however far
 * over — magnitude is conveyed separately by [severityMarks], not by shading this any darker.
 */
fun progressColors(
    progress0to1: Float,
    base: Color,
    onBackground: Color,
): Pair<Color, Color> {
    val track = onBackground.copy(alpha = .09f)
    return if (progress0to1 < 1f) base to track else ALERT_RED to track
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