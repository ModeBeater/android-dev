package kz.kbtu.releaseflow.domain.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** "12 Oct" */
fun Long.dayText(): String = dayFormatter.format(toLocalDate())

/** "12 Oct, 14:05" */
fun Long.dateTimeText(): String = dateTimeFormatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** "2026-09" -> "Sep 2026" */
fun monthLabel(key: String): String {
    val (year, month) = key.split("-").map { it.toInt() }
    return monthFormatter.format(LocalDate.of(year, month, 1))
}

/** "today", "in 3 d", "5 d ago" — relative to the current day. */
fun Long.relativeDays(now: Long = System.currentTimeMillis()): String {
    val days = ChronoUnit.DAYS.between(now.toLocalDate(), toLocalDate())
    return when {
        days == 0L -> "today"
        days == 1L -> "tomorrow"
        days == -1L -> "yesterday"
        days > 0 -> "in $days d"
        else -> "${-days} d ago"
    }
}

/** 90_000_000 -> "1 d 1 h"; 5_400_000 -> "1 h 30 min"; 120_000 -> "2 min" */
fun Long.durationText(): String {
    val totalMinutes = this / 60_000
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> if (hours > 0) "$days d $hours h" else "$days d"
        hours > 0 -> if (minutes > 0) "$hours h $minutes min" else "$hours h"
        else -> "$minutes min"
    }
}

/** How long a track has been in its current stage. */
fun Long.ageText(now: Long = System.currentTimeMillis()): String = (now - this).coerceAtLeast(0).durationText()

/** 0.75 -> "75 %" */
fun Double.percentText(): String = "${Math.round(this * 100)} %"
