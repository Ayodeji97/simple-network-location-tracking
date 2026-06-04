package com.danzucker.networklocationtracker.core.data.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration

private val zoneId = TimeZone.UTC.toJavaZoneId()

fun Instant.instantToMillis(): Long {
    return this.toEpochMilliseconds()
}

fun Long.millisToInstant(): Instant {
    return Instant.fromEpochMilliseconds(this)
}

fun Instant.toReadableDateTime(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy hh:mm a")
        .withZone(zoneId)
        .withLocale(Locale.ENGLISH)
    return this.toJavaInstant().atZone(zoneId)
        .format(formatter)
}

fun Instant.toReadableDate(): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy")
        .withZone(zoneId)
        .withLocale(Locale.ENGLISH)
    return this.toJavaInstant().atZone(zoneId)
        .format(formatter)
}

fun Instant.toReadableTime(): String {
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        .withZone(zoneId)
        .withLocale(Locale.ENGLISH)
    return this.toJavaInstant().atZone(zoneId)
        .format(formatter)
}


fun durationBetween(start: Instant, end: Instant): Duration = end - start

fun Duration.toReadableDuration(): String {
    val days = inWholeDays
    val hours = inWholeHours % 24
    val minutes = inWholeMinutes % 60
    val seconds = inWholeSeconds % 60

    return buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0) append("${hours}h ")
        if (minutes > 0) append("${minutes}m ")
        if (seconds > 0 || isEmpty()) append("${seconds}s") // Always show at least seconds
    }.trim()
}