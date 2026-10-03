package com.example.expensetracker.ui.lock

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LockScreenRoot(viewModel: LockViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LockScreen(state, viewModel::onDigit, viewModel::onBackspace, modifier)
}

/** Повноекранна непрозора поверхня: перекриває вміст і поглинає дотики, доки PIN не введено. */
@Composable
fun LockScreen(
    state: LockUiState,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
            PinPad(
                title = "Введіть PIN-код",
                subtitle = "Трекер витрат заблоковано",
                enteredLength = state.enteredLength,
                errorText = state.error?.let(::pinErrorText),
                enabled = !state.isChecking && state.error !is PinPadError.LockedOut,
                onDigit = onDigit,
                onBackspace = onBackspace,
            )
        }
    }
}

internal fun pinErrorText(error: PinPadError): String = when (error) {
    is PinPadError.WrongPin -> "Невірний PIN. Лишилось спроб: ${error.attemptsLeft}"
    is PinPadError.LockedOut -> "Забагато спроб. Спробуйте через ${error.secondsLeft} с"
}
