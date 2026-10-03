package com.example.expensetracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * Анімації переходів (Lab 2, AI-завдання):
 *  - між вкладками — горизонтальний слайд у бік руху (Огляд → Аналітика: ліворуч) + fade;
 *  - детальні екрани — fade (під ними вкладка), а самі вони мають власні специфікації нижче.
 */
internal object NavTransitions {
    private const val DURATION = 320
    private const val FADE = 180
    private val easing = FastOutSlowInEasing

    /** Індекс вкладки, якій належить запис back stack, або `null` для не-вкладок. */
    private fun NavBackStackEntry.tabIndex(): Int? {
        val index = TopLevelDestination.entries.indexOfFirst { tab ->
            destination.hierarchy.any { it.hasRoute(tab.route::class) }
        }
        return index.takeIf { it >= 0 }
    }

    /** Напрямок слайду, якщо обидва кінці переходу — вкладки. */
    private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): SlideDirection? {
        val from = initialState.tabIndex() ?: return null
        val to = targetState.tabIndex() ?: return null
        return if (to >= from) SlideDirection.Start else SlideDirection.End
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition {
        val direction = tabDirection()
        return if (direction != null) {
            slideIntoContainer(direction, tween(DURATION, easing = easing), initialOffset = { it / 4 }) +
                fadeIn(tween(DURATION, easing = easing))
        } else {
            fadeIn(tween(DURATION, delayMillis = FADE / 2, easing = easing))
        }
    }

    fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition {
        val direction = tabDirection()
        return if (direction != null) {
            slideOutOfContainer(direction, tween(DURATION, easing = easing), targetOffset = { it / 4 }) +
                fadeOut(tween(FADE, easing = easing))
        } else {
            fadeOut(tween(FADE, easing = easing))
        }
    }

    // --- Екран додавання витрати: «модальний», виїжджає знизу ---------------------------------

    fun addEnter(): EnterTransition =
        slideInVertically(tween(DURATION, easing = easing)) { it / 3 } + fadeIn(tween(DURATION, easing = easing))

    fun addPopExit(): ExitTransition =
        slideOutVertically(tween(DURATION, easing = easing)) { it / 3 } + fadeOut(tween(FADE, easing = easing))

    // --- Історія: «push» зправа ----------------------------------------------------------------

    fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnter(): EnterTransition =
        slideIntoContainer(SlideDirection.Start, tween(DURATION, easing = easing), initialOffset = { it / 3 }) +
            fadeIn(tween(DURATION, easing = easing))

    fun AnimatedContentTransitionScope<NavBackStackEntry>.pushPopExit(): ExitTransition =
        slideOutOfContainer(SlideDirection.End, tween(DURATION, easing = easing), targetOffset = { it / 3 }) +
            fadeOut(tween(FADE, easing = easing))
}
