package com.example.expensetracker.testutil

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

/**
 * Тримає підписку на [flow] у фоні тестового скоупу (для `stateIn(WhileSubscribed)`),
 * щоб після дій у тесті читати `StateFlow.value`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.keepCollecting(flow: Flow<*>) {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect() }
}
