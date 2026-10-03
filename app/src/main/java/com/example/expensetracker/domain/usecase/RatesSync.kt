package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.repository.RefreshResult
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Idle : SyncState
    data object Loading : SyncState
    data object Synced : SyncState
    data class Failed(val reason: RefreshFailure) : SyncState
}

/**
 * Запускає оновлення курсів у фоновому скоупі застосунку й публікує стан для UI
 * («оновлюється», «офлайн — використовується збережений курс»).
 *
 * Одночасно виконується лише одне оновлення; автоматичні виклики (`force = false`) не частіші за
 * [minIntervalMillis] після останнього успіху, а кнопка «Оновити» (`force = true`) працює завжди.
 */
class RatesSync(
    private val repository: RatesRepository,
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS,
) {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private var lastSuccessAt: Long? = null

    fun refresh(force: Boolean = false) {
        val last = lastSuccessAt
        if (!force && last != null && clock.millis() - last < minIntervalMillis) return

        val current = _state.value
        if (current is SyncState.Loading || !_state.compareAndSet(current, SyncState.Loading)) return
        scope.launch {
            _state.value = when (val result = repository.refresh()) {
                is RefreshResult.Success -> {
                    lastSuccessAt = clock.millis()
                    SyncState.Synced
                }
                is RefreshResult.Failure -> SyncState.Failed(result.reason)
            }
        }
    }

    companion object {
        const val DEFAULT_MIN_INTERVAL_MILLIS = 15 * 60 * 1000L
    }
}
