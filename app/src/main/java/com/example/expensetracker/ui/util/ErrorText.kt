package com.example.expensetracker.ui.util

import com.example.expensetracker.domain.money.AmountError
import com.example.expensetracker.domain.validation.CategoryError
import com.example.expensetracker.domain.validation.ExpenseValidator
import com.example.expensetracker.domain.validation.NoteError

/** Тексти помилок валідації (домен лишається без UI-рядків). */
fun AmountError.message(): String = when (this) {
    AmountError.EMPTY -> "Введіть суму"
    AmountError.NOT_A_NUMBER -> "Введіть коректне число, наприклад 125,50"
    AmountError.NEGATIVE -> "Сума не може бути від’ємною"
    AmountError.ZERO -> "Сума має бути більшою за 0"
    AmountError.TOO_MANY_DECIMALS -> "Не більше двох цифр після коми"
    AmountError.TOO_LARGE -> "Занадто велика сума"
}

fun CategoryError.message(): String = when (this) {
    CategoryError.NOT_SELECTED -> "Оберіть категорію"
}

fun NoteError.message(): String = when (this) {
    NoteError.TOO_LONG -> "Не більше ${ExpenseValidator.MAX_NOTE_LENGTH} символів"
}
