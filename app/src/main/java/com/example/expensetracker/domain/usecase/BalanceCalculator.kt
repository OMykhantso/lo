package com.example.expensetracker.domain.usecase

import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.RateTable

/** Наскільки вичерпано місячний бюджет. */
enum class BudgetStatus {
    /** Ліміт не задано. */
    NO_BUDGET,

    /** Витрачено менше за [BalanceCalculator.WARNING_THRESHOLD] бюджету. */
    OK,

    /** Витрачено від 80% до 100% включно: залишок малий, але не від’ємний. */
    WARNING,

    /** Витрачено більше за бюджет (залишок < 0). */
    EXCEEDED,
}

/**
 * Підсумок місяця у валюті [currency].
 *
 * @property remainingMinor залишок бюджету; від’ємний, якщо бюджет перевищено; `null`, якщо ліміту немає
 * @property usedFraction частка витраченого бюджету (може бути > 1); `null`, якщо ліміту немає
 * @property unconvertibleCount скільки витрат не враховано, бо немає курсу їхньої валюти
 */
data class BalanceSummary(
    val currency: Currency,
    val spentMinor: Long,
    val budgetMinor: Long?,
    val remainingMinor: Long?,
    val usedFraction: Float?,
    val status: BudgetStatus,
    val unconvertibleCount: Int,
)

/** Реактивний підрахунок залишку: витрати місяця (агрегація з БД) → у валюту відображення → мінус бюджет. */
object BalanceCalculator {
    const val WARNING_THRESHOLD = 0.8

    /**
     * @param totals підсумки місяця `SUM(amount) GROUP BY category, currency`
     * @param budgetUahMinor ліміт у гривнях або `null`
     * @param displayCurrency бажана валюта; якщо для неї немає курсу — розрахунок у гривнях
     */
    fun calculate(
        totals: List<CategoryTotal>,
        budgetUahMinor: Long?,
        table: RateTable,
        displayCurrency: Currency,
    ): BalanceSummary {
        val currency = table.resolveDisplayCurrency(displayCurrency)

        var spent = 0L
        var unconvertible = 0
        // Спершу складаємо в рідній валюті, потім конвертуємо раз на валюту — менше похибок округлення.
        totals.groupBy { it.currency }.forEach { (from, rows) ->
            val converted = table.convert(rows.sumOf { it.totalMinor }, from, currency)
            if (converted == null) unconvertible += rows.sumOf { it.count } else spent += converted
        }

        val budget = budgetUahMinor?.let { table.convert(it, Currency.BASE, currency) }
        return BalanceSummary(
            currency = currency,
            spentMinor = spent,
            budgetMinor = budget,
            remainingMinor = budget?.minus(spent),
            usedFraction = budget?.let { if (it > 0) spent.toDouble().div(it).toFloat() else null },
            status = statusOf(spent, budget),
            unconvertibleCount = unconvertible,
        )
    }

    fun statusOf(spentMinor: Long, budgetMinor: Long?): BudgetStatus = when {
        budgetMinor == null || budgetMinor <= 0 -> BudgetStatus.NO_BUDGET
        spentMinor > budgetMinor -> BudgetStatus.EXCEEDED
        spentMinor >= budgetMinor * WARNING_THRESHOLD -> BudgetStatus.WARNING
        else -> BudgetStatus.OK
    }
}
