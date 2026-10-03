package com.example.expensetracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.di.ViewModelFactories
import com.example.expensetracker.ui.add.AddExpenseRoute
import com.example.expensetracker.ui.add.AddExpenseViewModel
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val deps = (application as ExpenseTrackerApp).container

        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val context = LocalContext.current
                    val viewModel: AddExpenseViewModel = viewModel(factory = ViewModelFactories.addExpense(deps))
                    AddExpenseRoute(
                        viewModel = viewModel,
                        onSaved = { Toast.makeText(context, "Витрату збережено", Toast.LENGTH_SHORT).show() },
                        onBack = { finish() },
                    )
                }
            }
        }
    }
}
