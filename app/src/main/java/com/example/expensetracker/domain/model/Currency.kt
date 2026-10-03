package com.example.expensetracker.domain.model

/** Валюти, у яких можна вести витрати. UAH — базова валюта, до якої НБУ дає курс інших. */
enum class Currency(val code: String, val symbol: String) {
    UAH("UAH", "₴"),
    USD("USD", "$"),
    EUR("EUR", "€");

    companion object {
        val BASE: Currency = UAH

        fun fromCode(code: String?): Currency? =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) }
    }
}
