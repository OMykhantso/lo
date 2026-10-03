package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.expensetracker.di.AppContainer
import com.example.expensetracker.notifications.ActivityNotificationPermission
import com.example.expensetracker.ui.AppContent
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private lateinit var notificationPermission: ActivityNotificationPermission
    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container = (application as ExpenseTrackerApp).container
        notificationPermission = ActivityNotificationPermission(this)

        // Коли PIN увімкнено, вміст не потрапляє в «Нещодавні» та на скриншоти.
        lifecycleScope.launch {
            container.appLock.pinEnabled.collect { enabled ->
                if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        // Один раз просимо дозвіл на сповіщення, щоб щоденне нагадування реально показувалось.
        lifecycleScope.launch {
            val settings = container.settingsRepository
            if (!settings.notificationPromptShown.first() && settings.reminderEnabled.first()) {
                settings.setNotificationPromptShown()
                notificationPermission.request()
            }
        }

        setContent {
            ExpenseTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val controller = rememberNavController()
                    SideEffect { navController = controller }
                    AppContent(deps = container, notificationPermission = notificationPermission, navController = controller)
                }
            }
        }
    }

    /**
     * Activity у режимі `singleTop`: тап по сповіщенню, коли застосунок уже відкритий, приходить сюди,
     * а не в `onCreate`. Передаємо intent навігації — вона сама зіставить `expensetracker://add` із маршрутом.
     * (Холодний старт обробляє `NavHost` автоматично за `activity.intent`.)
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }

    override fun onStart() {
        super.onStart()
        container.appLock.onForegrounded()
        notificationPermission.refresh() // користувач міг змінити дозвіл у системних налаштуваннях
        // Тихо оновлюємо курси НБУ (не частіше ніж раз на 15 хв); без мережі лишається кеш у БД.
        container.ratesSync.refresh()
    }

    override fun onStop() {
        super.onStop()
        // Поворот екрана також викликає onStop — але це не «вихід із застосунку».
        if (!isChangingConfigurations) container.appLock.onBackgrounded()
    }
}
