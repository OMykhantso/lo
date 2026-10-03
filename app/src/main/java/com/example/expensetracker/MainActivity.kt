package com.example.expensetracker

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.di.AppContainer
import com.example.expensetracker.ui.AppContent
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = (application as ExpenseTrackerApp).container

        // Коли PIN увімкнено, вміст не потрапляє в «Нещодавні» та на скриншоти.
        lifecycleScope.launch {
            container.appLock.pinEnabled.collect { enabled ->
                if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    AppContent(deps = container, navController = navController)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        container.appLock.onForegrounded()
        // Тихо оновлюємо курси НБУ (не частіше ніж раз на 15 хв); без мережі лишається кеш у БД.
        container.ratesSync.refresh()
    }

    override fun onStop() {
        super.onStop()
        // Поворот екрана також викликає onStop — але це не «вихід із застосунку».
        if (!isChangingConfigurations) container.appLock.onBackgrounded()
    }
}
