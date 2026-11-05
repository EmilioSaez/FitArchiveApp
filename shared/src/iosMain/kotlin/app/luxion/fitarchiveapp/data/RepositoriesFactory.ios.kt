package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.luxion.fitarchiveapp.backup.IosPhotoStore
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

/** Crea la base de datos local de iOS y los repositorios sobre ella. Llamar una sola vez por proceso. */
fun crearRepositories(): Repositories {
    val driver = NativeSqliteDriver(
        schema = EsquemaFitArchive,
        name = NOMBRE_BD,
        onConfiguration = { it.copy(extendedConfig = it.extendedConfig.copy(foreignKeyConstraints = true)) },
    )
    return Repositories(FitArchiveDatabase(driver), photos = IosPhotoStore())
}
