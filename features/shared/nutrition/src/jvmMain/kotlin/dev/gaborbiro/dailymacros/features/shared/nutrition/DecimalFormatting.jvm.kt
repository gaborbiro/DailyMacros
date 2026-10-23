package dev.gaborbiro.dailymacros.features.shared.nutrition

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual fun platformFormatDecimal(
    value: Double,
    decimalCount: Int,
    dropTrailingZeroes: Boolean,
    localeTag: String?,
): String {
    val pattern = buildString {
        append("0")
        if (decimalCount > 0) {
            append('.')
            val digit = if (dropTrailingZeroes) '#' else '0'
            repeat(decimalCount) { append(digit) }
        }
    }
    val locale = localeTag?.let(Locale::forLanguageTag) ?: Locale.getDefault(Locale.Category.FORMAT)
    return DecimalFormat(pattern, DecimalFormatSymbols.getInstance(locale))
        .apply { roundingMode = RoundingMode.HALF_EVEN }
        .format(value)
}
