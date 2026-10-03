package com.example.expensetracker.domain.model

/**
 * Категорії витрат. [name] — стабільний ключ, який зберігається в БД та передається
 * у навігації (`ExpenseHistoryRoute(category: String?)`); [label] — підпис для користувача.
 * Кольори категорій — у `ui.theme.FinancePalette`, щоб домен не залежав від UI.
 */
enum class Category(val label: String, val emoji: String) {
    FOOD("Їжа", "🍔"),
    TRANSPORT("Транспорт", "🚌"),
    HOUSING("Житло", "🏠"),
    ENTERTAINMENT("Розваги", "🎬"),
    HEALTH("Здоров’я", "💊"),
    SHOPPING("Покупки", "🛍️"),
    EDUCATION("Освіта", "📚"),
    OTHER("Інше", "📦");

    companion object {
        fun fromKey(key: String?): Category? = entries.firstOrNull { it.name == key }
    }
}
