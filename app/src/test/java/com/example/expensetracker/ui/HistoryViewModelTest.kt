package com.example.expensetracker.ui

import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.testutil.MainDispatcherRule
import com.example.expensetracker.testutil.TestData.expense
import com.example.expensetracker.testutil.keepCollecting
import com.example.expensetracker.ui.history.HistoryViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HistoryViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repo = InMemoryExpenseRepository()

    private suspend fun seed() = repo.addAll(
        listOf(
            expense(category = Category.FOOD, note = "a", timestamp = 3),
            expense(category = Category.HEALTH, note = "b", timestamp = 2),
            expense(category = Category.FOOD, note = "c", timestamp = 1),
        ),
    )

    @Test
    fun `route category preselects the filter`() = runTest {
        seed()
        val viewModel = HistoryViewModel(repo, Category.FOOD)
        keepCollecting(viewModel.state)

        assertEquals(Category.FOOD, viewModel.state.value.selectedCategory)
        assertEquals(listOf("a", "c"), viewModel.state.value.items.map { it.note })
    }

    @Test
    fun `null category means everything`() = runTest {
        seed()
        val viewModel = HistoryViewModel(repo, null)
        keepCollecting(viewModel.state)
        assertEquals(listOf("a", "b", "c"), viewModel.state.value.items.map { it.note })
    }

    @Test
    fun `changing the filter switches the list`() = runTest {
        seed()
        val viewModel = HistoryViewModel(repo, null)
        keepCollecting(viewModel.state)

        viewModel.onCategorySelected(Category.HEALTH)
        assertEquals(listOf("b"), viewModel.state.value.items.map { it.note })

        viewModel.onCategorySelected(null)
        assertEquals(3, viewModel.state.value.items.size)
    }

    @Test
    fun `deletion needs confirmation`() = runTest {
        seed()
        val viewModel = HistoryViewModel(repo, null)
        keepCollecting(viewModel.state)
        val target = viewModel.state.value.items.first()

        viewModel.onDeleteRequested(target)
        assertEquals(target, viewModel.state.value.pendingDelete)
        assertEquals(3, viewModel.state.value.items.size)

        viewModel.onDeleteDismissed()
        assertNull(viewModel.state.value.pendingDelete)
        assertEquals(3, viewModel.state.value.items.size)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteConfirmed()
        assertNull(viewModel.state.value.pendingDelete)
        assertEquals(listOf("b", "c"), viewModel.state.value.items.map { it.note })
    }

    @Test
    fun `confirming without a request does nothing`() = runTest {
        seed()
        val viewModel = HistoryViewModel(repo, null)
        keepCollecting(viewModel.state)
        viewModel.onDeleteConfirmed()
        assertEquals(3, viewModel.state.value.items.size)
    }
}
