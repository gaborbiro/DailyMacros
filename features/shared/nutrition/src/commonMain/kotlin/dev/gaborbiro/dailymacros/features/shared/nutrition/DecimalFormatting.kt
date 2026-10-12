package dev.gaborbiro.dailymacros.features.shared.nutrition

/**
 * Formats [value] with up to [decimalCount] fractional digits, using the locale's decimal
 * separator, no grouping separator and half-even rounding.
 *
 * @param dropTrailingZeroes when true, fractional zeroes are omitted ("2" instead of "2.0")
 * @param localeTag BCP 47 tag such as "de-DE"; null formats with the device's current locale
 */
fun formatDecimal(
    value: Double,
    decimalCount: Int,
    dropTrailingZeroes: Boolean,
    localeTag: String? = null,
): String {
    require(decimalCount >= 0) { "decimalCount must not be negative, was $decimalCount" }
    return platformFormatDecimal(value, decimalCount, dropTrailingZeroes, localeTag)
}

internal expect fun platformFormatDecimal(
    value: Double,
    decimalCount: Int,
    dropTrailingZeroes: Boolean,
    localeTag: String?,
): String
