package com.example.expensetracker.ui.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale

/**
 * Нижня панель навігації Material 3 (Lab 2, AI-завдання). Анімації:
 *  - «пігулка»-індикатор M3 плавно переїжджає між пунктами (вбудовано в [NavigationBarItem]);
 *  - іконка кросфейдом змінюється з контурної на заповнену та «підстрибує» пружиною.
 */
@Composable
fun AppBottomBar(
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            val isSelected = destination == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(destination) },
                icon = { AnimatedTabIcon(destination, isSelected) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun AnimatedTabIcon(destination: TopLevelDestination, selected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabIconScale",
    )
    Crossfade(targetState = selected, animationSpec = tween(200), label = "tabIconCrossfade") { isSelected ->
        Icon(
            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = null, // підпис вже є у label
            modifier = Modifier.scale(scale),
        )
    }
}
