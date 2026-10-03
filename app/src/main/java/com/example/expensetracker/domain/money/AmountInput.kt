package com.example.expensetracker.domain.money

/** Фільтр введення для числового поля: залишає лише цифри та один роздільник. */
object AmountInput {
    private const val MAX_LENGTH = 14

    fun sanitize(input: String): String {
        val builder = StringBuilder()
        var hasSeparator = false
        for (ch in input) {
            when {
                ch in '0'..'9' -> builder.append(ch)
                (ch == '.' || ch == ',') && !hasSeparator -> {
                    hasSeparator = true
                    builder.append(ch)
                }
            }
            if (builder.length >= MAX_LENGTH) break
        }
        return builder.toString()
    }
}
