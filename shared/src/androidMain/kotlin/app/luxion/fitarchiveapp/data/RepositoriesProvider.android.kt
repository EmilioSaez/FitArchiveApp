package app.luxion.fitarchiveapp.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

@Composable
actual fun rememberRepositories(): Repositories {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        Repositories(FitArchiveDatabase(AndroidSqliteDriver(FitArchiveDatabase.Schema, context, "fitarchive.db")))
    }
}
