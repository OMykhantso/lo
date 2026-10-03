package com.example.expensetracker.domain.money

/** Причина, з якої введений текст не є коректною сумою. */
enum class AmountError {
    EMPTY,
    NOT_A_NUMBER,
    NEGATIVE,
    ZERO,
    TOO_MANY_DECIMALS,
    TOO_LARGE,
}

sealed interface AmountParseResult {
    /** [minor] — сума в копійках/центах. */
    data class Valid(val minor: Long) : AmountParseResult
    data class Invalid(val error: AmountError) : AmountParseResult
}

/**
 * Перетворює введений користувачем рядок на суму в мінімальних одиницях.
 *
 * Правила: пробіли (зокрема нерозривні) ігноруються, роздільник — крапка або кома,
 * не більше 2 знаків після коми, значення має бути > 0 і не перевищувати [MAX_MINOR].
 * Експоненційний запис (`1e3`), `NaN`, `Infinity` та будь-які літери — не число.
 */
object AmountParser {
    /** 999 999 999,99 */
    const val MAX_MINOR: Long = 99_999_999_999L
    private const val MAX_INTEGER_DIGITS = 9
    private val NUMBER = Regex("""(\d*)(?:\.(\d*))?""")

    fun parse(raw: String): AmountParseResult {
        val text = raw.filterNot { it.isWhitespace() || it == ' ' }.replace(',', '.')
        if (text.isEmpty()) return invalid(AmountError.EMPTY)

        val negative = text.startsWith('-')
        val body = if (negative) text.substring(1) else text
        val match = NUMBER.matchEntire(body) ?: return invalid(AmountError.NOT_A_NUMBER)
        val integerPart = match.groupValues[1].trimStart('0')
        val fractionPart = match.groupValues[2]
        if (match.groupValues[1].isEmpty() && fractionPart.isEmpty()) {
            return invalid(AmountError.NOT_A_NUMBER)
        }

        if (negative) return invalid(AmountError.NEGATIVE)
        if (fractionPart.length > 2) return invalid(AmountError.TOO_MANY_DECIMALS)
        if (integerPart.length > MAX_INTEGER_DIGITS) return invalid(AmountError.TOO_LARGE)

        val integer = if (integerPart.isEmpty()) 0L else integerPart.toLong()
        val fraction = if (fractionPart.isEmpty()) 0L else fractionPart.padEnd(2, '0').toLong()
        val minor = integer * 100 + fraction

        return when {
            minor == 0L -> invalid(AmountError.ZERO)
            minor > MAX_MINOR -> invalid(AmountError.TOO_LARGE)
            else -> AmountParseResult.Valid(minor)
        }
    }

    private fun invalid(error: AmountError) = AmountParseResult.Invalid(error)
}
