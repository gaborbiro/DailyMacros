package dev.gaborbiro.nutrition.core.clause

import java.time.ZoneId


interface DateFormatter {
    fun format(timestampMillis: Long, zoneId: ZoneId, pattern: String): String
}
