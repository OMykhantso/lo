package com.example.expensetracker.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.usecase.CategoryBreakdown
import com.example.expensetracker.domain.usecase.CategorySlice
import com.example.expensetracker.ui.components.CategoryBarRow
import com.example.expensetracker.ui.components.ChartSlice
import com.example.expensetracker.ui.components.CurrencySelector
import com.example.expensetracker.ui.components.DonutChart
import com.example.expensetracker.ui.components.RatesInfoText
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.theme.categoryColors
import com.example.expensetracker.ui.util.ExpenseDateText
import com.example.expensetracker.ui.util.LocalClock
import com.example.expensetracker.ui.util.RatesText

@Composable
fun AnalyticsScreenRoot(
    viewModel: AnalyticsViewModel,
    onOpenCategory: (Category) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AnalyticsScreen(
        state = state,
        onOpenCategory = onOpenCategory,
        onCurrencySelected = viewModel::onCurrencySelected,
        onRefreshRates = viewModel::onRefreshRates,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onOpenCategory: (Category) -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onRefreshRates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = LocalClock.current
    val breakdown = state.breakdown
    Scaffold(
        modifier = modifier,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text("Аналітика") },
                actions = {
                    IconButton(onClick = onRefreshRates) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Оновити курси валют")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "header") {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Витрати за ${ExpenseDateText.monthTitle(clock)}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    CurrencySelector(selected = state.requestedCurrency, onSelected = onCurrencySelected)
                    RatesInfoText(RatesText.info(state.rates, state.sync, clock))
                }
            }

            if (breakdown == null || breakdown.slices.isEmpty()) {
                if (!state.isLoading) {
                    item(key = "empty") {
                        Text(
                            "У цьому місяці витрат ще немає.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item(key = "chart") {
                    val slices = breakdown.slices.map { slice ->
                        ChartSlice(slice.totalMinor.toFloat(), categoryColors(slice.category).accent, slice.category.label)
                    }
                    DonutChart(
                        slices = slices,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 12.dp)
                            .widthIn(max = 320.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Разом", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                MoneyFormatter.format(breakdown.totalMinor, breakdown.currency),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                items(breakdown.slices, key = { it.category.name }) { slice ->
                    CategoryBarRow(
                        slice = slice,
                        currency = breakdown.currency,
                        onClick = { onOpenCategory(slice.category) },
                    )
                }
            }

            if (breakdown != null && breakdown.unconvertibleCount > 0) {
                item(key = "unconvertible") {
                    Text(
                        "Не враховано витрат: ${breakdown.unconvertibleCount} — немає курсу їхньої валюти",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AnalyticsScreenPreview() {
    ExpenseTrackerTheme {
        AnalyticsScreen(
            state = AnalyticsUiState(
                isLoading = false,
                breakdown = CategoryBreakdown(
                    currency = Currency.UAH,
                    totalMinor = 2_345_075,
                    slices = listOf(
                        CategorySlice(Category.HOUSING, 1_615_000, 0.69f, 2),
                        CategorySlice(Category.FOOD, 530_570, 0.23f, 6),
                        CategorySlice(Category.ENTERTAINMENT, 199_505, 0.08f, 2),
                    ),
                    unconvertibleCount = 0,
                ),
            ),
            onOpenCategory = {}, onCurrencySelected = {}, onRefreshRates = {},
        )
    }
}
