package com.example.expensetracker.domain.model

/**
 * Одна витрата.
 *
 * @property amountMinor сума в мінімальних одиницях валюти (копійки/центи), завжди > 0.
 *   Гроші не зберігаються як Double, щоб уникнути похибок округлення.
 * @property timestamp момент витрати, epoch-мілісекунди (UTC).
 */
data class Expense(
    val id: Long = 0L,
    val amountMinor: Long,
    val currency: Currency,
    val category: Category,
    val timestamp: Long,
    val note: String = "",
) {
    init {
        // Інваріант домену: нульових і від’ємних витрат не існує (повернення коштів — не «витрата»).
        require(amountMinor > 0) { "Expense amount must be positive, was $amountMinor" }
    }
}
