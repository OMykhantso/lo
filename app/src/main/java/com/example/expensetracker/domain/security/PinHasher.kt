package com.example.expensetracker.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Односторонній хеш PIN: PBKDF2-HMAC-SHA256 із випадковою сіллю. PIN ніколи не зберігається відкритим текстом.
 * Обчислення навмисно повільне — виконувати поза головним потоком.
 */
class PinHasher(
    private val iterations: Int = DEFAULT_ITERATIONS,
    private val random: SecureRandom = SecureRandom(),
) {
    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also(random::nextBytes)

    fun hash(pin: String, salt: ByteArray, iterations: Int = this.iterations): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Порівняння за сталий час (без витоку через час відповіді). */
    fun matches(pin: String, salt: ByteArray, iterations: Int, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt, iterations), expected)

    val currentIterations: Int get() = iterations

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
