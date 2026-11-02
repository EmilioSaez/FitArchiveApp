package com.emilio.fitarchive.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emilio.fitarchive.db.FitArchiveDatabase
import java.util.Properties

actual fun createTestDatabase(): FitArchiveDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    FitArchiveDatabase.Schema.create(driver)
    return FitArchiveDatabase(driver)
}
