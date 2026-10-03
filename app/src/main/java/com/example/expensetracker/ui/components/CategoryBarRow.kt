package com.example.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.usecase.CategorySlice
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.ui.theme.categoryColors

/** Рядок легенди: значок категорії, назва, сума, відсоток і смуга прогресу (ProgressBar) кольору категорії. */
@Composable
fun CategoryBarRow(
    slice: CategorySlice,
    currency: Currency,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = categoryColors(slice.category)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(slice.category, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(slice.category.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Транзакцій: ${slice.count}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(MoneyFormatter.format(slice.totalMinor, currency), style = MaterialTheme.typography.titleSmall)
                Text(
                    MoneyFormatter.formatPercent(slice.fraction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            progress = { slice.fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = colors.accent,
            trackColor = colors.container,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryBarRowPreview() {
    ExpenseTrackerTheme {
        CategoryBarRow(CategorySlice(Category.FOOD, 280_570, 0.42f, 6), Currency.UAH, onClick = {})
    }
}
