package com.example.expensetracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.expensetracker.di.AppDependencies
import com.example.expensetracker.di.ViewModelFactories
import com.example.expensetracker.domain.notifications.NotificationPermission
import com.example.expensetracker.ui.lock.LockScreenRoot
import com.example.expensetracker.ui.lock.LockViewModel
import com.example.expensetracker.ui.navigation.AppNavHost

/**
 * Навігація + екран блокування PIN-кодом поверх неї. Вміст під блокуванням лишається в композиції
 * (стек навігації зберігається), але прихований від доступності (TalkBack), а дотики перехоплює екран PIN.
 */
@Composable
fun AppContent(
    deps: AppDependencies,
    notificationPermission: NotificationPermission,
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val locked by deps.appLock.locked.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (locked) Modifier.clearAndSetSemantics { } else Modifier),
        ) {
            AppNavHost(deps = deps, notificationPermission = notificationPermission, navController = navController)
        }
        AnimatedVisibility(visible = locked, enter = fadeIn(), exit = fadeOut()) {
            val viewModel: LockViewModel = viewModel(factory = ViewModelFactories.lock(deps))
            LockScreenRoot(viewModel)
        }
    }
}
