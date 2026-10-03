package com.example.expensetracker.domain

import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.money.AmountInput
import com.example.expensetracker.domain.money.AmountParseResult
import com.example.expensetracker.domain.money.AmountParser
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountParserTest {
    private fun valid(text: String, expectedMinor: Long) =
        assertEquals("«$text»", AmountParseResult.Valid(expectedMinor), AmountParser.parse(text))

    private fun invalid(text: String, expected: AmountError) =
        assertEquals("«$text»", AmountParseResult.Invalid(expected), AmountParser.parse(text))

    @Test
    fun `parses integers and decimals with dot or comma`() {
        valid("125", 12_500)
        valid("125.5", 12_550)
        valid("125,50", 12_550)
        valid("0.01", 1)
        valid("0,99", 99)
        valid("1.07", 107)
    }

    @Test
    fun `tolerates partial input while typing`() {
        valid("5.", 500)
        valid(".5", 50)
        valid(",75", 75)
    }

    @Test
    fun `ignores whitespace including non-breaking space`() {
        valid(" 1 250,00 ", 125_000)
        valid("1 250", 125_000)
    }

    @Test
    fun `ignores leading zeros`() {
        valid("007", 700)
        valid("000.10", 10)
    }

    @Test
    fun `accepts the maximum amount and rejects one kopeck more`() {
        valid("999999999.99", AmountParser.MAX_MINOR)
        invalid("1000000000", AmountError.TOO_LARGE)
        invalid("1000000000.00", AmountError.TOO_LARGE)
    }

    @Test
    fun `empty and blank input`() {
        invalid("", AmountError.EMPTY)
        invalid("   ", AmountError.EMPTY)
    }

    @Test
    fun `zero is not positive`() {
        invalid("0", AmountError.ZERO)
        invalid("0.00", AmountError.ZERO)
        invalid("000", AmountError.ZERO)
        invalid(",0", AmountError.ZERO)
    }

    @Test
    fun `negative amounts are rejected with a dedicated error`() {
        invalid("-1", AmountError.NEGATIVE)
        invalid("-0", AmountError.NEGATIVE)
        invalid("-0.01", AmountError.NEGATIVE)
        invalid("-100,50", AmountError.NEGATIVE)
    }

    @Test
    fun `more than two decimals is rejected, not silently rounded`() {
        invalid("1.234", AmountError.TOO_MANY_DECIMALS)
        invalid("0,001", AmountError.TOO_MANY_DECIMALS)
        invalid("1.500", AmountError.TOO_MANY_DECIMALS)
    }

    @Test
    fun `non numeric garbage`() {
        invalid("abc", AmountError.NOT_A_NUMBER)
        invalid("12a", AmountError.NOT_A_NUMBER)
        invalid("1e3", AmountError.NOT_A_NUMBER)
        invalid("NaN", AmountError.NOT_A_NUMBER)
        invalid("Infinity", AmountError.NOT_A_NUMBER)
        invalid("1.2.3", AmountError.NOT_A_NUMBER)
        invalid("1,2,3", AmountError.NOT_A_NUMBER)
        invalid(".", AmountError.NOT_A_NUMBER)
        invalid("-", AmountError.NOT_A_NUMBER)
        invalid("+5", AmountError.NOT_A_NUMBER)
        invalid("--5", AmountError.NOT_A_NUMBER)
        invalid("١٢٣", AmountError.NOT_A_NUMBER) // арабсько-індійські цифри
    }

    @Test
    fun `input filter keeps digits and a single separator`() {
        assertEquals("12,5", AmountInput.sanitize("12,5"))
        assertEquals("125", AmountInput.sanitize("-1a2 5"))
        assertEquals("1.57", AmountInput.sanitize("1.5.7")) // другий роздільник відкидається, цифри лишаються
        assertEquals("1,57", AmountInput.sanitize("1,5,7"))
        assertEquals("", AmountInput.sanitize("abc"))
        assertEquals(14, AmountInput.sanitize("1".repeat(40)).length)
    }
}
