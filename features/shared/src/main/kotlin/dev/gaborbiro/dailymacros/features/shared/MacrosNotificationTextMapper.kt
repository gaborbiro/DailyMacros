package dev.gaborbiro.dailymacros.features.shared

import dev.gaborbiro.dailymacros.features.shared.model.NutrientBreakdown
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.ChatGPTDomainError

import javax.inject.Inject

/**
 * User-visible strings for macro-analysis notifications (success summary lines and API errors).
 * Shared by background work and modal flows without depending on modal UI mapping.
 */
class MacrosNotificationTextMapper @Inject constructor(
    private val nutrientsUiMapper: NutrientsUiMapper,
) {

    fun mapDomainErrorToUserMessage(error: ChatGPTDomainError): String = when (error) {
        is ChatGPTDomainError.DisplayMessageToUser.CheckInternetConnection -> "Internet connectivity error"
        is ChatGPTDomainError.DisplayMessageToUser.ContactSupport ->
            "Oops. Something went wrong. The issue has been logged and our engineers are looking into it."
        is ChatGPTDomainError.DisplayMessageToUser.Message -> error.message
        is ChatGPTDomainError.DisplayMessageToUser.TryAgain ->
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
