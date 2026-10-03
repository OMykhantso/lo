package com.example.expensetracker.domain.repository

import com.example.expensetracker.domain.model.RatesSnapshot
import kotlinx.coroutines.flow.Flow

enum class RefreshFailure {
    /** Немає з’єднання або таймаут. */
    NO_NETWORK,

    /** Сервер відповів помилкою (HTTP 4xx/5xx). */
    SERVER_ERROR,

    /** Відповідь не вдалося розібрати або в ній немає жодного коректного курсу. */
    INVALID_DATA,
}

sealed interface RefreshResult {
    data class Success(val updatedCount: Int) : RefreshResult
    data class Failure(val reason: RefreshFailure) : RefreshResult
}

/**
 * Курси валют за патерном Offline-First: єдине джерело правди — локальна БД.
 * UI підписується на [rates] і ніколи не чекає мережу; [refresh] лише оновлює кеш.
 */
interface RatesRepository {
    /** Кешовані курси; перевипускаються при кожному оновленні кешу. Порожній знімок, поки курсів не завантажено. */
    val rates: Flow<RatesSnapshot>

    /** Завантажує свіжі курси й зберігає в кеш. Помилки мережі не кидаються — повертаються як [RefreshResult.Failure]. */
    suspend fun refresh(): RefreshResult
}
