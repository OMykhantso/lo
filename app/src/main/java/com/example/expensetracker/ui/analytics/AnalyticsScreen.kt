package com.example.expensetracker.ui.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.CategoryTotal
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.ui.components.CategoryAvatar
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.util.ExpenseDateText
import com.example.expensetracker.ui.util.LocalClock

@Composable
fun AnalyticsScreenRoot(
    viewModel: AnalyticsViewModel,
    onOpenCategory: (Category) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AnalyticsScreen(state = state, onOpenCategory = onOpenCategory)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onOpenCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clock = LocalClock.current
    Scaffold(
        modifier = modifier,
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Horizontal),
        topBar = { TopAppBar(title = { Text("Аналітика") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "title") {
                Text(
                    text = "Витрати за ${ExpenseDateText.monthTitle(clock)}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (!state.isLoading && state.rows.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "У цьому місяці витрат ще немає.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.rows, key = { "${it.category}-${it.currency}" }) { row ->
                ListItem(
                    modifier = Modifier.clickable { onOpenCategory(row.category) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = { CategoryAvatar(row.category) },
                    headlineContent = { Text(row.category.label) },
                    supportingContent = { Text("Транзакцій: ${row.count}") },
                    trailingContent = {
                        Text(MoneyFormatter.format(row.totalMinor, row.currency), style = MaterialTheme.typography.titleSmall)
                    },
                )
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
                rows = listOf(
                    CategoryTotal(Category.HOUSING, Currency.UAH, 1_615_000, 2),
                    CategoryTotal(Category.FOOD, Currency.UAH, 280_570, 6),
                    CategoryTotal(Category.ENTERTAINMENT, Currency.USD, 999, 1),
                ),
            ),
            onOpenCategory = {},
        )
    }
}
