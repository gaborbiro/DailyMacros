package dev.gaborbiro.dailymacros.repo.records.domain.model

data class Template(
    val dbId: Long,
    val images: List<String>,
    val name: String,
    val description: String,
    val isPending: Boolean,
    val nutrients: NutrientBreakdown,
    val notes: String,
    val topContributors: TopContributors,
    val quickPickOverride: QuickPickOverride?,
) {
    enum class QuickPickOverride {
        INCLUDE, EXCLUDE
    }
}
