package com.example.expensetracker.ui.theme

import com.example.expensetracker.domain.model.Category

/**
 * Кольори у форматі 0xAARRGGBB. Свідомо БЕЗ залежності від Compose: палітру можна перевіряти
 * звичайними unit-тестами (контрастність WCAG), див. `FinancePaletteTest`.
 *
 * @property accent суцільний колір для графіків, смуг прогресу та акцентів (контраст ≥ 3:1 до поверхні)
 * @property container тональний фон чіпа/іконки категорії
 * @property onContainer текст/іконка на [container] (контраст ≥ 4.5:1)
 */
data class CategoryTone(val accent: Long, val container: Long, val onContainer: Long)

data class CategoryColorSet(val light: CategoryTone, val dark: CategoryTone) {
    fun tone(darkTheme: Boolean): CategoryTone = if (darkTheme) dark else light
}

/**
 * Палітра фінансових категорій (Lab 1, AI-завдання). Принципи:
 *  - 8 різних відтінків (≈25°, 46°, 143°, 187°, 211°, 262°, 330° + нейтральний), щоб категорії
 *    не зливались на діаграмі, зокрема при дейтеранопії/протанопії (відрізняються ще й яскравістю);
 *  - тональні пари M3: `container` ≈ tone 90 / `onContainer` ≈ tone 10 (світла тема)
 *    та tone 30 / tone 90 (темна);
 *  - в темній темі акценти світліші (tone ≈ 70-80), щоб тримати контраст на темній поверхні.
 */
object FinancePalette {
    const val LIGHT_SURFACE: Long = 0xFFF4FBF8
    const val DARK_SURFACE: Long = 0xFF0E1514

    private val food = CategoryColorSet(
        light = CategoryTone(0xFFC24E00, 0xFFFFDBC8, 0xFF4A1700),
        dark = CategoryTone(0xFFFFA06A, 0xFF6B2D00, 0xFFFFDBC8),
    )
    private val transport = CategoryColorSet(
        light = CategoryTone(0xFF1B6EC2, 0xFFD4E4FF, 0xFF001C3A),
        dark = CategoryTone(0xFF86B9FF, 0xFF0B4780, 0xFFD4E4FF),
    )
    private val housing = CategoryColorSet(
        light = CategoryTone(0xFF7A4FC9, 0xFFEADDFF, 0xFF26005C),
        dark = CategoryTone(0xFFC4A8FF, 0xFF4C2C94, 0xFFEADDFF),
    )
    private val entertainment = CategoryColorSet(
        light = CategoryTone(0xFFB5236F, 0xFFFFD9E8, 0xFF3E0024),
        dark = CategoryTone(0xFFFF8FC3, 0xFF7A1048, 0xFFFFD9E8),
    )
    private val health = CategoryColorSet(
        light = CategoryTone(0xFF1E7F46, 0xFFC4F0D2, 0xFF00210E),
        dark = CategoryTone(0xFF6FD995, 0xFF00522A, 0xFFB4F2C6),
    )
    private val shopping = CategoryColorSet(
        light = CategoryTone(0xFF0B7C8C, 0xFFBDEFF7, 0xFF001F25),
        dark = CategoryTone(0xFF56D3E6, 0xFF004E59, 0xFF9DEEFC),
    )
    private val education = CategoryColorSet(
        light = CategoryTone(0xFF8A6A00, 0xFFFFEDB0, 0xFF2D2000),
        dark = CategoryTone(0xFFE8C24A, 0xFF574500, 0xFFFFE08A),
    )
    private val other = CategoryColorSet(
        light = CategoryTone(0xFF58707C, 0xFFDCE7EC, 0xFF111E24),
        dark = CategoryTone(0xFFA9BCC7, 0xFF384A53, 0xFFD5E4EC),
    )

    fun of(category: Category): CategoryColorSet = when (category) {
        Category.FOOD -> food
        Category.TRANSPORT -> transport
        Category.HOUSING -> housing
        Category.ENTERTAINMENT -> entertainment
        Category.HEALTH -> health
        Category.SHOPPING -> shopping
        Category.EDUCATION -> education
        Category.OTHER -> other
    }
}
