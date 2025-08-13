package dev.gaborbiro.dailymacros.features.common

import dev.gaborbiro.dailymacros.data.records.domain.model.Nutrients

internal class NutrientsUIMapper {

    fun map(nutrients: Nutrients?, short: Boolean = false): String? {
        return listOfNotNull(
            nutrients?.calories?.let { mapCalories(it, short) },
            nutrients?.protein?.let { mapProtein(it, short) },
            nutrients?.carbohydrates?.let { mapCarbohydrates(it, nutrients.ofWhichSugar, short) },
            if (!short) {
                nutrients?.ofWhichSugar?.let { mapSugar(it) }
            } else null,
            nutrients?.fat?.let { mapFat(it, nutrients.ofWhichSaturated, short) },
            if (!short) {
                nutrients?.ofWhichSaturated?.let { mapSaturated(it) }
            } else null,
            nutrients?.salt?.let { mapSalt(it, short) },
        )
            .joinToString()
            .takeIf { it.isNotBlank() }
    }

    fun mapCalories(calories: Number?, short: Boolean = false) =
        calories
            ?.takeIf { it.toInt() > 0f }
            ?.let { if (short) "${calories.toInt()}cal" else "Calories: ${calories.toInt()}cal" }
            ?: run { if (short) null else "Calories: 0cal" }

    fun mapProtein(protein: Number?, short: Boolean = false) =
        protein
            ?.takeIf { it.toInt() > 0f }
            ?.let { if (short) "prot ${protein.toInt()}" else "Protein: ${protein}g" }
            ?: run { if (short) null else "Protein: 0g" }

    fun mapCarbohydrates(carbohydrates: Number?, sugar: Number?, short: Boolean = false) =
        carbohydrates
            ?.takeIf { it.toInt() > 0f }
            ?.let {
                val sugar = mapSugar(sugar, short)
                val shortSugar = sugar?.let { "($it)" } ?: ""
                if (short) "carb ${carbohydrates.toInt()}$shortSugar" else "Carbs: ${carbohydrates}g"
            }
            ?: run { if (short) null else "Carbs: 0g" }

    fun mapSugar(sugar: Number?, short: Boolean = false) =
        sugar
            ?.takeIf { it.toInt() > 0f }
            ?.let { if (short) "${sugar.toInt()}" else "of which sugar: ${sugar}g" }
            ?: run { if (short) null else "of which sugar: 0g" }

    fun mapFat(fat: Number?, saturated: Number?, short: Boolean = false) =
        fat
            ?.takeIf { it.toInt() > 0f }
            ?.let {
                val saturated = mapSaturated(saturated, short)
                val shortSaturated = saturated?.let { "($it)" } ?: ""
                if (short) "fat ${fat.toInt()}$shortSaturated" else "Fat: ${fat}g"
            }
            ?: run { if (short) null else "Fat: 0g" }

    fun mapSaturated(saturated: Number?, short: Boolean = false) =
        saturated
            ?.takeIf { it.toInt() > 0f }
            ?.let { if (short) saturated.toInt().toString() else "of which saturated: ${saturated}g" }
            ?: run { if (short) null else "of which saturated: 0g" }

    fun mapSalt(salt: Number?, short: Boolean = false) =
        salt
            ?.takeIf { it.toInt() > 0f }
            ?.let { if (short) "sal ${salt.toInt()}" else "Salt: ${salt}g" }
            ?: run { if (short) null else "Salt: 0g" }
}
