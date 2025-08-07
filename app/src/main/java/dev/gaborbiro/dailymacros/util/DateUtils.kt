package dev.gaborbiro.dailymacros.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDateTime.formatShort() = format(DateTimeFormatter.ofPattern("E dd MMM, H:mm"))

fun LocalDateTime.formatShortTime() = format(DateTimeFormatter.ofPattern("H:mm"))
