package dev.gaborbiro.dailymacros.features.common.views

enum class NutrientDisplayLine(val unit: String, val decimalCount: Int, val dropTrailingZeroes: Boolean) {
    Calories(unit = "kcal", decimalCount = 0, dropTrailingZeroes = true),
    Protein(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
    Carb(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
    OfWhichSugar(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
    OfWhichAddedSugar(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
    Fat(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
    OfWhichSaturated(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
    Salt(unit = "g", decimalCount = 1, dropTrailingZeroes = false),
    Fibre(unit = "g", decimalCount = 0, dropTrailingZeroes = true),
}