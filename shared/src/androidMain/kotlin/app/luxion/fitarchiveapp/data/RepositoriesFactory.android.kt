package app.luxion.fitarchiveapp.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.luxion.fitarchiveapp.backup.AndroidPhotoStore
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

/** Crea la base de datos local de Android y los repositorios sobre ella. Llamar una sola vez por proceso. */
fun crearRepositories(context: Context): Repositories {
    val driver = AndroidSqliteDriver(
        schema = EsquemaFitArchive,
        context = context.applicationContext,
        name = NOMBRE_BD,
        callback = object : AndroidSqliteDriver.Callback(EsquemaFitArchive) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
    return Repositories(FitArchiveDatabase(driver), photos = AndroidPhotoStore(context.applicationContext.filesDir))
}
