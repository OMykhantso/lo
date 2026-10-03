package com.example.expensetracker.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ui.add.AddExpenseViewModel

/** Фабрики ViewModel: конструкторна ін’єкція залежностей без Hilt. */
object ViewModelFactories {
    fun addExpense(deps: AppDependencies): ViewModelProvider.Factory = viewModelFactory {
        initializer { AddExpenseViewModel(deps.expenseRepository, deps.clock) }
    }
}
