package com.example.expensetracker.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.expensetracker.domain.security.PinPolicy
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

/** Індикатор введених цифр + цифрова клавіатура. Використовується на екрані блокування та в діалозі PIN. */
@Composable
fun PinPad(
    title: String,
    enteredLength: Int,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        PinDots(enteredLength)
        Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            if (errorText != null) {
                Text(
                    errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Keypad(enabled = enabled, onDigit = onDigit, onBackspace = onBackspace)
    }
}

@Composable
private fun PinDots(filled: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.semantics { contentDescription = "Введено цифр: $filled з ${PinPolicy.LENGTH}" },
    ) {
        repeat(PinPolicy.LENGTH) { index ->
            val color = if (index < filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(16.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun Keypad(enabled: Boolean, onDigit: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { digit -> DigitKey(digit, enabled, onDigit) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.size(KEY_SIZE))
            DigitKey('0', enabled, onDigit)
            IconButton(onClick = onBackspace, enabled = enabled, modifier = Modifier.size(KEY_SIZE)) {
                Icon(Icons.Filled.Clear, contentDescription = "Стерти цифру")
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Char, enabled: Boolean, onDigit: (Char) -> Unit) {
    FilledTonalButton(
        onClick = { onDigit(digit) },
        enabled = enabled,
        modifier = Modifier.size(KEY_SIZE),
        shape = CircleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
    ) {
        Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
    }
}

private val KEY_SIZE = 72.dp

@Preview(showBackground = true)
@Composable
private fun PinPadPreview() {
    ExpenseTrackerTheme {
        PinPad(title = "Введіть PIN", enteredLength = 2, onDigit = {}, onBackspace = {}, errorText = "Невірний PIN. Лишилось спроб: 4")
    }
}
