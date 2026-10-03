package com.example.expensetracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.expensetracker.ui.icons.AppIcons

/** Напрямки нижньої панелі навігації. Порядок визначає напрямок слайду між вкладками. */
enum class TopLevelDestination(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    OVERVIEW(OverviewRoute, "Огляд", Icons.Filled.Home, Icons.Outlined.Home),
    ANALYTICS(AnalyticsRoute, "Аналітика", AppIcons.PieChartFilled, AppIcons.PieChartOutlined),
}
