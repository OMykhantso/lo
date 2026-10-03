package com.example.expensetracker.domain.usecase

import com.example.expensetracker.data.mock.MockExpenses
import com.example.expensetracker.domain.repository.ExpenseRepository
import java.time.Clock

/** Наповнює базу демонстраційними витратами (кнопка в налаштуваннях) — для показу застосунку без ручного вводу. */
class DemoDataSeeder(
    private val repository: ExpenseRepository,
    private val clock: Clock,
) {
    suspend fun seed() = repository.addAll(MockExpenses.generate(clock.instant(), clock.zone))
}
