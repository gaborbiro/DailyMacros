package dev.gaborbiro.dailymacros.features.shared.nutrition

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterRoundHalfEven
import platform.Foundation.canonicalLocaleIdentifierFromString
import platform.Foundation.currentLocale

internal actual fun platformFormatDecimal(
    value: Double,
    decimalCount: Int,
    dropTrailingZeroes: Boolean,
    localeTag: String?,
): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = false
        roundingMode = NSNumberFormatterRoundHalfEven
        minimumIntegerDigits = 1u
        maximumFractionDigits = decimalCount.toULong()
        minimumFractionDigits = if (dropTrailingZeroes) 0u else decimalCount.toULong()
        locale = localeTag
            ?.let { NSLocale(localeIdentifier = NSLocale.canonicalLocaleIdentifierFromString(it)) }
            ?: NSLocale.currentLocale
    }
    return checkNotNull(formatter.stringFromNumber(NSNumber(double = value))) {
        "NSNumberFormatter could not format $value"
    }
}
