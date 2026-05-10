package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.shared.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.DomainError

/**
 * User-visible strings for macro-analysis notifications (success summary lines and API errors).
 * Shared by background work and modal flows without depending on modal UI mapping.
 */
class MacrosNotificationTextMapper(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {

    fun mapDomainErrorToUserMessage(error: DomainError): String = when (error) {
        is DomainError.DisplayMessageToUser.CheckInternetConnection -> "Internet connectivity error"
        is DomainError.DisplayMessageToUser.ContactSupport ->
            "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
        is DomainError.DisplayMessageToUser.Message -> error.message
        is DomainError.DisplayMessageToUser.TryAgain ->
            "Oops. Something went wrong. Please try again later."
    }

    fun mapMacrosPrintout(nutrientBreakdown: NutrientBreakdown?): String? {
        return listOfNotNull(
            nutrientBreakdown?.calories?.let { nutrientsUiMapper.formatCalories(it, withLabel = true) },
            nutrientBreakdown?.protein?.let { nutrientsUiMapper.formatProtein(it, withLabel = true) },
            nutrientBreakdown?.fat?.let { nutrientsUiMapper.formatFat(it, nutrientBreakdown.ofWhichSaturated, withLabel = true) },
            nutrientBreakdown?.carbs?.let {
                nutrientsUiMapper.formatCarbs(it, nutrientBreakdown.ofWhichSugar, nutrientBreakdown.ofWhichAddedSugar, withLabel = true)
            },
            nutrientBreakdown?.salt?.let { nutrientsUiMapper.formatSalt(it, withLabel = true) },
            nutrientBreakdown?.fibre?.let { nutrientsUiMapper.formatFibre(it, withLabel = true) },
        )
            .joinToString()
            .takeIf { it.isNotBlank() }
    }
}
