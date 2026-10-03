package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.expensetracker.ui.util.RatesInfo

/** Курси НБУ + стан синхронізації (офлайн/оновлення/застарілий кеш) двома рядками. */
@Composable
fun RatesInfoText(info: RatesInfo, modifier: Modifier = Modifier) {
    if (info.headline == null && info.status == null) return
    Column(modifier = modifier) {
        if (info.headline != null) {
            Text(
                info.headline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (info.status != null) {
            Text(
                info.status,
                style = MaterialTheme.typography.bodySmall,
                color = if (info.isProblem) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
