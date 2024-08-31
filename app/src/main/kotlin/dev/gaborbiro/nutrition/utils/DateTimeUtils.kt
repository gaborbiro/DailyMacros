package dev.gaborbiro.nutrition.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Converts the date-time to epoch millis
 */
fun LocalDateTime.toEpochMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * Converts the date-time to epoch millis
 */
fun ZonedDateTime.toEpochMillis(): Long = toInstant().toEpochMilli()

/**
 * Converts epoch millis to the corresponding date-time in the device's timezone
 */
fun Long.epochMillisToLocal(): ZonedDateTime = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())

/**
 * Converts epoch millis to the corresponding Zulu date-time
 */
fun Long.epochMillisToUTC(): ZonedDateTime = Instant.ofEpochMilli(this)
    .atZone(ZoneId.of("UTC"))
