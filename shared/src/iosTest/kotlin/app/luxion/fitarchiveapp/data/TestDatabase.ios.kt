package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.driver.native.inMemoryDriver
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

// Igual que la app real (RepositoriesFactory.ios.kt): sin claves foráneas fallan los borrados en cascada.
actual fun createTestDatabase(): FitArchiveDatabase {
    val driver = inMemoryDriver(FitArchiveDatabase.Schema)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    return FitArchiveDatabase(driver)
}
