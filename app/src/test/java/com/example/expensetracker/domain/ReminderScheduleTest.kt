package com.example.expensetracker.domain

import com.example.expensetracker.domain.notifications.NextReminderTime
import com.example.expensetracker.domain.notifications.ReminderBootPolicy
import com.example.expensetracker.domain.notifications.RescheduleMode
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NextReminderTimeTest {
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private fun at(local: String, zone: ZoneId = kyiv): ZonedDateTime = LocalDateTime.parse(local).atZone(zone)

    private fun next(local: String, zone: ZoneId = kyiv) = NextReminderTime.after(at(local, zone), NextReminderTime.REMINDER_TIME)

    @Test
    fun `before 20 00 the reminder is today`() {
        assertEquals(at("2025-03-15T20:00:00"), next("2025-03-15T12:00:00"))
        assertEquals(Duration.ofHours(8), NextReminderTime.delayFrom(at("2025-03-15T12:00:00")))
    }

    @Test
    fun `after 20 00 the reminder is tomorrow`() {
        assertEquals(at("2025-03-16T20:00:00"), next("2025-03-15T21:30:00"))
        assertEquals(Duration.ofMinutes(22 * 60 + 30), NextReminderTime.delayFrom(at("2025-03-15T21:30:00")))
    }

    @Test
    fun `exactly at 20 00 the next one is tomorrow, not a duplicate now`() {
        assertEquals(at("2025-03-16T20:00:00"), next("2025-03-15T20:00:00"))
        assertEquals(Duration.ofHours(24), NextReminderTime.delayFrom(at("2025-03-15T20:00:00")))
    }

    @Test
    fun `one millisecond before 20 00`() {
        val now = at("2025-03-15T19:59:59.999")
        assertEquals(Duration.ofMillis(1), NextReminderTime.delayFrom(now))
    }

    @Test
    fun `just after 20 00 waits almost a full day`() {
        val now = at("2025-03-15T20:00:00.001")
        assertEquals(Duration.ofHours(24).minusMillis(1), NextReminderTime.delayFrom(now))
    }

    @Test
    fun `midnight`() {
        assertEquals(Duration.ofHours(20), NextReminderTime.delayFrom(at("2025-03-15T00:00:00")))
        assertEquals(at("2025-03-15T20:00:00"), next("2025-03-15T00:00:00"))
    }

    @Test
    fun `spring forward - the day is 23 hours long, so the real delay is shorter than the wall clock one`() {
        // У Києві 30.03.2025 о 03:00 годинник переходить на 04:00.
        val now = at("2025-03-29T21:00:00")
        assertEquals(at("2025-03-30T20:00:00"), next("2025-03-29T21:00:00"))
        assertEquals(Duration.ofHours(22), NextReminderTime.delayFrom(now))

        assertEquals(Duration.ofHours(18), NextReminderTime.delayFrom(at("2025-03-30T01:00:00")))
    }

    @Test
    fun `fall back - the day is 25 hours long, so the real delay is longer`() {
        // У Києві 26.10.2025 о 04:00 годинник переходить на 03:00.
        val now = at("2025-10-25T21:00:00")
        assertEquals(at("2025-10-26T20:00:00"), next("2025-10-25T21:00:00"))
        assertEquals(Duration.ofHours(24), NextReminderTime.delayFrom(now))
    }

    @Test
    fun `month and year boundaries`() {
        assertEquals(at("2025-02-01T20:00:00"), next("2025-01-31T21:00:00"))
        assertEquals(at("2026-01-01T20:00:00"), next("2025-12-31T21:00:00"))
    }

    @Test
    fun `leap day is not skipped`() {
        assertEquals(at("2024-02-29T20:00:00"), next("2024-02-28T21:00:00"))
        assertEquals(at("2024-03-01T20:00:00"), next("2024-02-29T21:00:00"))
        assertEquals(at("2025-03-01T20:00:00"), next("2025-02-28T21:00:00"))
    }

    @Test
    fun `result stays in the user's zone`() {
        val newYork = ZoneId.of("America/New_York")
        val result = next("2025-03-15T12:00:00", newYork)
        assertEquals(newYork, result.zone)
        assertEquals(LocalTime.of(20, 0), result.toLocalTime())
    }

    @Test
    fun `a calendar day that does not exist in the zone is skipped`() {
        // Самоа пропустило 30.12.2011 цілком: після 29-го одразу 31-ше.
        val apia = ZoneId.of("Pacific/Apia")
        val result = next("2011-12-29T21:00:00", apia)
        assertEquals(LocalDateTime.parse("2011-12-31T20:00:00"), result.toLocalDateTime())
    }

    @Test
    fun `custom time of day`() {
        val result = NextReminderTime.after(at("2025-03-15T12:00:00"), LocalTime.of(9, 30))
        assertEquals(at("2025-03-16T09:30:00"), result)
    }

    @Test
    fun `the delay is never negative or longer than 25 hours`() {
        var now = at("2025-01-01T00:00:00")
        repeat(24 * 400) {
            val delay = NextReminderTime.delayFrom(now)
            assertTrue("delay at $now = $delay", !delay.isNegative && !delay.isZero)
            assertTrue("delay at $now = $delay", delay <= Duration.ofHours(25))
            now = now.plusHours(1)
        }
    }
}

class ReminderBootPolicyTest {
    @Test
    fun `reboot and app update only make sure the schedule exists`() {
        assertEquals(RescheduleMode.ENSURE_SCHEDULED, ReminderBootPolicy.modeFor("android.intent.action.BOOT_COMPLETED"))
        assertEquals(RescheduleMode.ENSURE_SCHEDULED, ReminderBootPolicy.modeFor("android.intent.action.MY_PACKAGE_REPLACED"))
    }

    @Test
    fun `clock or time zone changes realign the schedule`() {
        assertEquals(RescheduleMode.REALIGN, ReminderBootPolicy.modeFor("android.intent.action.TIME_SET"))
        assertEquals(RescheduleMode.REALIGN, ReminderBootPolicy.modeFor("android.intent.action.TIMEZONE_CHANGED"))
    }

    @Test
    fun `anything else is ignored, including a null action`() {
        assertEquals(RescheduleMode.IGNORE, ReminderBootPolicy.modeFor(null))
        assertEquals(RescheduleMode.IGNORE, ReminderBootPolicy.modeFor(""))
        assertEquals(RescheduleMode.IGNORE, ReminderBootPolicy.modeFor("android.intent.action.SCREEN_ON"))
        assertEquals(RescheduleMode.IGNORE, ReminderBootPolicy.modeFor("com.evil.app.BOOT_COMPLETED"))
    }
}
