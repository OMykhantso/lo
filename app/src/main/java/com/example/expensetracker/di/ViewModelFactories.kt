package com.example.expensetracker.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.ui.add.AddExpenseViewModel
import com.example.expensetracker.ui.analytics.AnalyticsViewModel
import com.example.expensetracker.ui.history.HistoryViewModel
import com.example.expensetracker.ui.overview.OverviewViewModel

/** Фабрики ViewModel: конструкторна ін’єкція залежностей без Hilt. */
object ViewModelFactories {
    fun addExpense(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer { AddExpenseViewModel(deps.expenseRepository, deps.clock) }
    }

    fun overview(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer { OverviewViewModel(deps.expenseRepository, deps.clock) }
    }

    fun analytics(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer { AnalyticsViewModel(deps.expenseRepository, deps.clock) }
    }

    fun history(deps: AppDependencies, category: Category?): ViewModelProvider.Factory = viewModelFactory {
        initializer { HistoryViewModel(deps.expenseRepository, category) }
    }
}
