package com.example.expensetracker.domain.security

import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Стан блокування застосунку PIN-кодом.
 *
 *  - холодний старт: заблоковано, якщо PIN встановлено;
 *  - згортання на ≥ [gracePeriodMillis] блокує знову (коротке перемикання між застосунками — ні);
 *  - зміна/встановлення PIN не вимагає повторного вводу — користувач щойно автентифікувався.
 */
class AppLock(
    private val authenticator: PinAuthenticator,
    private val clock: Clock,
    private val gracePeriodMillis: Long = DEFAULT_GRACE_MILLIS,
) {
    private val _pinEnabled = MutableStateFlow(authenticator.isPinSet())
    private val _locked = MutableStateFlow(_pinEnabled.value)
    private var backgroundedAt: Long? = null

    val pinEnabled: StateFlow<Boolean> = _pinEnabled.asStateFlow()
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun unlock() {
        _locked.value = false
    }

    /** Викликати після встановлення, зміни або вимкнення PIN. */
    fun onPinChanged() {
        _pinEnabled.value = authenticator.isPinSet()
        _locked.value = false
    }

    fun onBackgrounded() {
        backgroundedAt = clock.millis()
    }

    fun onForegrounded() {
        val since = backgroundedAt ?: return
        backgroundedAt = null
        if (_pinEnabled.value && clock.millis() - since >= gracePeriodMillis) _locked.value = true
    }

    companion object {
        const val DEFAULT_GRACE_MILLIS = 30_000L
    }
}
