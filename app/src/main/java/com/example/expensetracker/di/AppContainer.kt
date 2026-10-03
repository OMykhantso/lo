package com.example.expensetracker.di

import android.content.Context
import com.example.expensetracker.data.mock.MockExpenses
import com.example.expensetracker.data.repository.InMemoryExpenseRepository
import com.example.expensetracker.domain.repository.ExpenseRepository
import java.time.Clock

/** Композиційний корінь застосунку. Живе стільки ж, скільки процес (див. [com.example.expensetracker.ExpenseTrackerApp]). */
class AppContainer(@Suppress("UNUSED_PARAMETER") context: Context) : AppDependencies {
    override val clock: Clock = Clock.systemDefaultZone()

    // Lab 1: дані в пам’яті, засіяні mock-витратами. У Lab 3 замінюється на Room.
    override val expenseRepository: ExpenseRepository = InMemoryExpenseRepository(MockExpenses.generate())
}
