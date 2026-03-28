package dev.gaborbiro.dailymacros.features.common

/**
 * Decimal precision for each nutrient line formatted in [NutrientsUiMapper].
 */
internal enum class NutrientDisplayLine(val decimalCount: Int) {
    Calories(0),
    Protein(0),
    Carb(0),
    OfWhichSugar(0),
    OfWhichAddedSugar(0),
    Fat(0),
    OfWhichSaturated(0),
    Salt(2),
    Fibre(0),
}
