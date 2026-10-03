package com.example.expensetracker.di

import com.example.expensetracker.domain.repository.ExpenseRepository
import java.time.Clock

/** Залежності застосунку (ручний DI). Реалізація — [AppContainer]; у тестах підставляються фейки. */
interface AppDependencies {
    val clock: Clock
    val expenseRepository: ExpenseRepository
}
