package com.example.expensetracker.data.local.db

/**
 * SQL-запити DAO винесені в константи, щоб їх можна було перевірити на реальному SQLite
 * в unit-тестах (`SqlQueriesTest`) без емулятора. Ім’я таблиці/колонок — з [ExpenseEntity].
 */
object SqlQueries {
    const val TABLE = "expenses"

    /**
     * Останні N витрат: нові першими, `id` розв’язує рівність часу детерміновано.
     * Порядок `(timestamp, rowid)` збігається з індексом `index_expenses_timestamp`, тож сортування не потрібне.
     */
    const val RECENT = "SELECT * FROM expenses ORDER BY timestamp DESC, id DESC LIMIT :limit"

    /** Уся історія (нові першими). */
    const val HISTORY_ALL = "SELECT * FROM expenses ORDER BY timestamp DESC, id DESC"

    /** Історія однієї категорії; обслуговується індексом `index_expenses_category_timestamp`. */
    const val HISTORY_BY_CATEGORY = "SELECT * FROM expenses WHERE category = :category " +
        "ORDER BY timestamp DESC, id DESC"

    /**
     * Lab 3, AI-завдання: агрегація витрат за період із групуванням за категорією (`SUM(amount) GROUP BY`).
     *
     *  - період напіввідкритий `[from, to)`: рівно початок наступного місяця вже не враховується;
     *  - групуємо ще й за `currency`, бо сума різних валют не має сенсу — зведення робить домен за курсом;
     *  - усі колонки запиту (`timestamp, category, currency, amount`) входять до покривного індексу
     *    `index_expenses_aggregation`, тому SQLite обходиться без звернень до самої таблиці.
     */
    const val CATEGORY_TOTALS = "SELECT category, currency, SUM(amount) AS total, COUNT(*) AS count " +
        "FROM expenses WHERE timestamp >= :from AND timestamp < :to " +
        "GROUP BY category, currency ORDER BY category, currency"

    const val DELETE_BY_ID = "DELETE FROM expenses WHERE id = :id"
    const val DELETE_ALL = "DELETE FROM expenses"
}
