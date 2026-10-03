package com.example.expensetracker.domain.security

import java.time.Clock
import java.util.Base64

sealed interface SetPinResult {
    data object Success : SetPinResult
    data class Invalid(val error: PinError) : SetPinResult
}

sealed interface VerifyResult {
    data object Success : VerifyResult

    /** Невірний PIN; [attemptsLeft] — скільки спроб лишилось до блокування. */
    data class WrongPin(val attemptsLeft: Int) : VerifyResult

    /** Забагато невдалих спроб: введення заблоковане ще на [remainingMillis]. */
    data class LockedOut(val remainingMillis: Long) : VerifyResult
}

/**
 * Встановлення та перевірка PIN поверх [SecureKeyValueStore].
 *
 * Захист від перебору: після [MAX_ATTEMPTS] помилок поспіль введення блокується, а тривалість
 * блокування росте (30 с → 1 хв → 5 хв → 15 хв). Успішний вхід скидає лічильники.
 *
 * Важливо: 4-значний PIN сам по собі слабкий проти офлайн-перебору хеша; його сила — у
 * шифруванні сховища ключем з Android Keystore та в обмеженні спроб. PIN — це замок інтерфейсу,
 * а не шифрування бази даних.
 */
class PinAuthenticator(
    private val store: SecureKeyValueStore,
    private val hasher: PinHasher,
    private val clock: Clock,
) {
    fun isPinSet(): Boolean = store.getString(KEY_HASH) != null

    fun setPin(pin: String): SetPinResult {
        PinPolicy.validate(pin)?.let { return SetPinResult.Invalid(it) }
        val salt = hasher.newSalt()
        val hash = hasher.hash(pin, salt)
        store.putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
        store.putString(KEY_HASH, Base64.getEncoder().encodeToString(hash))
        store.putLong(KEY_ITERATIONS, hasher.currentIterations.toLong())
        resetCounters()
        return SetPinResult.Success
    }

    fun verify(pin: String): VerifyResult {
        val storedHash = store.getString(KEY_HASH) ?: return VerifyResult.Success // PIN не встановлено
        val now = clock.millis()

        val lockedUntil = store.getLong(KEY_LOCKED_UNTIL, 0L)
        if (now < lockedUntil) return VerifyResult.LockedOut(lockedUntil - now)

        val salt = Base64.getDecoder().decode(store.getString(KEY_SALT).orEmpty())
        val iterations = store.getLong(KEY_ITERATIONS, hasher.currentIterations.toLong()).toInt()
        val expected = Base64.getDecoder().decode(storedHash)

        if (hasher.matches(pin, salt, iterations, expected)) {
            resetCounters()
            return VerifyResult.Success
        }

        val failed = store.getLong(KEY_FAILED, 0L).toInt() + 1
        if (failed >= MAX_ATTEMPTS) {
            val level = store.getLong(KEY_LEVEL, 0L).toInt()
            val duration = LOCKOUT_MILLIS[level.coerceAtMost(LOCKOUT_MILLIS.lastIndex)]
            store.putLong(KEY_FAILED, 0L)
            store.putLong(KEY_LEVEL, level + 1L)
            store.putLong(KEY_LOCKED_UNTIL, now + duration)
            return VerifyResult.LockedOut(duration)
        }
        store.putLong(KEY_FAILED, failed.toLong())
        return VerifyResult.WrongPin(attemptsLeft = MAX_ATTEMPTS - failed)
    }

    fun clearPin() {
        store.remove(KEY_HASH, KEY_SALT, KEY_ITERATIONS, KEY_FAILED, KEY_LEVEL, KEY_LOCKED_UNTIL)
    }

    private fun resetCounters() {
        store.remove(KEY_FAILED, KEY_LEVEL, KEY_LOCKED_UNTIL)
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        val LOCKOUT_MILLIS = longArrayOf(30_000L, 60_000L, 5 * 60_000L, 15 * 60_000L)

        private const val KEY_HASH = "pin_hash"
        private const val KEY_SALT = "pin_salt"
        private const val KEY_ITERATIONS = "pin_iterations"
        private const val KEY_FAILED = "pin_failed_attempts"
        private const val KEY_LEVEL = "pin_lockout_level"
        private const val KEY_LOCKED_UNTIL = "pin_locked_until"
    }
}
