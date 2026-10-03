package com.example.expensetracker.ui.util

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.usecase.SyncState
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Текст про курси для користувача.
 *
 * @property headline `Курс НБУ на 03.10.2025: $ 41,18 · € 48,01` або `null`, якщо курсів ще немає
 * @property status рядок про стан оновлення (завантаження/офлайн/застарілий кеш) або `null`, якщо все добре
 * @property isProblem чи варто підсвітити [status] як попередження
 */
data class RatesInfo(val headline: String?, val status: String?, val isProblem: Boolean)

object RatesText {
    /** Кеш старший за це вважається застарілим (вихідні, збій синхронізації). */
    const val STALE_AFTER_MILLIS = 48L * 60 * 60 * 1000

    private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.forLanguageTag("uk-UA"))

    fun info(snapshot: RatesSnapshot, sync: SyncState, clock: Clock): RatesInfo {
        val headline = if (snapshot.isEmpty) null else buildString {
            append("Курс НБУ на ")
            append(snapshot.rateDate?.format(dateFormat))
            append(": ")
            append(
                listOf(Currency.USD, Currency.EUR)
                    .mapNotNull { currency ->
                        snapshot.rates[currency]?.let { "${currency.symbol} ${MoneyFormatter.formatRate(it.rateToUah)}" }
                    }
                    .joinToString(" · "),
            )
        }

        val fetchedAt = snapshot.fetchedAtMillis
        val stale = fetchedAt != null && clock.millis() - fetchedAt > STALE_AFTER_MILLIS

        return when {
            sync is SyncState.Loading -> RatesInfo(headline, "Оновлення курсів…", isProblem = false)
            sync is SyncState.Failed && snapshot.isEmpty ->
                RatesInfo(null, "Немає курсів: ${failureText(sync.reason)}. Суми в USD/EUR не буде враховано.", isProblem = true)
            sync is SyncState.Failed ->
                RatesInfo(headline, "Офлайн: використовується збережений курс (${failureText(sync.reason)})", isProblem = true)
            snapshot.isEmpty -> RatesInfo(null, "Курси ще не завантажені", isProblem = false)
            stale -> RatesInfo(headline, "Курс може бути неактуальним — оновіть, коли з’явиться інтернет", isProblem = true)
            else -> RatesInfo(headline, null, isProblem = false)
        }
    }

    private fun failureText(reason: RefreshFailure): String = when (reason) {
        RefreshFailure.NO_NETWORK -> "немає зʼєднання"
        RefreshFailure.SERVER_ERROR -> "помилка сервера НБУ"
        RefreshFailure.INVALID_DATA -> "некоректна відповідь НБУ"
    }
}
