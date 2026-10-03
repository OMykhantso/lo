package com.example.expensetracker.ui.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.data.mock.MockExpenses
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.usecase.BalanceSummary
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.ui.components.ExpenseListItem
import com.example.expensetracker.ui.components.RatesInfoText
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.util.LocalClock
import com.example.expensetracker.ui.util.RatesText

@Composable
fun OverviewScreenRoot(
    viewModel: OverviewViewModel,
    onAddExpense: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    OverviewScreen(
        state = state,
        onAddExpense = onAddExpense,
        onOpenHistory = onOpenHistory,
        onOpenSettings = onOpenSettings,
        onCurrencySelected = viewModel::onCurrencySelected,
        onRefreshRates = viewModel::onRefreshRates,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    state: OverviewUiState,
    onAddExpense: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onCurrencySelected: (Currency) -> Unit,
    onRefreshRates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = LocalClock.current
    Scaffold(
        modifier = modifier,
        // Нижні відступи враховує зовнішній Scaffold із панеллю навігації.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
        topBar = {
            TopAppBar(
                title = { Text("Огляд") },
                actions = {
                    IconButton(onClick = onRefreshRates) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Оновити курси валют")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Налаштування")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddExpense,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Витрата") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.balance?.let { balance ->
                item(key = "balance") {
                    BalanceCard(
                        balance = balance,
                        requestedCurrency = state.requestedCurrency,
                        onCurrencySelected = onCurrencySelected,
                        onSetBudget = onOpenSettings,
                    )
                }
                item(key = "rates") {
                    RatesInfoText(RatesText.info(state.rates, state.sync, clock))
                }
            }
            item(key = "recent-header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Останні витрати", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onOpenHistory) { Text("Усі") }
                }
            }
            if (!state.isLoading && state.recent.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Витрат ще немає. Натисніть «Витрата», щоб додати першу.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.recent, key = { it.id }) { expense -> ExpenseListItem(expense) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OverviewScreenPreview() {
    ExpenseTrackerTheme {
        val recent = MockExpenses.generate().take(5).mapIndexed { i, e -> e.copy(id = i + 1L) }
        OverviewScreen(
            state = OverviewUiState(
                isLoading = false,
                balance = BalanceSummary(
                    Currency.UAH, 2_345_075, 3_000_000, 654_925, 0.78f, BudgetStatus.OK, 0,
                ),
                recent = recent,
            ),
            onAddExpense = {}, onOpenHistory = {}, onOpenSettings = {}, onCurrencySelected = {}, onRefreshRates = {},
        )
    }
}
