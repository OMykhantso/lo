package com.example.expensetracker.ui

import com.example.expensetracker.domain.model.Currency
import com.example.expensetracker.domain.model.RatesSnapshot
import com.example.expensetracker.domain.repository.RefreshFailure
import com.example.expensetracker.domain.usecase.SyncState
import com.example.expensetracker.testutil.FakeRatesRepository.Companion.snapshotOf
import com.example.expensetracker.testutil.MutableClock
import com.example.expensetracker.testutil.TestData
import com.example.expensetracker.ui.util.RatesText
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RatesTextTest {
    private val clock = TestData.clock
    private val fresh = snapshotOf(Currency.USD to "41.5", Currency.EUR to "48")

    @Test
    fun `headline lists the date and both rates`() {
        val info = RatesText.info(fresh, SyncState.Synced, clock)
        assertEquals("Курс НБУ на 14.03.2025: $ 41,50 · € 48,00", info.headline)
        assertNull(info.status)
        assertFalse(info.isProblem)
    }

    @Test
    fun `only the available currencies are listed`() {
        val info = RatesText.info(snapshotOf(Currency.USD to "41.5"), SyncState.Idle, clock)
        assertEquals("Курс НБУ на 14.03.2025: $ 41,50", info.headline)
    }

    @Test
    fun `loading is reported without alarm`() {
        val info = RatesText.info(fresh, SyncState.Loading, clock)
        assertEquals("Оновлення курсів…", info.status)
        assertFalse(info.isProblem)
    }

    @Test
    fun `offline with a cache keeps showing the saved rates and says so`() {
        val info = RatesText.info(fresh, SyncState.Failed(RefreshFailure.NO_NETWORK), clock)
        assertEquals("Курс НБУ на 14.03.2025: $ 41,50 · € 48,00", info.headline)
        assertTrue(info.status!!.startsWith("Офлайн: використовується збережений курс"))
        assertTrue(info.isProblem)
    }

    @Test
    fun `offline without any cache explains that foreign currencies are not counted`() {
        val info = RatesText.info(RatesSnapshot(), SyncState.Failed(RefreshFailure.NO_NETWORK), clock)
        assertNull(info.headline)
        assertTrue(info.status!!.contains("Суми в USD/EUR не буде враховано"))
        assertTrue(info.isProblem)
    }

    @Test
    fun `server and data failures have their own wording`() {
        assertTrue(RatesText.info(fresh, SyncState.Failed(RefreshFailure.SERVER_ERROR), clock).status!!.contains("помилка сервера"))
        assertTrue(RatesText.info(fresh, SyncState.Failed(RefreshFailure.INVALID_DATA), clock).status!!.contains("некоректна відповідь"))
    }

    @Test
    fun `a cache older than two days is called out as stale`() {
        val mutable = MutableClock()
        val old = snapshotOf(Currency.USD to "41.5", fetchedAt = mutable.millis())
        mutable.advance(Duration.ofHours(47))
        assertNull(RatesText.info(old, SyncState.Idle, mutable).status)
        mutable.advance(Duration.ofHours(2))
        val stale = RatesText.info(old, SyncState.Idle, mutable)
        assertTrue(stale.status!!.contains("неактуальним"))
        assertTrue(stale.isProblem)
    }

    @Test
    fun `nothing cached and nothing attempted yet`() {
        val info = RatesText.info(RatesSnapshot(), SyncState.Idle, clock)
        assertNull(info.headline)
        assertEquals("Курси ще не завантажені", info.status)
        assertFalse(info.isProblem)
    }
}
