package com.example.expensetracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.data.mock.MockExpenses
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.ui.components.ExpenseListItem
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.theme.categoryColors

@Composable
fun HistoryScreenRoot(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryScreen(
        state = state,
        onBack = onBack,
        onCategorySelected = viewModel::onCategorySelected,
        onDeleteRequested = viewModel::onDeleteRequested,
        onDeleteConfirmed = viewModel::onDeleteConfirmed,
        onDeleteDismissed = viewModel::onDeleteDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDeleteRequested: (Expense) -> Unit,
    onDeleteConfirmed: () -> Unit,
    onDeleteDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Історія витрат") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "all") {
                        FilterChip(
                            selected = state.selectedCategory == null,
                            onClick = { onCategorySelected(null) },
                            label = { Text("Усі") },
                        )
                    }
                    items(Category.entries, key = { it.name }) { category ->
                        val colors = categoryColors(category)
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.label) },
                            leadingIcon = { Text(category.emoji) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.container,
                                selectedLabelColor = colors.onContainer,
                            ),
                        )
                    }
                }
            }
            if (!state.isLoading && state.items.isEmpty()) {
                item(key = "empty") {
                    Text(
                        "Немає витрат за цим фільтром.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.items, key = { it.id }) { expense ->
                ExpenseListItem(
                    expense = expense,
                    trailingAction = {
                        IconButton(onClick = { onDeleteRequested(expense) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Видалити витрату")
                        }
                    },
                )
            }
        }
    }

    state.pendingDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = onDeleteDismissed,
            title = { Text("Видалити витрату?") },
            text = {
                Text("${expense.note.ifBlank { expense.category.label }} — ${MoneyFormatter.format(expense.amountMinor, expense.currency)}")
            },
            confirmButton = { TextButton(onClick = onDeleteConfirmed) { Text("Видалити") } },
            dismissButton = { TextButton(onClick = onDeleteDismissed) { Text("Скасувати") } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    ExpenseTrackerTheme {
        HistoryScreen(
            state = HistoryUiState(
                isLoading = false,
                selectedCategory = Category.FOOD,
                items = MockExpenses.generate().filter { it.category == Category.FOOD }.mapIndexed { i, e -> e.copy(id = i + 1L) },
            ),
            onBack = {}, onCategorySelected = {}, onDeleteRequested = {}, onDeleteConfirmed = {}, onDeleteDismissed = {},
        )
    }
}
