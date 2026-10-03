package com.example.expensetracker.ui.overview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.usecase.BalanceSummary
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.ui.components.CurrencySelector
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

/**
 * Картка залишку бюджету. Колір відображає стан: норма — основний, ≥80% — третинний (попередження),
 * перевищення — колір помилки. Залишок перераховується в обрану валюту за збереженим курсом (працює офлайн).
 */
@Composable
fun BalanceCard(
    balance: BalanceSummary,
    requestedCurrency: Currency,
    onCurrencySelected: (Currency) -> Unit,
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content, indicator) = when (balance.status) {
        BudgetStatus.EXCEEDED -> Triple(scheme.errorContainer, scheme.onErrorContainer, scheme.error)
        BudgetStatus.WARNING -> Triple(scheme.tertiaryContainer, scheme.onTertiaryContainer, scheme.tertiary)
        else -> Triple(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary)
    }
    val containerColor by animateColorAsState(container, tween(400), label = "balanceContainer")
    val contentColor by animateColorAsState(content, tween(400), label = "balanceContent")
    val indicatorColor by animateColorAsState(indicator, tween(400), label = "balanceIndicator")

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val remaining = balance.remainingMinor
            if (remaining != null && balance.budgetMinor != null) {
                Text("Залишок на місяць", style = MaterialTheme.typography.labelLarge)
                Text(
                    MoneyFormatter.format(remaining, balance.currency),
                    style = MaterialTheme.typography.headlineLarge,
                )
                Text(
                    "з ${MoneyFormatter.format(balance.budgetMinor, balance.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val target = (balance.usedFraction ?: 0f).coerceIn(0f, 1f)
                val animated by animateFloatAsState(target, tween(600), label = "balanceProgress")
                LinearProgressIndicator(
                    progress = { animated },
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = indicatorColor,
                    trackColor = contentColor.copy(alpha = 0.18f),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
                Text(
                    "Витрачено ${MoneyFormatter.format(balance.spentMinor, balance.currency)} " +
                        "(${MoneyFormatter.formatPercent(balance.usedFraction ?: 0f)})",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (balance.status == BudgetStatus.EXCEEDED) {
                    Text("Бюджет перевищено", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Text("Витрачено цього місяця", style = MaterialTheme.typography.labelLarge)
                Text(
                    MoneyFormatter.format(balance.spentMinor, balance.currency),
                    style = MaterialTheme.typography.headlineLarge,
                )
                TextButton(onClick = onSetBudget) { Text("Встановити місячний бюджет") }
            }

            CurrencySelector(selected = requestedCurrency, onSelected = onCurrencySelected)

            if (balance.currency != requestedCurrency) {
                Text(
                    "Курсу ${requestedCurrency.code} ще немає — показано в ${balance.currency.symbol}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (balance.unconvertibleCount > 0) {
                Text(
                    "Не враховано витрат: ${balance.unconvertibleCount} — немає курсу їхньої валюти",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCardPreview() {
    ExpenseTrackerTheme {
        BalanceCard(
            balance = BalanceSummary(
                Currency.UAH, spentMinor = 2_345_075, budgetMinor = 3_000_000, remainingMinor = 654_925,
                usedFraction = 0.78f, status = BudgetStatus.OK, unconvertibleCount = 0,
            ),
            requestedCurrency = Currency.UAH,
            onCurrencySelected = {}, onSetBudget = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BalanceCardExceededPreview() {
    ExpenseTrackerTheme {
        BalanceCard(
            balance = BalanceSummary(
                Currency.USD, spentMinor = 85_000, budgetMinor = 75_000, remainingMinor = -10_000,
                usedFraction = 1.13f, status = BudgetStatus.EXCEEDED, unconvertibleCount = 2,
            ),
            requestedCurrency = Currency.USD,
            onCurrencySelected = {}, onSetBudget = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
