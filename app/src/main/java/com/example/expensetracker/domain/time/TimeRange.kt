package com.example.expensetracker.domain.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Напіввідкритий проміжок часу `[startInclusive, endExclusive)` в epoch-мілісекундах. */
data class TimeRange(val startInclusive: Long, val endExclusive: Long) {
    init {
        require(startInclusive <= endExclusive) { "start must not be after end" }
    }

    operator fun contains(timestamp: Long): Boolean = timestamp >= startInclusive && timestamp < endExclusive

    companion object {
        /** Календарний місяць, у який потрапляє [instant] у часовій зоні [zone]. */
        fun monthOf(instant: Instant, zone: ZoneId): TimeRange {
            val start = ZonedDateTime.ofInstant(instant, zone)
                .toLocalDate().withDayOfMonth(1).atStartOfDay(zone)
            return TimeRange(start.toInstant().toEpochMilli(), start.plusMonths(1).toInstant().toEpochMilli())
        }

        fun currentMonth(clock: Clock): TimeRange = monthOf(clock.instant(), clock.zone)
    }
}
