package com.example.expensetracker.ui

import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.InMemorySecureKeyValueStore
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.security.PinHasher
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.MutableClock
import com.example.expensetracker.ui.lock.LockViewModel
import com.example.expensetracker.ui.lock.PinPadError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LockViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = MutableClock()
    private val auth = PinAuthenticator(InMemorySecureKeyValueStore(), PinHasher(iterations = 1_000), clock).also { it.setPin("2580") }
    private val appLock = AppLock(auth, clock)
    private val viewModel by lazy { LockViewModel(auth, appLock, mainDispatcher.dispatcher) }

    private fun type(pin: String) = pin.forEach(viewModel::onDigit)

    @Test
    fun `correct pin unlocks the app`() = runTest {
        assertTrue(appLock.locked.value)
        type("2580")
        assertFalse(appLock.locked.value)
        assertEquals(0, viewModel.state.value.enteredLength)
    }

    @Test
    fun `digits fill the dots and backspace removes them`() = runTest {
        type("25")
        assertEquals(2, viewModel.state.value.enteredLength)
        viewModel.onBackspace()
        assertEquals(1, viewModel.state.value.enteredLength)
        viewModel.onBackspace()
        viewModel.onBackspace() // зайвий — нічого не ламає
        assertEquals(0, viewModel.state.value.enteredLength)
    }

    @Test
    fun `wrong pin keeps the app locked, clears input and shows attempts left`() = runTest {
        type("0852")
        assertTrue(appLock.locked.value)
        assertEquals(0, viewModel.state.value.enteredLength)
        assertEquals(PinPadError.WrongPin(attemptsLeft = 4), viewModel.state.value.error)
    }

    @Test
    fun `typing again clears the previous error`() = runTest {
        type("0852")
        viewModel.onDigit('1')
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `digits typed while the pin is being checked are ignored`() = runTest {
        // Перевірка PIN йде на «повільному» диспетчері, тож між четвертою цифрою і результатом є вікно.
        val slow = LockViewModel(auth, appLock, StandardTestDispatcher(testScheduler))
        "2580".forEach(slow::onDigit)
        assertTrue(slow.state.value.isChecking)

        slow.onDigit('9')
        slow.onBackspace()
        assertEquals(4, slow.state.value.enteredLength)

        advanceUntilIdle()
        assertFalse(appLock.locked.value)
        assertFalse(slow.state.value.isChecking)
    }

    @Test
    fun `five failures start a countdown and block the keypad until it ends`() = runTest {
        repeat(5) { type("0852") }
        assertEquals(PinPadError.LockedOut(secondsLeft = 30), viewModel.state.value.error)

        // Під час блокування цифри ігноруються, навіть правильний PIN не пройде.
        type("2580")
        assertTrue(appLock.locked.value)

        advanceTimeBy(10_500)
        assertEquals(PinPadError.LockedOut(secondsLeft = 20), viewModel.state.value.error)

        advanceTimeBy(20_000)
        assertNull(viewModel.state.value.error)
        clock.advance(java.time.Duration.ofSeconds(31))
        type("2580")
        assertFalse(appLock.locked.value)
    }
}
