package com.example.expensetracker.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.usecase.BudgetImpact
import com.example.expensetracker.domain.usecase.BudgetStatus
import com.example.expensetracker.domain.validation.ExpenseValidator
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.theme.categoryColors
import com.example.expensetracker.ui.util.message

/** Stateful-обгортка: підписується на ViewModel і делегує відображення [AddExpenseScreen]. */
@Composable
fun AddExpenseScreenRoot(
    viewModel: AddExpenseViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddExpenseEvent.Saved -> onSaved()
            }
        }
    }
    AddExpenseScreen(
        state = state,
        onAmountChange = viewModel::onAmountChange,
        onCurrencyChange = viewModel::onCurrencyChange,
        onCategorySelect = viewModel::onCategorySelect,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
        onBack = onBack,
    )
}

/** Stateless-екран (Lab 1): сума, валюта, категорія, нотатка та кнопка збереження з валідацією. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    state: AddExpenseUiState,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (Currency) -> Unit,
    onCategorySelect: (Category) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validation = state.validation
    val amountError = validation.amountError.takeIf { state.showErrors }
    val categoryError = validation.categoryError.takeIf { state.showErrors }
    val noteError = validation.noteError

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Нова витрата") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.amountText,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сума") },
                suffix = { Text(state.currency.symbol) },
                textStyle = MaterialTheme.typography.headlineSmall,
                singleLine = true,
                isError = amountError != null,
                supportingText = { if (amountError != null) Text(amountError.message()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Currency.entries.forEachIndexed { index, currency ->
                    SegmentedButton(
                        selected = state.currency == currency,
                        onClick = { onCurrencyChange(currency) },
                        shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size),
                    ) {
                        Text(currency.code)
                    }
                }
            }

            state.budgetImpact?.let { BudgetImpactText(it) }

            Text("Категорія", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.entries.forEach { category ->
                    val colors = categoryColors(category)
                    val selected = state.category == category
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category.label) },
                        leadingIcon = { Text(category.emoji) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.container,
                            selectedLabelColor = colors.onContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            selectedBorderColor = colors.accent,
                        ),
                    )
                }
            }
            if (categoryError != null) {
                Text(
                    text = categoryError.message(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Нотатка (необов’язково)") },
                singleLine = true,
                isError = noteError != null,
                supportingText = {
                    Text(noteError?.message() ?: "${state.note.trim().length}/${ExpenseValidator.MAX_NOTE_LENGTH}")
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text("Зберегти")
            }
        }
    }
}

/** Підказка під сумою: що лишиться від бюджету або наскільки його буде перевищено. */
@Composable
private fun BudgetImpactText(impact: BudgetImpact) {
    val remaining = MoneyFormatter.format(impact.remainingAfterMinor, impact.currency)
    val (text, color) = when (impact.status) {
        BudgetStatus.EXCEEDED -> {
            val over = MoneyFormatter.format(-impact.remainingAfterMinor, impact.currency)
            "Бюджет буде перевищено на $over" to MaterialTheme.colorScheme.error
        }
        BudgetStatus.WARNING -> "Бюджет майже вичерпано: після витрати лишиться $remaining" to MaterialTheme.colorScheme.tertiary
        else -> "Після цієї витрати лишиться $remaining" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
}

@Preview(showBackground = true)
@Composable
private fun AddExpenseScreenPreview() {
    ExpenseTrackerTheme {
        AddExpenseScreen(
            state = AddExpenseUiState(amountText = "125,50", category = Category.FOOD, note = "Обід"),
            onAmountChange = {}, onCurrencyChange = {}, onCategorySelect = {},
            onNoteChange = {}, onSave = {}, onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddExpenseScreenErrorPreview() {
    ExpenseTrackerTheme {
        AddExpenseScreen(
            state = AddExpenseUiState(amountText = "0", showErrors = true),
            onAmountChange = {}, onCurrencyChange = {}, onCategorySelect = {},
            onNoteChange = {}, onSave = {}, onBack = {},
        )
    }
}
