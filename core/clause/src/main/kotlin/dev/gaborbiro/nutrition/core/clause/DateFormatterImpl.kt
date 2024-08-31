package dev.gaborbiro.nutrition.core.clause

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class DateFormatterImpl(
    private val localeProvider: LocaleProvider,
) : DateFormatter {

    override fun format(timestampMillis: Long, zoneId: ZoneId, pattern: String): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(zoneId)
            .format(
                DateTimeFormatter.ofPattern(
                    pattern,
                    localeProvider.getLocale()
                )
            )
    }
}