package com.example.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.expensetracker.domain.model.Category

/** Чи активна зараз темна тема (потрібно кольорам категорій, які живуть поза `ColorScheme`). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}

/** Кольори категорії, узгоджені з поточною (світлою/темною) темою. */
data class CategoryColors(val accent: Color, val container: Color, val onContainer: Color)

@Composable
@ReadOnlyComposable
fun categoryColors(category: Category): CategoryColors {
    val tone = FinancePalette.of(category).tone(LocalDarkTheme.current)
    return CategoryColors(Color(tone.accent), Color(tone.container), Color(tone.onContainer))
}
