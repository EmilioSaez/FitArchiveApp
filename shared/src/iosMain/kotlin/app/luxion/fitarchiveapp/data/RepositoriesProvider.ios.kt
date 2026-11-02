package app.luxion.fitarchiveapp.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

@Composable
actual fun rememberRepositories(): Repositories =
    remember { Repositories(FitArchiveDatabase(NativeSqliteDriver(FitArchiveDatabase.Schema, "fitarchive.db"))) }
