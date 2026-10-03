package com.example.expensetracker.domain

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.money.RateTable
import com.example.expensetracker.domain.usecase.BalanceSummary
import com.example.expensetracker.domain.usecase.BudgetImpactCalculator
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.domain.validation.BudgetValidation
import com.example.expensetracker.domain.validation.BudgetValidator
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetValidatorTest {
    @Test
    fun `valid limits are parsed into minor units`() {
        assertEquals(BudgetValidation.Valid(3_000_000), BudgetValidator.validate("30000"))
        assertEquals(BudgetValidation.Valid(3_000_050), BudgetValidator.validate("30 000,50"))
        assertEquals(BudgetValidation.Valid(1), BudgetValidator.validate("0.01"))
    }

    @Test
    fun `blank input clears the limit`() {
        assertEquals(BudgetValidation.Cleared, BudgetValidator.validate(""))
        assertEquals(BudgetValidation.Cleared, BudgetValidator.validate("   "))
    }

    @Test
    fun `zero, negative and malformed limits are rejected`() {
        assertEquals(BudgetValidation.Invalid(AmountError.ZERO), BudgetValidator.validate("0"))
        assertEquals(BudgetValidation.Invalid(AmountError.NEGATIVE), BudgetValidator.validate("-100"))
        assertEquals(BudgetValidation.Invalid(AmountError.NOT_A_NUMBER), BudgetValidator.validate("abc"))
        assertEquals(BudgetValidation.Invalid(AmountError.TOO_MANY_DECIMALS), BudgetValidator.validate("10.999"))
        assertEquals(BudgetValidation.Invalid(AmountError.TOO_LARGE), BudgetValidator.validate("1000000000"))
    }
}

class BudgetImpactCalculatorTest {
    private val table = RateTable(mapOf(Currency.USD to BigDecimal("40")))

    private fun summary(spent: Long, budget: Long?, currency: Currency = Currency.UAH) = BalanceSummary(
        currency = currency, spentMinor = spent, budgetMinor = budget,
        remainingMinor = budget?.minus(spent), usedFraction = null, status = BudgetStatus.OK, unconvertibleCount = 0,
    )

    @Test
    fun `shows what remains after the expense`() {
        val impact = BudgetImpactCalculator.evaluate(10_000, Currency.UAH, summary(50_000, 100_000), table)!!
        assertEquals(40_000L, impact.remainingAfterMinor)
        assertEquals(BudgetStatus.OK, impact.status)
    }

    @Test
    fun `warns when the expense crosses 80 percent`() {
        val impact = BudgetImpactCalculator.evaluate(35_000, Currency.UAH, summary(50_000, 100_000), table)!!
        assertEquals(BudgetStatus.WARNING, impact.status)
    }

    @Test
    fun `reports an exceeded budget with a negative remainder`() {
        val impact = BudgetImpactCalculator.evaluate(60_000, Currency.UAH, summary(50_000, 100_000), table)!!
        assertEquals(-10_000L, impact.remainingAfterMinor)
        assertEquals(BudgetStatus.EXCEEDED, impact.status)
    }

    @Test
    fun `converts a foreign currency expense into the display currency`() {
        val impact = BudgetImpactCalculator.evaluate(1_000, Currency.USD, summary(50_000, 100_000), table)!! // $10 = 400 грн
        assertEquals(50_000L - 40_000L, impact.remainingAfterMinor)
    }

    @Test
    fun `no budget or no rate means no estimate`() {
        assertNull(BudgetImpactCalculator.evaluate(100, Currency.UAH, summary(0, null), table))
        assertNull(BudgetImpactCalculator.evaluate(100, Currency.EUR, summary(0, 100_000), table))
    }
}
