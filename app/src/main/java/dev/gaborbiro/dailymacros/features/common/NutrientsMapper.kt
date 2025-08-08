package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.data.records.domain.model.Nutrients

fun map(nutrients: Nutrients?): String? {
    return listOfNotNull(
        nutrients?.calories?.let(::mapCalories),
        nutrients?.protein?.let(::mapProtein),
        nutrients?.carbohydrates?.let(::mapCarbohydrates),
        nutrients?.ofWhichSugar?.let(::mapSugar),
        nutrients?.fat?.let(::mapFat),
        nutrients?.ofWhichSaturated?.let(::mapSaturated),
        nutrients?.salt?.let(::mapSalt),
    )
        .joinToString()
        .takeIf { it.isNotBlank() }
}

fun mapCalories(calories: Number?) = calories?.let { "Calories: ${calories.toInt()} cal" }
fun mapProtein(protein: Number?) = protein?.let { "Protein: ${protein}g" }
fun mapCarbohydrates(carbohydrates: Number?) = carbohydrates?.let { "Carbs: ${carbohydrates}g" }
fun mapSugar(sugar: Number?) = sugar?.let { "of which sugar: ${sugar}g" }
fun mapFat(fat: Number?) = fat?.let { "Fat: ${fat}g" }
fun mapSaturated(saturated: Number?) = saturated?.let { "of which saturated: ${saturated}g" }
fun mapSalt(salt: Number?) = salt?.let { "Salt: ${salt}g" }
