package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toKotlinTimeZone
import java.time.ZonedDateTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

/**
 * java.time bridges for the Android modules, which still do their date maths with java.time.
 * Both directions are lossless (nanosecond instant, original zone id).
 */
fun ZonedInstant.toZonedDateTime(): ZonedDateTime =
    instant.toJavaInstant().atZone(timeZone.toJavaZoneId())

fun ZonedDateTime.toZonedInstant(): ZonedInstant =
    ZonedInstant(toInstant().toKotlinInstant(), zone.toKotlinTimeZone())
