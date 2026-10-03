package com.example.expensetracker.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import java.time.Clock

/** Годинник для відносних дат у списках («Сьогодні», «Вчора»). Перевизначається у прев’ю та тестах. */
val LocalClock = staticCompositionLocalOf<Clock> { Clock.systemDefaultZone() }
