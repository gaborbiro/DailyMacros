package dev.gaborbiro.dailymacros.repositories.records.domain.model

import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors

data class Template(
    val dbId: Long,
    val images: List<String>,
    /** Same order as [images]; null = never classified (legacy row or no analysis). */
    val isRepresentativeOfMealByImageIndex: List<Boolean?>,
    val name: String,
    val description: String,
    /** When this template was created by forking from another template; null for roots or legacy rows. */
    val parentTemplateId: Long? = null,
    /** Epoch ms when the template row was created (0 = legacy / epoch start default). */
    val createdAtEpochMs: Long = 0L,
    /** Epoch ms when the template row was last updated. */
    val updatedAtEpochMs: Long = 0L,
    val isPending: Boolean,
    val nutrients: Nutrients,
    val notes: String,
    /** Parsed from persisted AI analysis; empty if none or legacy data. */
    val mealComponents: List<MealComponent>,
    val topContributors: TopContributors,
    val quickPickOverride: QuickPickOverride?,
) {
    enum class QuickPickOverride {
        INCLUDE, EXCLUDE
    }
}

