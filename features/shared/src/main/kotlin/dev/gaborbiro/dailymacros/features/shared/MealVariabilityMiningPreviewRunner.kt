package dev.gaborbiro.dailymacros.features.shared

/**
 * Snapshot of a variability mining preview, decoupled from where the mining orchestration lives.
 */
data class VariabilityMiningPreviewSnapshot(
    val requestJsonPretty: String,
    val responseJsonPretty: String,
    val skippedNoNewObservations: Boolean,
)

/**
 * Runs meal variability mining preview work. Implemented in the app module so the worker does not
 * depend on a concrete use-case type or module placement.
 */
fun interface MealVariabilityMiningPreviewRunner {
    suspend fun runPreview(): VariabilityMiningPreviewSnapshot
}
