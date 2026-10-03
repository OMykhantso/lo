package com.example.expensetracker.ui

import app.cash.turbine.test
import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.InMemorySecureKeyValueStore
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinHasher
import com.example.expensetracker.domain.security.VerifyResult
import com.example.expensetracker.domain.usecase.DemoDataSeeder
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.MutableClock
import com.example.expensetracker.testutil.keepCollecting
import com.example.expensetracker.testutil.TestData.expense
import com.example.expensetracker.ui.settings.PinDialogError
import com.example.expensetracker.ui.settings.PinStep
import com.example.expensetracker.ui.settings.SettingsEvent
import com.example.expensetracker.ui.settings.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = MutableClock()
    private val auth = PinAuthenticator(InMemorySecureKeyValueStore(), PinHasher(iterations = 1_000), clock)
    private val appLock by lazy { AppLock(auth, clock) }
    private val repository = InMemoryExpenseRepository()
    private val viewModel by lazy {
        SettingsViewModel(auth, appLock, repository, DemoDataSeeder(repository, clock), mainDispatcher.dispatcher)
    }

    private fun type(pin: String) = pin.forEach(viewModel::onPinDigit)

    @Test
    fun `setting a pin takes enter and confirm steps`() = runTest {
        keepCollecting(viewModel.state)
        viewModel.events.test {
            viewModel.onSetPinClicked()
            assertEquals(PinStep.ENTER_NEW, viewModel.state.value.pinDialog?.step)

            type("2580")
            assertEquals(PinStep.CONFIRM_NEW, viewModel.state.value.pinDialog?.step)

            type("2580")
            assertNull(viewModel.state.value.pinDialog)
            assertTrue(viewModel.state.value.pinEnabled)
            assertTrue(auth.isPinSet())
            assertEquals(SettingsEvent.Message("PIN-код встановлено"), awaitItem())
        }
        assertFalse(appLock.locked.value) // користувач щойно автентифікувався
    }

    @Test
    fun `a trivial pin is rejected with a hint`() = runTest {
        keepCollecting(viewModel.state)
        viewModel.onSetPinClicked()
        type("1234")
        val dialog = viewModel.state.value.pinDialog!!
        assertEquals(PinStep.ENTER_NEW, dialog.step)
        assertEquals(PinDialogError.Weak, dialog.error)
        assertEquals(0, dialog.enteredLength)
        assertFalse(auth.isPinSet())
    }

    @Test
    fun `mismatching confirmation returns to the first step`() = runTest {
        keepCollecting(viewModel.state)
        viewModel.onSetPinClicked()
        type("2580")
        type("2581")
        val dialog = viewModel.state.value.pinDialog!!
        assertEquals(PinStep.ENTER_NEW, dialog.step)
        assertEquals(PinDialogError.Mismatch, dialog.error)
        assertFalse(auth.isPinSet())
    }

    @Test
    fun `changing the pin requires the current one first`() = runTest {
        auth.setPin("2580")
        keepCollecting(viewModel.state)

        viewModel.onChangePinClicked()
        assertEquals(PinStep.VERIFY_FOR_CHANGE, viewModel.state.value.pinDialog?.step)

        type("0852")
        assertEquals(PinDialogError.WrongPin(4), viewModel.state.value.pinDialog?.error)

        type("2580")
        assertEquals(PinStep.ENTER_NEW, viewModel.state.value.pinDialog?.step)
        type("9041")
        type("9041")

        assertNull(viewModel.state.value.pinDialog)
        assertEquals(VerifyResult.Success, auth.verify("9041"))
        assertTrue(auth.verify("2580") is VerifyResult.WrongPin)
    }

    @Test
    fun `disabling the pin needs the current pin and removes protection`() = runTest {
        auth.setPin("2580")
        keepCollecting(viewModel.state)

        viewModel.onDisablePinClicked()
        type("2580")

        assertNull(viewModel.state.value.pinDialog)
        assertFalse(auth.isPinSet())
        assertFalse(viewModel.state.value.pinEnabled)
    }

    @Test
    fun `repeated wrong pins in the dialog lock it out`() = runTest {
        auth.setPin("2580")
        keepCollecting(viewModel.state)
        viewModel.onDisablePinClicked()
        repeat(5) { type("0852") }
        assertEquals(PinDialogError.LockedOut(30), viewModel.state.value.pinDialog?.error)
        assertTrue(auth.isPinSet())
    }

    @Test
    fun `dismissing the dialog discards partial input`() = runTest {
        keepCollecting(viewModel.state)
        viewModel.onSetPinClicked()
        type("25")
        viewModel.onPinDialogDismissed()
        assertNull(viewModel.state.value.pinDialog)
        viewModel.onSetPinClicked()
        assertEquals(0, viewModel.state.value.pinDialog?.enteredLength)
    }

    @Test
    fun `demo data can be added and everything can be cleared after confirmation`() = runTest {
        keepCollecting(viewModel.state)
        viewModel.events.test {
            viewModel.onSeedDemoData()
            assertEquals(SettingsEvent.Message("Демо-витрати додано"), awaitItem())
            assertTrue(repository.observeHistory(null).first().isNotEmpty())

            viewModel.onClearAllRequested()
            assertTrue(viewModel.state.value.confirmClearAll)
            viewModel.onClearAllDismissed()
            assertTrue(repository.observeHistory(null).first().isNotEmpty())

            viewModel.onClearAllRequested()
            viewModel.onClearAllConfirmed()
            assertEquals(SettingsEvent.Message("Усі витрати видалено"), awaitItem())
            assertTrue(repository.observeHistory(null).first().isEmpty())
        }
    }

    @Test
    fun `pin keys without an open dialog do nothing`() = runTest {
        keepCollecting(viewModel.state)
        type("2580")
        viewModel.onPinBackspace()
        assertNull(viewModel.state.value.pinDialog)
        assertFalse(auth.isPinSet())
        repository.add(expense())
    }
}
