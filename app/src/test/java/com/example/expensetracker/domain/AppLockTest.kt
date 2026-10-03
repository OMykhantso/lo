package com.example.expensetracker.domain

import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.InMemorySecureKeyValueStore
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinHasher
import com.example.expensetracker.testutil.MutableClock
import java.time.Duration
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    private val clock = MutableClock()
    private val auth = PinAuthenticator(InMemorySecureKeyValueStore(), PinHasher(iterations = 1_000), clock)

    private fun lock() = AppLock(auth, clock, gracePeriodMillis = 30_000)

    @Test
    fun `unlocked on cold start when no pin is set`() {
        val lock = lock()
        assertFalse(lock.locked.value)
        assertFalse(lock.pinEnabled.value)
    }

    @Test
    fun `locked on cold start when a pin is set`() {
        auth.setPin("2580")
        val lock = lock()
        assertTrue(lock.locked.value)
        assertTrue(lock.pinEnabled.value)
        lock.unlock()
        assertFalse(lock.locked.value)
    }

    @Test
    fun `short trips to the background do not lock`() {
        auth.setPin("2580")
        val lock = lock().also { it.unlock() }
        lock.onBackgrounded()
        clock.advance(Duration.ofSeconds(29))
        lock.onForegrounded()
        assertFalse(lock.locked.value)
    }

    @Test
    fun `staying in the background past the grace period locks`() {
        auth.setPin("2580")
        val lock = lock().also { it.unlock() }
        lock.onBackgrounded()
        clock.advance(Duration.ofSeconds(30))
        lock.onForegrounded()
        assertTrue(lock.locked.value)
    }

    @Test
    fun `never locks without a pin`() {
        val lock = lock()
        lock.onBackgrounded()
        clock.advance(Duration.ofHours(5))
        lock.onForegrounded()
        assertFalse(lock.locked.value)
    }

    @Test
    fun `foreground without a prior background is a no-op`() {
        auth.setPin("2580")
        val lock = lock().also { it.unlock() }
        lock.onForegrounded()
        assertFalse(lock.locked.value)
    }

    @Test
    fun `changing the pin keeps the user signed in and updates the flag`() {
        val lock = lock()
        auth.setPin("2580")
        lock.onPinChanged()
        assertTrue(lock.pinEnabled.value)
        assertFalse(lock.locked.value)

        auth.clearPin()
        lock.onPinChanged()
        assertFalse(lock.pinEnabled.value)
    }
}
