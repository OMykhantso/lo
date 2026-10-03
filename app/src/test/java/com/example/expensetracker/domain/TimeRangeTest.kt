package com.example.expensetracker.domain

import com.example.expensetracker.domain.time.TimeRange
import com.example.expensetracker.testutil.TestData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeRangeTest {
    private fun millis(date: String, zone: ZoneId) = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `month of a mid month instant spans the whole calendar month in the given zone`() {
        val range = TimeRange.currentMonth(TestData.clock)
        assertEquals(millis("2025-03-01", TestData.KYIV), range.startInclusive)
        assertEquals(millis("2025-04-01", TestData.KYIV), range.endExclusive)
    }

    @Test
    fun `boundaries are start inclusive and end exclusive`() {
        val range = TimeRange.currentMonth(TestData.clock)
        assertTrue(range.startInclusive in range)
        assertFalse(range.startInclusive - 1 in range)
        assertTrue(range.endExclusive - 1 in range)
        assertFalse(range.endExclusive in range)
    }

    @Test
    fun `the zone decides which month a late evening instant belongs to`() {
        // 2025-03-31 22:30 UTC — це вже 1 квітня 01:30 у Києві (літній час, UTC+3).
        val instant = Instant.parse("2025-03-31T22:30:00Z")
        val utc = TimeRange.monthOf(instant, ZoneId.of("UTC"))
        val kyiv = TimeRange.monthOf(instant, TestData.KYIV)
        assertEquals(millis("2025-03-01", ZoneId.of("UTC")), utc.startInclusive)
        assertEquals(millis("2025-04-01", TestData.KYIV), kyiv.startInclusive)
    }

    @Test
    fun `month length follows the calendar including a leap february`() {
        val utc = ZoneId.of("UTC")
        val leap = TimeRange.monthOf(Instant.parse("2024-02-10T00:00:00Z"), utc)
        val regular = TimeRange.monthOf(Instant.parse("2025-02-10T00:00:00Z"), utc)
        val day = 24L * 60 * 60 * 1000
        assertEquals(29 * day, leap.endExclusive - leap.startInclusive)
        assertEquals(28 * day, regular.endExclusive - regular.startInclusive)
    }

    @Test
    fun `a month with a daylight saving switch is one hour shorter or longer than 30 days`() {
        // 30 березня 2025 у Києві переходять на літній час (+1 год): березень має 31 доба - 1 година.
        val range = TimeRange.currentMonth(TestData.clock)
        val hours = (range.endExclusive - range.startInclusive) / (60 * 60 * 1000)
        assertEquals(31L * 24 - 1, hours)
    }
}
