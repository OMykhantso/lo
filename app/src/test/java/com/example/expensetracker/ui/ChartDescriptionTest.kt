package com.example.expensetracker.ui

import androidx.compose.ui.graphics.Color
import com.example.expensetracker.ui.components.ChartSlice
import com.example.expensetracker.ui.components.chartDescription
import org.junit.Assert.assertEquals
import org.junit.Test

/** Опис діаграми для TalkBack: без нього кругова діаграма недоступна незрячим користувачам. */
class ChartDescriptionTest {
    private fun slice(value: Float, label: String) = ChartSlice(value, Color.Red, label)

    @Test
    fun `lists every slice with its percentage`() {
        val slices = listOf(slice(50f, "Їжа"), slice(30f, "Транспорт"), slice(20f, "Житло"))
        assertEquals("Діаграма витрат: Їжа 50%, Транспорт 30%, Житло 20%", chartDescription(slices, 100f))
    }

    @Test
    fun `empty chart`() {
        assertEquals("Діаграма витрат: даних немає", chartDescription(emptyList(), 0f))
    }
}
