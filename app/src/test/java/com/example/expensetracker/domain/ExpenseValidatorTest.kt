package com.example.expensetracker.domain

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.validation.CategoryError
import com.example.expensetracker.domain.validation.ExpenseDraft
import com.example.expensetracker.domain.validation.ExpenseValidator
import com.example.expensetracker.domain.validation.NoteError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseValidatorTest {
    @Test
    fun `valid draft passes`() {
        val result = ExpenseValidator.validate(ExpenseDraft("125,50", Category.FOOD, "Обід"))
        assertTrue(result.isValid)
        assertEquals(12_550L, result.amountMinor)
        assertNull(result.amountError)
        assertNull(result.categoryError)
    }

    @Test
    fun `amount must be greater than zero`() {
        val result = ExpenseValidator.validate(ExpenseDraft("0", Category.FOOD))
        assertFalse(result.isValid)
        assertEquals(AmountError.ZERO, result.amountError)
        assertNull(result.amountMinor)
    }

    @Test
    fun `category is required`() {
        val result = ExpenseValidator.validate(ExpenseDraft("10", null))
        assertFalse(result.isValid)
        assertEquals(CategoryError.NOT_SELECTED, result.categoryError)
        assertNull(result.amountError)
    }

    @Test
    fun `all problems are reported at once`() {
        val result = ExpenseValidator.validate(ExpenseDraft("", null, "x".repeat(500)))
        assertEquals(AmountError.EMPTY, result.amountError)
        assertEquals(CategoryError.NOT_SELECTED, result.categoryError)
        assertEquals(NoteError.TOO_LONG, result.noteError)
    }

    @Test
    fun `note length is measured after trimming`() {
        val padded = "  " + "x".repeat(ExpenseValidator.MAX_NOTE_LENGTH) + "  "
        assertTrue(ExpenseValidator.validate(ExpenseDraft("1", Category.OTHER, padded)).isValid)
        val tooLong = "x".repeat(ExpenseValidator.MAX_NOTE_LENGTH + 1)
        assertEquals(NoteError.TOO_LONG, ExpenseValidator.validate(ExpenseDraft("1", Category.OTHER, tooLong)).noteError)
    }
}
