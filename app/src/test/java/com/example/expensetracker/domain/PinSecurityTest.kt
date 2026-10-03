package com.example.expensetracker.domain

import com.example.expensetracker.domain.security.InMemorySecureKeyValueStore
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinError
import com.example.expensetracker.domain.security.PinHasher
import com.example.expensetracker.domain.security.PinPolicy
import com.example.expensetracker.domain.security.SetPinResult
import com.example.expensetracker.domain.security.VerifyResult
import com.example.expensetracker.testutil.MutableClock
import java.time.Duration
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {
    @Test
    fun `accepts non trivial four digit pins`() {
        listOf("2580", "1357", "9041", "7392").forEach { assertNull(it, PinPolicy.validate(it)) }
    }

    @Test
    fun `rejects wrong length`() {
        listOf("", "1", "123", "12345", "246802").forEach {
            assertEquals(it, PinError.WRONG_LENGTH, PinPolicy.validate(it))
        }
    }

    @Test
    fun `rejects non digits`() {
        listOf("12a4", "12 4", "١٢٣٥", "12.5").forEach {
            assertEquals(it, PinError.NOT_DIGITS, PinPolicy.validate(it))
        }
    }

    @Test
    fun `rejects repeated digits and straight sequences`() {
        listOf("0000", "1111", "9999", "1234", "2345", "6789", "4321", "9876", "3210").forEach {
            assertEquals(it, PinError.TRIVIAL, PinPolicy.validate(it))
        }
    }
}

class PinHasherTest {
    private val hasher = PinHasher(iterations = 1_000)

    @Test
    fun `same pin and salt give the same hash`() {
        val salt = hasher.newSalt()
        assertArrayEquals(hasher.hash("2580", salt), hasher.hash("2580", salt))
    }

    @Test
    fun `different salt or pin changes the hash`() {
        val salt = hasher.newSalt()
        assertFalse(hasher.hash("2580", salt).contentEquals(hasher.hash("2581", salt)))
        assertFalse(hasher.hash("2580", salt).contentEquals(hasher.hash("2580", hasher.newSalt())))
    }

    @Test
    fun `salts are random and 16 bytes`() {
        val a = hasher.newSalt()
        val b = hasher.newSalt()
        assertEquals(16, a.size)
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun `matches compares in constant time semantics`() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("2580", salt)
        assertTrue(hasher.matches("2580", salt, 1_000, hash))
        assertFalse(hasher.matches("0852", salt, 1_000, hash))
    }

    @Test
    fun `hash is 32 bytes`() {
        assertEquals(32, hasher.hash("2580", hasher.newSalt()).size)
    }
}

class PinAuthenticatorTest {
    private val store = InMemorySecureKeyValueStore()
    private val clock = MutableClock()
    private val auth = PinAuthenticator(store, PinHasher(iterations = 1_000), clock)

    private fun failTimes(n: Int, wrong: String = "0852"): VerifyResult {
        var last: VerifyResult = VerifyResult.Success
        repeat(n) { last = auth.verify(wrong) }
        return last
    }

    @Test
    fun `no pin set initially`() {
        assertFalse(auth.isPinSet())
        assertEquals(VerifyResult.Success, auth.verify("anything"))
    }

    @Test
    fun `set and verify`() {
        assertEquals(SetPinResult.Success, auth.setPin("2580"))
        assertTrue(auth.isPinSet())
        assertEquals(VerifyResult.Success, auth.verify("2580"))
        assertTrue(auth.verify("2581") is VerifyResult.WrongPin)
    }

    @Test
    fun `invalid pins are not stored`() {
        assertEquals(SetPinResult.Invalid(PinError.TRIVIAL), auth.setPin("1234"))
        assertEquals(SetPinResult.Invalid(PinError.WRONG_LENGTH), auth.setPin("12"))
        assertFalse(auth.isPinSet())
    }

    @Test
    fun `pin is never stored in clear text`() {
        auth.setPin("2580")
        val stored = store.snapshot()
        assertTrue(stored.isNotEmpty())
        stored.forEach { (key, value) ->
            assertFalse("$key contains the pin", value.contains("2580") || key.contains("2580"))
        }
    }

    @Test
    fun `setting the same pin twice produces different salts and hashes`() {
        auth.setPin("2580")
        val first = store.snapshot()
        auth.setPin("2580")
        val second = store.snapshot()
        assertNotEquals(first["pin_salt"], second["pin_salt"])
        assertNotEquals(first["pin_hash"], second["pin_hash"])
    }

    @Test
    fun `wrong pin reports remaining attempts`() {
        auth.setPin("2580")
        assertEquals(VerifyResult.WrongPin(4), auth.verify("0852"))
        assertEquals(VerifyResult.WrongPin(3), auth.verify("0852"))
    }

    @Test
    fun `fifth failure locks input for 30 seconds even for the correct pin`() {
        auth.setPin("2580")
        assertEquals(VerifyResult.LockedOut(30_000), failTimes(PinAuthenticator.MAX_ATTEMPTS))

        clock.advance(Duration.ofSeconds(10))
        assertEquals(VerifyResult.LockedOut(20_000), auth.verify("2580"))

        clock.advance(Duration.ofSeconds(20))
        assertEquals(VerifyResult.Success, auth.verify("2580"))
    }

    @Test
    fun `lockout grows with every repeated lock`() {
        auth.setPin("2580")
        val expected = listOf(30_000L, 60_000L, 300_000L, 900_000L, 900_000L)
        expected.forEach { duration ->
            assertEquals(VerifyResult.LockedOut(duration), failTimes(PinAuthenticator.MAX_ATTEMPTS))
            clock.advance(Duration.ofMillis(duration))
        }
    }

    @Test
    fun `success resets the counters`() {
        auth.setPin("2580")
        failTimes(4)
        assertEquals(VerifyResult.Success, auth.verify("2580"))
        assertEquals(VerifyResult.WrongPin(4), auth.verify("0852"))
    }

    @Test
    fun `clearing the pin wipes every key`() {
        auth.setPin("2580")
        failTimes(5)
        auth.clearPin()
        assertFalse(auth.isPinSet())
        assertTrue(store.snapshot().isEmpty())
    }

    @Test
    fun `setting a new pin resets an active lockout`() {
        auth.setPin("2580")
        failTimes(5)
        auth.setPin("9041")
        assertEquals(VerifyResult.Success, auth.verify("9041"))
    }
}
