package com.example.expensetracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.notifications.ReminderController
import com.example.expensetracker.domain.money.AmountInput
import com.example.expensetracker.domain.money.MoneyFormatter
import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinPolicy
import com.example.expensetracker.domain.security.VerifyResult
import com.example.expensetracker.domain.usecase.DemoDataSeeder
import com.example.expensetracker.domain.validation.BudgetValidation
import com.example.expensetracker.domain.validation.BudgetValidator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PinStep { VERIFY_FOR_CHANGE, VERIFY_FOR_DISABLE, ENTER_NEW, CONFIRM_NEW }

sealed interface PinDialogError {
    data class WrongPin(val attemptsLeft: Int) : PinDialogError
    data class LockedOut(val secondsLeft: Int) : PinDialogError
    data object Weak : PinDialogError
    data object Mismatch : PinDialogError
}

data class PinDialogState(
    val step: PinStep,
    val enteredLength: Int = 0,
    val error: PinDialogError? = null,
    val isChecking: Boolean = false,
)

data class SettingsUiState(
    val pinEnabled: Boolean = false,
    val pinDialog: PinDialogState? = null,
    val confirmClearAll: Boolean = false,
    /** Текст поля «Місячний бюджет» (у гривнях). */
    val budgetInput: String = "",
    val budgetError: AmountError? = null,
    /** Щоденне нагадування о 20:00 увімкнено. */
    val reminderEnabled: Boolean = true,
)

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
}

class SettingsViewModel(
    private val authenticator: PinAuthenticator,
    private val appLock: AppLock,
    private val repository: ExpenseRepository,
    private val demoDataSeeder: DemoDataSeeder,
    private val settings: SettingsRepository,
    private val reminders: ReminderController,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val local = MutableStateFlow(SettingsUiState())
    private var entered = ""
    private var firstPin: String? = null

    val state: StateFlow<SettingsUiState> = combine(
        local, appLock.pinEnabled, settings.reminderEnabled,
    ) { ui, pinEnabled, reminderEnabled ->
        ui.copy(pinEnabled = pinEnabled, reminderEnabled = reminderEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(pinEnabled = appLock.pinEnabled.value))

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    init {
        // Поле бюджету заповнюється збереженим значенням один раз; далі ним керує користувач.
        viewModelScope.launch {
            val saved = settings.budgetLimitMinor.first()
            local.update { it.copy(budgetInput = saved?.let(MoneyFormatter::formatPlain).orEmpty()) }
        }
    }

    // --- Нагадування -----------------------------------------------------------------------------

    fun onReminderToggled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            if (enabled) reminders.enable() else reminders.disable()
        }
    }

    // --- Бюджет ----------------------------------------------------------------------------------

    fun onBudgetInputChange(text: String) =
        local.update { it.copy(budgetInput = AmountInput.sanitize(text), budgetError = null) }

    fun onBudgetSave() {
        when (val validation = BudgetValidator.validate(local.value.budgetInput)) {
            is BudgetValidation.Invalid -> local.update { it.copy(budgetError = validation.error) }
            BudgetValidation.Cleared -> viewModelScope.launch {
                settings.setBudgetLimit(null)
                _events.send(SettingsEvent.Message("Ліміт бюджету прибрано"))
            }
            is BudgetValidation.Valid -> viewModelScope.launch {
                settings.setBudgetLimit(validation.limitMinor)
                local.update { it.copy(budgetInput = MoneyFormatter.formatPlain(validation.limitMinor)) }
                _events.send(SettingsEvent.Message("Бюджет збережено"))
            }
        }
    }

    // --- PIN -------------------------------------------------------------------------------------

    fun onSetPinClicked() = openDialog(PinStep.ENTER_NEW)

    fun onChangePinClicked() = openDialog(PinStep.VERIFY_FOR_CHANGE)

    fun onDisablePinClicked() = openDialog(PinStep.VERIFY_FOR_DISABLE)

    fun onPinDialogDismissed() {
        entered = ""
        firstPin = null
        local.update { it.copy(pinDialog = null) }
    }

    fun onPinDigit(digit: Char) {
        val dialog = local.value.pinDialog ?: return
        if (dialog.isChecking || dialog.error is PinDialogError.LockedOut || entered.length >= PinPolicy.LENGTH) return
        entered += digit
        updateDialog { it.copy(enteredLength = entered.length, error = null) }
        if (entered.length == PinPolicy.LENGTH) submit(dialog.step, entered)
    }

    fun onPinBackspace() {
        val dialog = local.value.pinDialog ?: return
        if (dialog.isChecking || entered.isEmpty()) return
        entered = entered.dropLast(1)
        updateDialog { it.copy(enteredLength = entered.length) }
    }

    private fun openDialog(step: PinStep) {
        entered = ""
        firstPin = null
        local.update { it.copy(pinDialog = PinDialogState(step)) }
    }

    private fun updateDialog(transform: (PinDialogState) -> PinDialogState) =
        local.update { state -> state.copy(pinDialog = state.pinDialog?.let(transform)) }

    private fun submit(step: PinStep, pin: String) {
        updateDialog { it.copy(isChecking = true) }
        viewModelScope.launch {
            when (step) {
                PinStep.VERIFY_FOR_CHANGE, PinStep.VERIFY_FOR_DISABLE -> verifyCurrent(step, pin)
                PinStep.ENTER_NEW -> enterNew(pin)
                PinStep.CONFIRM_NEW -> confirmNew(pin)
            }
        }
    }

    private suspend fun verifyCurrent(step: PinStep, pin: String) {
        entered = ""
        when (val result = withContext(defaultDispatcher) { authenticator.verify(pin) }) {
            VerifyResult.Success ->
                if (step == PinStep.VERIFY_FOR_CHANGE) {
                    local.update { it.copy(pinDialog = PinDialogState(PinStep.ENTER_NEW)) }
                } else {
                    authenticator.clearPin()
                    appLock.onPinChanged()
                    local.update { it.copy(pinDialog = null) }
                    _events.send(SettingsEvent.Message("PIN-код вимкнено"))
                }
            is VerifyResult.WrongPin ->
                updateDialog { PinDialogState(step, error = PinDialogError.WrongPin(result.attemptsLeft)) }
            is VerifyResult.LockedOut ->
                updateDialog {
                    PinDialogState(step, error = PinDialogError.LockedOut(((result.remainingMillis + 999) / 1000).toInt()))
                }
        }
    }

    private fun enterNew(pin: String) {
        entered = ""
        if (PinPolicy.validate(pin) != null) {
            updateDialog { PinDialogState(PinStep.ENTER_NEW, error = PinDialogError.Weak) }
        } else {
            firstPin = pin
            updateDialog { PinDialogState(PinStep.CONFIRM_NEW) }
        }
    }

    private suspend fun confirmNew(pin: String) {
        entered = ""
        val first = firstPin
        if (first != null && first == pin) {
            withContext(defaultDispatcher) { authenticator.setPin(pin) }
            firstPin = null
            appLock.onPinChanged()
            local.update { it.copy(pinDialog = null) }
            _events.send(SettingsEvent.Message("PIN-код встановлено"))
        } else {
            firstPin = null
            updateDialog { PinDialogState(PinStep.ENTER_NEW, error = PinDialogError.Mismatch) }
        }
    }

    // --- Дані ------------------------------------------------------------------------------------

    fun onSeedDemoData() {
        viewModelScope.launch {
            demoDataSeeder.seed()
            _events.send(SettingsEvent.Message("Демо-витрати додано"))
        }
    }

    fun onClearAllRequested() = local.update { it.copy(confirmClearAll = true) }

    fun onClearAllDismissed() = local.update { it.copy(confirmClearAll = false) }

    fun onClearAllConfirmed() {
        local.update { it.copy(confirmClearAll = false) }
        viewModelScope.launch {
            repository.clear()
            _events.send(SettingsEvent.Message("Усі витрати видалено"))
        }
    }
}
