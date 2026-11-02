package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import java.util.Properties

actual fun createTestDatabase(): FitArchiveDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    FitArchiveDatabase.Schema.create(driver)
    return FitArchiveDatabase(driver)
}
