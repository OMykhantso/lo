package com.example.expensetracker.di

import com.example.expensetracker.domain.repository.ExpenseRepository
import com.example.expensetracker.domain.repository.RatesRepository
import com.example.expensetracker.domain.repository.SettingsRepository
import com.example.expensetracker.domain.security.AppLock
import com.example.expensetracker.domain.security.PinAuthenticator
import com.example.expensetracker.domain.usecase.DemoDataSeeder
import com.example.expensetracker.domain.usecase.ObserveBalance
import com.example.expensetracker.domain.usecase.ObserveCategoryBreakdown
import com.example.expensetracker.domain.usecase.RatesSync
import java.time.Clock

/** Залежності застосунку (ручний DI). Реалізація — [AppContainer]; у тестах підставляються фейки. */
interface AppDependencies {
    val clock: Clock
    val expenseRepository: ExpenseRepository
    val settingsRepository: SettingsRepository
    val ratesRepository: RatesRepository
    val ratesSync: RatesSync
    val observeBalance: ObserveBalance
    val observeCategoryBreakdown: ObserveCategoryBreakdown
    val pinAuthenticator: PinAuthenticator
    val appLock: AppLock
    val demoDataSeeder: DemoDataSeeder
}
