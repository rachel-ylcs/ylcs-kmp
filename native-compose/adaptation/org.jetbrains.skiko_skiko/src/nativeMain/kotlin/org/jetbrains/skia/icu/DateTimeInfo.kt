package org.jetbrains.skia.icu

/** Gregorian calendar conventions for a BCP 47 locale; weekdays are ordered Monday through Sunday. */
data class DateTimeInfo(
    val firstDayOfWeek: Int,
    val shortDatePattern: String,
    val is24HourFormat: Boolean,
    val weekdayNames: List<Pair<String, String>>,
)
