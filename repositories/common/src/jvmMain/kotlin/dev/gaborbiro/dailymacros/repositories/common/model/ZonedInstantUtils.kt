package dev.gaborbiro.dailymacros.repositories.common.model

import kotlinx.datetime.toJavaZoneId
import kotlinx.datetime.toKotlinTimeZone
import java.time.ZonedDateTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

fun ZonedDateTime.toZonedInstant(): ZonedInstant {
    return ZonedInstant(
        instant = toInstant().toKotlinInstant(),
        timeZone = zone.toKotlinTimeZone(),
    )
}

fun ZonedInstant.toZonedDateTime(): ZonedDateTime {
    return ZonedDateTime.ofInstant(
        instant.toJavaInstant(),
        timeZone.toJavaZoneId()
    )
}