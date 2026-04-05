package com.planora.app.utils

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/** Date and time helpers using java.time. */
object DateUtils {

    private val zone get() = ZoneId.systemDefault()

    // Formatters (stateless, created once)
    private val fmtDate       = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val fmtTime       = DateTimeFormatter.ofPattern("hh:mm a")
    private val fmtShortDate  = DateTimeFormatter.ofPattern("dd MMM")
    private val fmtMonthYear  = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val fmtDayNumber  = DateTimeFormatter.ofPattern("dd")
    private val fmtDayOfWeek  = DateTimeFormatter.ofPattern("EEEE, MMMM d")

    // Private extension helpers
    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

    private fun Long.toZonedDateTime(): ZonedDateTime =
        Instant.ofEpochMilli(this).atZone(zone)

    // Public formatting
    fun formatDate(millis: Long): String      = millis.toLocalDate().format(fmtDate)
    fun formatTime(millis: Long): String      = millis.toZonedDateTime().format(fmtTime)
    fun formatShortDate(millis: Long): String = millis.toLocalDate().format(fmtShortDate)
    fun formatMonthYear(millis: Long): String = millis.toLocalDate().format(fmtMonthYear)
    fun formatDayNumber(millis: Long): String = millis.toLocalDate().format(fmtDayNumber)
    /** Returns e.g. "Sunday, March 8" â€” used in the hero greeting card. */
    fun formatDayOfWeek(millis: Long): String = millis.toLocalDate().format(fmtDayOfWeek)

    fun relativeTime(millis: Long): String {
        val diff = System.currentTimeMillis() - millis
        return when {
            diff < 60_000      -> "Just now"
            diff < 3_600_000   -> "${diff / 60_000}m ago"
            diff < 86_400_000  -> "${diff / 3_600_000}h ago"
            diff < 604_800_000 -> "${diff / 86_400_000}d ago"
            else               -> formatDate(millis)
        }
    }

    // Day / period boundaries
    fun getDayStart(millis: Long): Long =
        millis.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun getDayEnd(millis: Long): Long =
        millis.toLocalDate().atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()

    fun getWeekStart(millis: Long): Long =
        millis.toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun getMonthStart(millis: Long): Long =
        millis.toLocalDate().withDayOfMonth(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun getMonthEnd(millis: Long): Long {
        val ld = millis.toLocalDate()
        return ld.withDayOfMonth(ld.lengthOfMonth())
            .atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
    }

    // Calendar grid logic
    /**
     * Returns a list of epoch-millis (or null for leading blank cells) for every cell
     * in a Mon-first calendar grid for the given [year] and [month] (0-indexed, Calendar style).
     */
    fun getCalendarDaysForMonth(year: Int, month: Int): List<Long?> {
        val firstDay = LocalDate.of(year, month + 1, 1)   // month is 0-indexed â†’ +1
        val daysInMonth = firstDay.lengthOfMonth()
        // Monday = 1 in DayOfWeek, so offset from Monday = dayOfWeek.value - 1
        val offset = firstDay.dayOfWeek.value - 1
        val totalCells = 42   // 6 rows Ã— 7 columns â€” always stable grid
        return buildList(totalCells) {
            repeat(offset) { add(null) }
            for (day in 1..daysInMonth) {
                add(firstDay.withDayOfMonth(day).atStartOfDay(zone).toInstant().toEpochMilli())
            }
            // Pad trailing cells to fill the 6Ã—7 grid
            while (size < totalCells) add(null)
        }
    }

    fun isSameDay(millis1: Long, millis2: Long): Boolean =
        millis1.toLocalDate() == millis2.toLocalDate()

    fun isOverdue(millis: Long): Boolean = millis < System.currentTimeMillis()
}
