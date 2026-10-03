package com.example.expensetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.expensetracker.domain.notifications.NotificationPermission
import com.example.expensetracker.ui.lock.PinPad
import com.example.expensetracker.ui.util.message
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

@Composable
fun SettingsScreenRoot(
    viewModel: SettingsViewModel,
    notificationPermission: NotificationPermission,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notificationsGranted by notificationPermission.granted.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }
    SettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        notificationsGranted = notificationsGranted,
        onBack = onBack,
        onReminderToggled = { enabled ->
            viewModel.onReminderToggled(enabled)
            if (enabled && !notificationsGranted) notificationPermission.request()
        },
        onRequestNotifications = notificationPermission::request,
        onBudgetInputChange = viewModel::onBudgetInputChange,
        onBudgetSave = viewModel::onBudgetSave,
        onSetPin = viewModel::onSetPinClicked,
        onChangePin = viewModel::onChangePinClicked,
        onDisablePin = viewModel::onDisablePinClicked,
        onPinDigit = viewModel::onPinDigit,
        onPinBackspace = viewModel::onPinBackspace,
        onPinDialogDismissed = viewModel::onPinDialogDismissed,
        onSeedDemoData = viewModel::onSeedDemoData,
        onClearAll = viewModel::onClearAllRequested,
        onClearAllConfirmed = viewModel::onClearAllConfirmed,
        onClearAllDismissed = viewModel::onClearAllDismissed,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState,
    notificationsGranted: Boolean,
    onBack: () -> Unit,
    onReminderToggled: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
    onBudgetInputChange: (String) -> Unit,
    onBudgetSave: () -> Unit,
    onSetPin: () -> Unit,
    onChangePin: () -> Unit,
    onDisablePin: () -> Unit,
    onPinDigit: (Char) -> Unit,
    onPinBackspace: () -> Unit,
    onPinDialogDismissed: () -> Unit,
    onSeedDemoData: () -> Unit,
    onClearAll: () -> Unit,
    onClearAllConfirmed: () -> Unit,
    onClearAllDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Налаштування") },
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
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Бюджет")
            BudgetSection(state.budgetInput, state.budgetError, onBudgetInputChange, onBudgetSave)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle("Нагадування")
            ReminderSection(state.reminderEnabled, notificationsGranted, onReminderToggled, onRequestNotifications)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle("Безпека")
            PinSection(state.pinEnabled, onSetPin, onChangePin, onDisablePin)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle("Дані")
            ListItem(
                modifier = Modifier.clickable(onClick = onSeedDemoData),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("Додати демо-витрати") },
                supportingContent = { Text("Тестові транзакції за два місяці — щоб побачити діаграми та баланс") },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onClearAll),
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text("Видалити всі витрати", color = MaterialTheme.colorScheme.error) },
            )
        }
    }

    state.pinDialog?.let { dialog ->
        PinDialog(dialog, onPinDigit, onPinBackspace, onPinDialogDismissed)
    }

    if (state.confirmClearAll) {
        AlertDialog(
            onDismissRequest = onClearAllDismissed,
            title = { Text("Видалити всі витрати?") },
            text = { Text("Цю дію неможливо скасувати.") },
            confirmButton = { TextButton(onClick = onClearAllConfirmed) { Text("Видалити") } },
            dismissButton = { TextButton(onClick = onClearAllDismissed) { Text("Скасувати") } },
        )
    }
}

@Composable
private fun ReminderSection(
    enabled: Boolean,
    notificationsGranted: Boolean,
    onToggled: (Boolean) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text("Щоденне нагадування о 20:00") },
        supportingContent = { Text("«Не забудьте зафіксувати сьогоднішні витрати!»") },
        trailingContent = { Switch(checked = enabled, onCheckedChange = onToggled) },
    )
    if (enabled && !notificationsGranted) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Сповіщення вимкнені в системі — нагадування не буде показано.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRequestNotifications) { Text("Дозволити сповіщення") }
        }
    }
}

@Composable
private fun BudgetSection(
    input: String,
    error: com.example.expensetracker.domain.money.AmountError?,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Місячний ліміт витрат") },
            suffix = { Text("₴") },
            singleLine = true,
            isError = error != null,
            supportingText = { Text(error?.message() ?: "Порожнє поле — без ліміту. Залишок рахується за цим лімітом.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        )
        Button(onClick = onSave) { Text("Зберегти бюджет") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun PinSection(enabled: Boolean, onSet: () -> Unit, onChange: () -> Unit, onDisable: () -> Unit) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = { Text("PIN-код") },
        supportingContent = {
            Text(
                if (enabled) "Увімкнено: застосунок блокується після виходу"
                else "Вимкнено: застосунок відкривається без пароля",
            )
        },
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (enabled) {
            Button(onClick = onChange) { Text("Змінити PIN") }
            TextButton(onClick = onDisable) { Text("Вимкнути") }
        } else {
            Button(onClick = onSet) { Text("Встановити PIN") }
        }
    }
}

@Composable
private fun PinDialog(
    dialog: PinDialogState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (title, subtitle) = when (dialog.step) {
        PinStep.VERIFY_FOR_CHANGE -> "Поточний PIN" to "Введіть його, щоб змінити PIN"
        PinStep.VERIFY_FOR_DISABLE -> "Поточний PIN" to "Введіть його, щоб вимкнути захист"
        PinStep.ENTER_NEW -> "Новий PIN" to "4 цифри, без очевидних комбінацій на кшталт 1234"
        PinStep.CONFIRM_NEW -> "Повторіть PIN" to "Введіть той самий PIN ще раз"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            PinPad(
                title = title,
                subtitle = subtitle,
                enteredLength = dialog.enteredLength,
                errorText = dialog.error?.let(::pinDialogErrorText),
                enabled = !dialog.isChecking && dialog.error !is PinDialogError.LockedOut,
                onDigit = onDigit,
                onBackspace = onBackspace,
            )
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

internal fun pinDialogErrorText(error: PinDialogError): String = when (error) {
    is PinDialogError.WrongPin -> "Невірний PIN. Лишилось спроб: ${error.attemptsLeft}"
    is PinDialogError.LockedOut -> "Забагато спроб. Спробуйте через ${error.secondsLeft} с"
    PinDialogError.Weak -> "Занадто простий PIN. Оберіть інший"
    PinDialogError.Mismatch -> "PIN-коди не збігаються. Спробуйте ще раз"
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ExpenseTrackerTheme {
        SettingsScreen(
            state = SettingsUiState(pinEnabled = true),
            snackbarHostState = remember { SnackbarHostState() },
            notificationsGranted = false, onReminderToggled = {}, onRequestNotifications = {},
            onBack = {}, onBudgetInputChange = {}, onBudgetSave = {}, onSetPin = {}, onChangePin = {}, onDisablePin = {}, onPinDigit = {}, onPinBackspace = {},
            onPinDialogDismissed = {}, onSeedDemoData = {}, onClearAll = {}, onClearAllConfirmed = {}, onClearAllDismissed = {},
        )
    }
}
