package com.example.expensetracker.domain.model

import com.example.expensetracker.domain.money.RateTable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Офіційний курс НБУ: скільки гривень коштує одна одиниця [currency].
 *
 * @property rateDate дата, на яку НБУ встановив курс
 * @property fetchedAtMillis коли застосунок завантажив курс (для підказки «курс застарів»)
 */
data class ExchangeRate(
    val currency: Currency,
    val rateToUah: BigDecimal,
    val rateDate: LocalDate,
    val fetchedAtMillis: Long,
) {
    init {
        require(currency != Currency.BASE) { "Base currency has no rate" }
        require(rateToUah.signum() > 0) { "Rate must be positive, was $rateToUah" }
    }
}

/** Усі збережені курси (кеш у БД). Порожній знімок означає «курсів ще не завантажували». */
data class RatesSnapshot(val rates: Map<Currency, ExchangeRate> = emptyMap()) {
    val isEmpty: Boolean get() = rates.isEmpty()

    val table: RateTable by lazy { RateTable(rates.mapValues { it.value.rateToUah }) }

    /** Коли курси востаннє успішно завантажено (найстаріше з наявних — щоб не обіцяти більше, ніж є). */
    val fetchedAtMillis: Long? get() = rates.values.minOfOrNull { it.fetchedAtMillis }

    val rateDate: LocalDate? get() = rates.values.minOfOrNull { it.rateDate }
}
