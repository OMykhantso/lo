package com.example.expensetracker.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.example.expensetracker.di.AppDependencies
import com.example.expensetracker.di.ViewModelFactories
import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.domain.notifications.NotificationPermission
import com.example.expensetracker.ui.add.AddExpenseScreenRoot
import com.example.expensetracker.ui.add.AddExpenseViewModel
import com.example.expensetracker.ui.analytics.AnalyticsScreenRoot
import com.example.expensetracker.ui.analytics.AnalyticsViewModel
import com.example.expensetracker.ui.history.HistoryScreenRoot
import com.example.expensetracker.ui.history.HistoryViewModel
import com.example.expensetracker.ui.navigation.NavTransitions.enter
import com.example.expensetracker.ui.navigation.NavTransitions.exit
import com.example.expensetracker.ui.navigation.NavTransitions.pushEnter
import com.example.expensetracker.ui.navigation.NavTransitions.pushPopExit
import com.example.expensetracker.ui.overview.OverviewScreenRoot
import com.example.expensetracker.ui.overview.OverviewViewModel
import com.example.expensetracker.ui.settings.SettingsScreenRoot
import com.example.expensetracker.ui.settings.SettingsViewModel
import com.example.expensetracker.ui.util.LocalClock

/**
 * Корінь навігації: Overview → AddExpense → ExpenseHistory(category?) + вкладка Analytics.
 * Нижня панель показується лише на вкладках і зникає зі слайдом на детальних екранах.
 */
@Composable
fun AppNavHost(
    deps: AppDependencies,
    notificationPermission: NotificationPermission,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    CompositionLocalProvider(LocalClock provides deps.clock) {
        Scaffold(
            modifier = modifier,
            // Системні відступи обробляють самі екрани та панель навігації.
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                AnimatedVisibility(
                    visible = currentTab != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    AppBottomBar(
                        selected = currentTab,
                        onSelect = { tab ->
                            navController.navigate(tab.route) {
                                // Єдиний екземпляр вкладки, збереження її стану між перемиканнями.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = OverviewRoute,
                modifier = Modifier.padding(padding),
                enterTransition = { enter() },
                exitTransition = { exit() },
                popEnterTransition = { enter() },
                popExitTransition = { exit() },
            ) {
                composable<OverviewRoute> {
                    val viewModel: OverviewViewModel = viewModel(factory = ViewModelFactories.overview(deps))
                    OverviewScreenRoot(
                        viewModel = viewModel,
                        onAddExpense = { navController.navigate(AddExpenseRoute) },
                        onOpenHistory = { navController.navigate(ExpenseHistoryRoute(category = null)) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                    )
                }
                composable<AnalyticsRoute> {
                    val viewModel: AnalyticsViewModel = viewModel(factory = ViewModelFactories.analytics(deps))
                    AnalyticsScreenRoot(
                        viewModel = viewModel,
                        onOpenCategory = { category: Category ->
                            navController.navigate(ExpenseHistoryRoute(category = category.name))
                        },
                    )
                }
                composable<AddExpenseRoute>(
                    deepLinks = listOf(navDeepLink<AddExpenseRoute>(basePath = ADD_EXPENSE_DEEP_LINK)),
                    enterTransition = { NavTransitions.addEnter() },
                    popExitTransition = { NavTransitions.addPopExit() },
                ) {
                    val viewModel: AddExpenseViewModel = viewModel(factory = ViewModelFactories.addExpense(deps))
                    AddExpenseScreenRoot(
                        viewModel = viewModel,
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<ExpenseHistoryRoute>(
                    enterTransition = { pushEnter() },
                    popExitTransition = { pushPopExit() },
                ) { entry ->
                    val route = entry.toRoute<ExpenseHistoryRoute>()
                    val viewModel: HistoryViewModel = viewModel(
                        factory = ViewModelFactories.history(deps, Category.fromKey(route.category)),
                    )
                    HistoryScreenRoot(viewModel = viewModel, onBack = { navController.popBackStack() })
                }
                composable<SettingsRoute>(
                    enterTransition = { pushEnter() },
                    popExitTransition = { pushPopExit() },
                ) {
                    val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactories.settings(deps))
                    SettingsScreenRoot(
                        viewModel = viewModel,
                        notificationPermission = notificationPermission,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
