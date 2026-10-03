package com.example.expensetracker.ui

import com.example.expensetracker.ui.navigation.AddExpenseRoute
import com.example.expensetracker.ui.navigation.AnalyticsRoute
import com.example.expensetracker.ui.navigation.ExpenseHistoryRoute
import com.example.expensetracker.ui.navigation.OverviewRoute
import com.example.expensetracker.ui.navigation.TopLevelDestination
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Маршрути мають бути @Serializable — на цьому тримається типобезпечна навігація 2.8. */
class RoutesTest {
    private val json = Json

    @Test
    fun `history route carries an optional category`() {
        val all = ExpenseHistoryRoute()
        assertNull(all.category)
        assertEquals(all, json.decodeFromString<ExpenseHistoryRoute>(json.encodeToString(all)))

        val food = ExpenseHistoryRoute(category = "FOOD")
        assertEquals(food, json.decodeFromString<ExpenseHistoryRoute>(json.encodeToString(food)))
        assertEquals("""{"category":"FOOD"}""", json.encodeToString(food))
    }

    @Test
    fun `object routes serialise`() {
        assertEquals(OverviewRoute, json.decodeFromString<OverviewRoute>(json.encodeToString(OverviewRoute)))
        assertEquals(AddExpenseRoute, json.decodeFromString<AddExpenseRoute>(json.encodeToString(AddExpenseRoute)))
        assertEquals(AnalyticsRoute, json.decodeFromString<AnalyticsRoute>(json.encodeToString(AnalyticsRoute)))
    }

    @Test
    fun `bottom bar tabs are Overview then Analytics`() {
        assertEquals(listOf(OverviewRoute, AnalyticsRoute), TopLevelDestination.entries.map { it.route })
    }
}
