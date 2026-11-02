package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.driver.native.inMemoryDriver
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

actual fun createTestDatabase(): FitArchiveDatabase = FitArchiveDatabase(inMemoryDriver(FitArchiveDatabase.Schema))
