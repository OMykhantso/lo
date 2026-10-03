package com.example.expensetracker.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.expensetracker.domain.security.SecureKeyValueStore
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * [SecureKeyValueStore] на `EncryptedSharedPreferences`: ключі шифруються AES256-SIV, значення — AES256-GCM,
 * майстер-ключ AES-256 зберігається в Android Keystore (апаратно, де це можливо).
 *
 * Якщо Keystore втратив ключ (скидання блокування екрана, відновлення з бекапу на іншому пристрої),
 * зашифрований файл прочитати неможливо: тоді він видаляється й створюється наново — PIN доведеться
 * встановити знову, але застосунок не «цегла».
 */
class EncryptedPrefsKeyValueStore(private val context: Context) : SecureKeyValueStore {
    private val prefs: SharedPreferences by lazy { open() }

    private fun open(): SharedPreferences = try {
        create()
    } catch (e: GeneralSecurityException) {
        recreate()
    } catch (e: IOException) {
        recreate()
    }

    private fun recreate(): SharedPreferences {
        context.deleteSharedPreferences(FILE_NAME)
        return create()
    }

    private fun create(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun remove(vararg keys: String) {
        val editor = prefs.edit()
        keys.forEach { key -> editor.remove(key) }
        editor.apply()
    }

    private companion object {
        const val FILE_NAME = "secure_prefs"
    }
}
