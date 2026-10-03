package com.example.expensetracker.data

import com.example.expensetracker.data.local.db.SqlQueries
import java.sql.Connection
import java.sql.DriverManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Перевірка SQL-запитів DAO на реальному SQLite (sqlite-jdbc) без емулятора.
 * DDL нижче відтворює те, що Room генерує для `ExpenseEntity` (таблиця + три індекси); при зміні сутності оновіть і його.
 */
class SqlQueriesTest {
    private lateinit var db: Connection

    @Before
    fun setUp() {
        db = DriverManager.getConnection("jdbc:sqlite::memory:")
        db.createStatement().use {
            it.executeUpdate(
                "CREATE TABLE expenses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, amount INTEGER NOT NULL, " +
                    "currency TEXT NOT NULL, category TEXT NOT NULL, timestamp INTEGER NOT NULL, note TEXT NOT NULL)",
            )
            it.executeUpdate("CREATE INDEX index_expenses_timestamp ON expenses(timestamp)")
            it.executeUpdate("CREATE INDEX index_expenses_aggregation ON expenses(timestamp, category, currency, amount)")
            it.executeUpdate("CREATE INDEX index_expenses_category_timestamp ON expenses(category, timestamp)")
        }
    }

    @After
    fun tearDown() = db.close()

    private fun insert(amount: Long, category: String, timestamp: Long, currency: String = "UAH", note: String = "") {
        db.prepareStatement("INSERT INTO expenses(amount, currency, category, timestamp, note) VALUES (?,?,?,?,?)").use {
            it.setLong(1, amount); it.setString(2, currency); it.setString(3, category)
            it.setLong(4, timestamp); it.setString(5, note); it.executeUpdate()
        }
    }

    /** Виконує запит у стилі Room (`:name`) через JDBC (`?`), прив’язуючи параметри за іменами. */
    private fun run(sql: String, vararg params: Pair<String, Any?>): List<Map<String, Any?>> {
        val names = Regex(":(\\w+)").findAll(sql).map { it.groupValues[1] }.toList()
        val values = params.toMap()
        db.prepareStatement(Regex(":\\w+").replace(sql, "?")).use { statement ->
            names.forEachIndexed { index, name -> statement.setObject(index + 1, values.getValue(name)) }
            statement.executeQuery().use { rs ->
                val columns = (1..rs.metaData.columnCount).map { rs.metaData.getColumnLabel(it) }
                val rows = mutableListOf<Map<String, Any?>>()
                while (rs.next()) rows += columns.associateWith { rs.getObject(it) }
                return rows
            }
        }
    }

    private fun Map<String, Any?>.long(key: String): Long = (getValue(key) as Number).toLong()

    private fun plan(sql: String, vararg params: Pair<String, Any?>): String =
        run("EXPLAIN QUERY PLAN $sql", *params).joinToString("\n") { it["detail"].toString() }

    @Test
    fun `totals are grouped by category and currency`() {
        insert(10_000, "FOOD", 100)
        insert(2_550, "FOOD", 200)
        insert(500, "FOOD", 300, currency = "USD")
        insert(7_000, "HEALTH", 400)

        val rows = run(SqlQueries.CATEGORY_TOTALS, "from" to 0L, "to" to 1_000L)

        assertEquals(3, rows.size)
        val foodUah = rows.single { it["category"] == "FOOD" && it["currency"] == "UAH" }
        assertEquals(12_550L, foodUah.long("total"))
        assertEquals(2, (foodUah["count"] as Number).toInt())
        assertEquals(500L, rows.single { it["category"] == "FOOD" && it["currency"] == "USD" }.long("total"))
        assertEquals(7_000L, rows.single { it["category"] == "HEALTH" }.long("total"))
    }

    @Test
    fun `period is start inclusive and end exclusive`() {
        insert(1, "FOOD", 999)     // до періоду
        insert(10, "FOOD", 1_000)  // рівно початок — враховується
        insert(100, "FOOD", 1_999) // останній момент — враховується
        insert(1_000, "FOOD", 2_000) // рівно кінець — ні

        val rows = run(SqlQueries.CATEGORY_TOTALS, "from" to 1_000L, "to" to 2_000L)

        assertEquals(1, rows.size)
        assertEquals(110L, rows.single().long("total"))
    }

    @Test
    fun `empty table and empty period give no rows`() {
        assertTrue(run(SqlQueries.CATEGORY_TOTALS, "from" to 0L, "to" to 10L).isEmpty())
        insert(100, "FOOD", 50)
        assertTrue(run(SqlQueries.CATEGORY_TOTALS, "from" to 100L, "to" to 200L).isEmpty())
    }

    @Test
    fun `sums are exact in minor units (no floating point drift)`() {
        repeat(10) { insert(10, "FOOD", it.toLong()) } // 10 × 0,10 грн
        assertEquals(100L, run(SqlQueries.CATEGORY_TOTALS, "from" to 0L, "to" to 100L).single().long("total"))
    }

    @Test
    fun `rows come back in a deterministic order`() {
        insert(1, "TRANSPORT", 1)
        insert(1, "FOOD", 1, currency = "USD")
        insert(1, "FOOD", 1)
        val order = run(SqlQueries.CATEGORY_TOTALS, "from" to 0L, "to" to 10L).map { "${it["category"]}-${it["currency"]}" }
        assertEquals(listOf("FOOD-UAH", "FOOD-USD", "TRANSPORT-UAH"), order)
    }

    @Test
    fun `aggregation is answered from the covering index without touching the table`() {
        repeat(500) { insert(100L + it, listOf("FOOD", "HEALTH", "OTHER")[it % 3], it.toLong()) }
        val plan = plan(SqlQueries.CATEGORY_TOTALS, "from" to 0L, "to" to 400L)
        assertTrue(plan, plan.contains("COVERING INDEX index_expenses_aggregation"))
        assertFalse(plan, plan.contains("SCAN expenses") && !plan.contains("INDEX"))
    }

    @Test
    fun `recent is newest first, id breaks ties and limit applies`() {
        insert(1, "FOOD", 10, note = "old")
        insert(1, "FOOD", 30, note = "tie-first")
        insert(1, "FOOD", 30, note = "tie-second")
        insert(1, "FOOD", 20, note = "mid")

        assertEquals(
            listOf("tie-second", "tie-first", "mid"),
            run(SqlQueries.RECENT, "limit" to 3).map { it["note"] },
        )
    }

    @Test
    fun `recent is served by the timestamp index without a separate sort`() {
        repeat(500) { insert(100, "FOOD", it.toLong()) }
        val plan = plan(SqlQueries.RECENT, "limit" to 10)
        assertTrue(plan, plan.contains("index_expenses_timestamp"))
        assertFalse(plan, plan.contains("TEMP B-TREE"))
    }

    @Test
    fun `history by category filters and sorts`() {
        insert(1, "FOOD", 1, note = "f1")
        insert(1, "HEALTH", 2, note = "h")
        insert(1, "FOOD", 3, note = "f2")

        assertEquals(listOf("f2", "f1"), run(SqlQueries.HISTORY_BY_CATEGORY, "category" to "FOOD").map { it["note"] })
        assertEquals(listOf("f2", "h", "f1"), run(SqlQueries.HISTORY_ALL).map { it["note"] })
        assertTrue(run(SqlQueries.HISTORY_BY_CATEGORY, "category" to "HOUSING").isEmpty())
    }

    @Test
    fun `history by category uses the category index without sorting`() {
        repeat(300) { insert(1, if (it % 2 == 0) "FOOD" else "HEALTH", it.toLong()) }
        val plan = plan(SqlQueries.HISTORY_BY_CATEGORY, "category" to "FOOD")
        assertTrue(plan, plan.contains("index_expenses_category_timestamp"))
        assertFalse(plan, plan.contains("TEMP B-TREE"))
    }

    @Test
    fun `delete statements work`() {
        insert(1, "FOOD", 1)
        insert(1, "FOOD", 2)
        db.prepareStatement(SqlQueries.DELETE_BY_ID.replace(":id", "?")).use { it.setLong(1, 1); it.executeUpdate() }
        assertEquals(1, run(SqlQueries.HISTORY_ALL).size)
        db.createStatement().use { it.executeUpdate(SqlQueries.DELETE_ALL) }
        assertTrue(run(SqlQueries.HISTORY_ALL).isEmpty())
    }
}
