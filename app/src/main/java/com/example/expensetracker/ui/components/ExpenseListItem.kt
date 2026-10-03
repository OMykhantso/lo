package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.expensetracker.domain.model.Expense
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.ui.util.ExpenseDateText
import com.example.expensetracker.ui.util.LocalClock

/** Рядок списку витрат: значок категорії, назва/нотатка, дата, сума. */
@Composable
fun ExpenseListItem(
    expense: Expense,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null,
) {
    val clock = LocalClock.current
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { CategoryAvatar(expense.category) },
        headlineContent = { Text(expense.note.ifBlank { expense.category.label }) },
        supportingContent = {
            val date = ExpenseDateText.format(expense.timestamp, clock)
            Text(if (expense.note.isBlank()) date else "${expense.category.label} · $date")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = MoneyFormatter.format(expense.amountMinor, expense.currency),
                    style = MaterialTheme.typography.titleSmall,
                )
                trailingAction?.invoke()
            }
        },
    )
}
