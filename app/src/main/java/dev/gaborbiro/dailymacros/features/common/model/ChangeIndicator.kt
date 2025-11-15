package dev.gaborbiro.dailymacros.features.common.model

/**
 * Represents a change indicator with direction and value.
 */
internal data class ChangeIndicator(
    val direction: ChangeDirection,
    val value: String, // e.g., "+5%", "-3%", "0%"
)

/**
 * Direction of change for the indicator.
 */
internal enum class ChangeDirection {
    UP,    // Rising (green/positive)
    DOWN,  // Falling (red/negative)
    NEUTRAL // No change or minimal change
}