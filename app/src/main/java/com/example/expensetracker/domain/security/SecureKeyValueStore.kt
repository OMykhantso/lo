package com.example.expensetracker.domain.security

/**
 * Мінімальне захищене сховище «ключ → значення». Android-реалізація — `EncryptedSharedPreferences`
 * (ключі й значення шифруються AES-256, майстер-ключ лежить в Android Keystore).
 */
interface SecureKeyValueStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun remove(vararg keys: String)
}

/** Сховище в пам’яті — для юніт-тестів і прев’ю. */
class InMemorySecureKeyValueStore : SecureKeyValueStore {
    private val values = linkedMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default
    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun remove(vararg keys: String) { keys.forEach(values::remove) }

    /** Усі збережені значення як рядки (для перевірок «PIN не лежить відкритим текстом»). */
    fun snapshot(): Map<String, String> = values.mapValues { it.value.toString() }
}
