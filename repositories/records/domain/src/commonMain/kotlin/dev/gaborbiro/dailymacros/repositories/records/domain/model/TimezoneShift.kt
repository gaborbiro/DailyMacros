package dev.gaborbiro.dailymacros.repositories.records.domain.model

import dev.gaborbiro.dailymacros.repositories.common.model.ZonedInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * How far the wall clock moved between two consecutive records, when they were logged in different
 * time zones (e.g. crossing time zones mid-flight). Null when both share a zone, or when the zones
 * differ but happen to have the same offset at those moments.
 */
fun timezoneShift(previous: ZonedInstant, current: ZonedInstant): Duration? {
    if (previous.timeZone == current.timeZone) return null
    val diffSeconds = current.offset.totalSeconds - previous.offset.totalSeconds
    return if (diffSeconds == 0) null else diffSeconds.seconds
}
