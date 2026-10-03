package com.example.expensetracker.ui

import com.example.expensetracker.domain.model.Category
import com.example.expensetracker.ui.theme.FinancePalette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Перевірка доступності палітри за WCAG 2.1: 4.5:1 для тексту, 3:1 для графічних елементів. */
class FinancePaletteTest {

    private fun channel(value: Int): Double {
        val c = value / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Long): Double {
        val r = ((argb shr 16) and 0xFF).toInt()
        val g = ((argb shr 8) and 0xFF).toInt()
        val b = (argb and 0xFF).toInt()
        return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
    }

    private fun contrast(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    @Test
    fun `text on category container meets WCAG AA in both themes`() {
        for (category in Category.entries) {
            for (dark in listOf(false, true)) {
                val tone = FinancePalette.of(category).tone(dark)
                val ratio = contrast(tone.onContainer, tone.container)
                assertTrue("$category dark=$dark text contrast $ratio", ratio >= 4.5)
            }
        }
    }

    @Test
    fun `accent colour is distinguishable from the surface (non-text 3 to 1)`() {
        for (category in Category.entries) {
            val light = contrast(FinancePalette.of(category).light.accent, FinancePalette.LIGHT_SURFACE)
            val dark = contrast(FinancePalette.of(category).dark.accent, FinancePalette.DARK_SURFACE)
            assertTrue("$category light accent $light", light >= 3.0)
            assertTrue("$category dark accent $dark", dark >= 3.0)
        }
    }

    @Test
    fun `every category has its own colours`() {
        val tones = Category.entries.map { FinancePalette.of(it) }
        assertEquals(Category.entries.size, tones.map { it.light.accent }.toSet().size)
        assertEquals(Category.entries.size, tones.map { it.dark.accent }.toSet().size)
        assertEquals(Category.entries.size, tones.map { it.light.container }.toSet().size)
    }

    @Test
    fun `colours are fully opaque`() {
        for (category in Category.entries) {
            val set = FinancePalette.of(category)
            for (tone in listOf(set.light, set.dark)) {
                for (color in listOf(tone.accent, tone.container, tone.onContainer)) {
                    assertEquals("alpha of ${java.lang.Long.toHexString(color)}", 0xFFL, (color shr 24) and 0xFF)
                }
            }
        }
    }
}
