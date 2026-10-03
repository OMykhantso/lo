package com.example.expensetracker.di

import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.usecase.DemoDataSeeder
import java.time.Clock

/** Залежності застосунку (ручний DI). Реалізація — [AppContainer]; у тестах підставляються фейки. */
interface AppDependencies {
    val clock: Clock
    val expenseRepository: ExpenseRepository
    val pinAuthenticator: PinAuthenticator
    val appLock: AppLock
    val demoDataSeeder: DemoDataSeeder
}
