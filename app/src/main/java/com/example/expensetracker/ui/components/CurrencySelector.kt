package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.expensetracker.domain.model.Currency

/** Перемикач валюти (UAH / USD / EUR) на `SegmentedButton`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(
    selected: Currency,
    onSelected: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        Currency.entries.forEachIndexed { index, currency ->
            SegmentedButton(
                selected = selected == currency,
                onClick = { onSelected(currency) },
                shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size),
            ) {
                Text(currency.code)
            }
        }
    }
}
