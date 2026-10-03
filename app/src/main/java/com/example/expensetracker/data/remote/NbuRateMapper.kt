package com.example.expensetracker.data.remote

import com.example.expensetracker.domain.model.Currency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Перетворює відповідь НБУ на перевірені курси. Відкидає: невідомі валюти (лишаємо USD/EUR), гривню,
 * відсутні/нульові/від’ємні/нескінченні курси та курси поза розумним діапазоном, неправильні дати.
 * Якщо для валюти кілька записів — береться найсвіжіший.
 */
object NbuRateMapper {
    // STRICT + uuuu: «31.02.2025» має відхилятись, а не тихо ставати 28.02.2025 (поведінка SMART за замовчуванням).
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)
    private const val MIN_RATE = 0.0001
    private const val MAX_RATE = 100_000.0

    fun map(dtos: List<NbuRateDto>): List<RemoteRate> =
        dtos.mapNotNull(::mapOne)
            .groupBy { it.currency }
            .map { (_, rates) -> rates.maxBy { it.date } }
            .sortedBy { it.currency }

    private fun mapOne(dto: NbuRateDto): RemoteRate? {
        val currency = Currency.fromCode(dto.cc) ?: return null
        if (currency == Currency.BASE) return null
        val rate = dto.rate ?: return null
        if (!rate.isFinite() || rate < MIN_RATE || rate > MAX_RATE) return null
        val date = parseDate(dto.exchangedate) ?: return null
        return RemoteRate(currency, BigDecimal.valueOf(rate), date)
    }

    private fun parseDate(text: String?): LocalDate? = try {
        text?.let { LocalDate.parse(it.trim(), DATE_FORMAT) }
    } catch (e: DateTimeParseException) {
        null
    }
}
