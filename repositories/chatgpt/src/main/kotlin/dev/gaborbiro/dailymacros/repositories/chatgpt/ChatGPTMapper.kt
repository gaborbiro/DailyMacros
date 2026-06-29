package dev.gaborbiro.dailymacros.repositories.chatgpt

import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.MealComponent
import dev.gaborbiro.dailymacros.repositories.chatgpt.domain.model.NutrientAnalysisResult
import dev.gaborbiro.dailymacros.repositories.chatgpt.prompts.NutrientAnalysisResponse
import dev.gaborbiro.dailymacros.repositories.chatgpt.service.model.ChatGPTApiError
import dev.gaborbiro.dailymacros.repositories.common.model.DomainError
import dev.gaborbiro.dailymacros.repositories.common.model.Nutrients
import dev.gaborbiro.dailymacros.repositories.common.model.TopContributors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChatGPTMapper @Inject constructor() {

    fun map(parsed: NutrientAnalysisResponse, imageCount: Int): NutrientAnalysisResult {
        val nutrients = parsed.nutrients?.let {
            Nutrients(
                calories = it.calories?.toInt(),
                protein = it.protein?.grams?.toFloat(),
                fat = it.fat?.grams?.toFloat(),
                ofWhichSaturated = it.ofWhichSaturated?.grams?.toFloat(),
                carbs = it.carbs?.grams?.toFloat(),
                ofWhichSugar = it.ofWhichSugar?.grams?.toFloat(),
                ofWhichAddedSugar = it.ofWhichAddedSugar?.grams?.toFloat(),
                salt = it.salt?.grams?.toFloat(),
                fibre = it.fibre?.grams?.toFloat(),
            )
        }
        val topContributors = parsed.nutrients?.let {
            TopContributors(
                topProteinContributors = it.protein?.topContributorIngredients,
                topFatContributors = it.fat?.topContributorIngredients,
                topSaturatedFatContributors = it.ofWhichSaturated?.topContributorIngredients,
                topCarbsContributors = it.carbs?.topContributorIngredients,
                topSugarContributors = it.ofWhichSugar?.topContributorIngredients,
                topAddedSugarContributors = it.ofWhichAddedSugar?.topContributorIngredients,
                topSaltContributors = it.salt?.topContributorIngredients,
                topFibreContributors = it.fibre?.topContributorIngredients,
            )
        }
        val components = parsed.components.orEmpty().mapNotNull { component ->
            val name = component.name?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            MealComponent(
                name = name,
                estimatedAmount = component.estimatedAmount?.trim().orEmpty(),
                confidence = component.confidence?.trim().orEmpty().ifEmpty { "unknown" },
            )
        }
        val representativeFlags = normalizeRepresentativeOfMealFlags(imageCount, parsed.representativeOfMeal)

        return NutrientAnalysisResult(
            nutrients = nutrients,
            topContributors = topContributors,
            title = parsed.title,
            notes = parsed.notes.takeIf { it.isNullOrBlank().not() },
            components = components,
            isRepresentativeOfMealByImageIndex = representativeFlags,
            error = parsed.error,
        )
    }

    fun map(error: ChatGPTApiError): DomainError {
        return when (error) {
            is ChatGPTApiError.InternetError -> DomainError.DisplayMessageToUser.CheckInternetConnection(error)
            is ChatGPTApiError.ServerErrorResponse -> DomainError.DisplayMessageToUser.TechnicalMessage(errorMessage = error.errorMessage, error) // for power users
            is ChatGPTApiError.GenericError, is ChatGPTApiError.MappingError -> DomainError.DisplayMessageToUser.OperationFailed(analyticsMessage = error.analyticsMessage, error)
            // we do not at the moment have any critical ForcedErrorMessage (which would be shown even to non-power-users)
        }
    }

    private fun normalizeRepresentativeOfMealFlags(imageCount: Int, fromModel: List<Boolean>?): List<Boolean?> {
        if (imageCount <= 0) return emptyList()
        if (fromModel == null) return List(imageCount) { null }
        return List(imageCount) { index -> fromModel.getOrNull(index) }
    }
}
