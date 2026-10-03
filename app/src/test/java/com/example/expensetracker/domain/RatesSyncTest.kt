package com.example.expensetracker.domain

import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.repository.RefreshResult
import com.example.expensetracker.domain.usecase.RatesSync
import com.example.expensetracker.domain.usecase.SyncState
import com.example.expensetracker.testutil.MutableClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RatesSyncTest {
    private val repository = mockk<RatesRepository>()
    private val clock = MutableClock()

    private fun TestScope.sync() = RatesSync(repository, CoroutineScope(UnconfinedTestDispatcher(testScheduler)), clock)

    @Test
    fun `starts idle, then synced after a successful refresh`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Success(2)
        val sync = sync()
        assertEquals(SyncState.Idle, sync.state.value)

        sync.refresh()

        assertEquals(SyncState.Synced, sync.state.value)
    }

    @Test
    fun `failure is exposed with its reason`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Failure(RefreshFailure.NO_NETWORK)
        val sync = sync()

        sync.refresh()

        assertEquals(SyncState.Failed(RefreshFailure.NO_NETWORK), sync.state.value)
    }

    @Test
    fun `a second refresh while loading is ignored`() = runTest {
        val gate = CompletableDeferred<RefreshResult>()
        coEvery { repository.refresh() } coAnswers { gate.await() }
        val sync = sync()

        sync.refresh()
        assertEquals(SyncState.Loading, sync.state.value)
        sync.refresh(force = true)
        sync.refresh()

        gate.complete(RefreshResult.Success(1))
        assertEquals(SyncState.Synced, sync.state.value)
        coVerify(exactly = 1) { repository.refresh() }
    }

    @Test
    fun `can refresh again right after a failure`() = runTest {
        coEvery { repository.refresh() } returnsMany listOf(
            RefreshResult.Failure(RefreshFailure.SERVER_ERROR), RefreshResult.Success(2),
        )
        val sync = sync()

        sync.refresh()
        sync.refresh()

        assertEquals(SyncState.Synced, sync.state.value)
    }

    @Test
    fun `automatic refreshes are throttled after a success but the button is not`() = runTest {
        coEvery { repository.refresh() } returns RefreshResult.Success(2)
        val sync = sync()

        sync.refresh()
        clock.advance(Duration.ofMinutes(14))
        sync.refresh()                       // надто рано
        coVerify(exactly = 1) { repository.refresh() }

        sync.refresh(force = true)           // кнопка «Оновити»
        coVerify(exactly = 2) { repository.refresh() }

        clock.advance(Duration.ofMinutes(15))
        sync.refresh()                       // інтервал минув
        coVerify(exactly = 3) { repository.refresh() }
    }
}
