package com.emilio.fitarchive.data

import app.cash.sqldelight.driver.native.inMemoryDriver
import com.emilio.fitarchive.db.FitArchiveDatabase

actual fun createTestDatabase(): FitArchiveDatabase = FitArchiveDatabase(inMemoryDriver(FitArchiveDatabase.Schema))
