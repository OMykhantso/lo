package com.example.expensetracker.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.ui.add.AddExpenseViewModel
import com.example.expensetracker.ui.analytics.AnalyticsViewModel
import com.example.expensetracker.ui.history.HistoryViewModel
import com.example.expensetracker.ui.lock.LockViewModel
import com.example.expensetracker.ui.overview.OverviewViewModel
import com.example.expensetracker.ui.settings.SettingsViewModel

/** Фабрики ViewModel: конструкторна ін’єкція залежностей без Hilt. */
object ViewModelFactories {
    fun addExpense(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer { AddExpenseViewModel(deps.expenseRepository, deps.clock, deps.observeBalance()) }
    }

    fun overview(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            OverviewViewModel(deps.expenseRepository, deps.settingsRepository, deps.ratesSync, deps.observeBalance)
        }
    }

    fun analytics(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            AnalyticsViewModel(deps.settingsRepository, deps.ratesSync, deps.observeCategoryBreakdown)
        }
    }

    fun history(deps: AppDependencies, category: Category?): ViewModelProvider.Factory = viewModelFactory {
        initializer { HistoryViewModel(deps.expenseRepository, category) }
    }

    fun lock(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer { LockViewModel(deps.pinAuthenticator, deps.appLock) }
    }

    fun settings(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer {
            SettingsViewModel(
                deps.pinAuthenticator, deps.appLock, deps.expenseRepository, deps.demoDataSeeder, deps.settingsRepository,
            )
        }
    }
}
