package com.example.expensetracker.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinPolicy
import com.example.expensetracker.domain.security.VerifyResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PinPadError {
    data class WrongPin(val attemptsLeft: Int) : PinPadError
    data class LockedOut(val secondsLeft: Int) : PinPadError
}

data class LockUiState(
    val enteredLength: Int = 0,
    val error: PinPadError? = null,
    val isChecking: Boolean = false,
)

/** Екран блокування: збирає 4 цифри, перевіряє через [PinAuthenticator] (поза головним потоком) і знімає блокування. */
class LockViewModel(
    private val authenticator: PinAuthenticator,
    private val appLock: AppLock,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private var entered = ""
    private var countdown: Job? = null

    private val _state = MutableStateFlow(LockUiState())
    val state: StateFlow<LockUiState> = _state.asStateFlow()

    fun onDigit(digit: Char) {
        val current = _state.value
        if (current.isChecking || current.error is PinPadError.LockedOut || entered.length >= PinPolicy.LENGTH) return
        entered += digit
        _state.update { it.copy(enteredLength = entered.length, error = null) }
        if (entered.length == PinPolicy.LENGTH) check()
    }

    fun onBackspace() {
        if (_state.value.isChecking || entered.isEmpty()) return
        entered = entered.dropLast(1)
        _state.update { it.copy(enteredLength = entered.length) }
    }

    private fun check() {
        val pin = entered
        _state.update { it.copy(isChecking = true) }
        viewModelScope.launch {
            val result = withContext(defaultDispatcher) { authenticator.verify(pin) }
            entered = ""
            when (result) {
                VerifyResult.Success -> {
                    _state.value = LockUiState()
                    appLock.unlock()
                }
                is VerifyResult.WrongPin ->
                    _state.value = LockUiState(error = PinPadError.WrongPin(result.attemptsLeft))
                is VerifyResult.LockedOut -> startCountdown(result.remainingMillis)
            }
        }
    }

    private fun startCountdown(remainingMillis: Long) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            var seconds = ((remainingMillis + 999) / 1000).toInt()
            while (seconds > 0) {
                _state.value = LockUiState(error = PinPadError.LockedOut(seconds))
                delay(1_000)
                seconds--
            }
            _state.value = LockUiState()
        }
    }
}
