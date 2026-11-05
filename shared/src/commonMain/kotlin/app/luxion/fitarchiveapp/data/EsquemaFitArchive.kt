package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.luxion.fitarchiveapp.db.FitArchiveDatabase

/**
 * Esquema que usan los drivers de Android e iOS: el generado por SQLDelight más la parte de la migración
 * a la v3 que SQL no puede expresar de forma idempotente.
 *
 * Contexto: la versión del esquema fue siempre 1 y no había migraciones, así que toda BD instalada dice ser v1
 * (o v2, si ya pasó por `1.sqm` del avatar) aunque le falte parte del esquema posterior. `2.sqm` crea con
 * `IF NOT EXISTS` las tablas e índices que faltan; las columnas las añade este código solo si no existen
 * (SQLite no tiene `ADD COLUMN IF NOT EXISTS`).
 *
 * Orden de columnas: `ALTER TABLE ADD COLUMN` añade siempre al final, así que una BD migrada y una nueva
 * tienen las mismas columnas en distinto orden físico (p. ej. `Garment.fit`). Por eso las consultas de lectura
 * de las tablas afectadas listan las columnas en vez de `SELECT *` (los mapeadores generados leen por posición).
 *
 * Al cambiar el esquema en el futuro: crear `N.sqm` (no tocar los anteriores, ya están en móviles) y, si se
 * añade una columna a una tabla existente, migrarla con un simple ALTER TABLE y usar columnas explícitas en sus
 * lecturas. Desde la versión 3 el número de versión de la BD es fiable.
 */
object EsquemaFitArchive : SqlSchema<QueryResult.Value<Unit>> {
    private val generado = FitArchiveDatabase.Schema

    override val version: Long get() = generado.version

    override fun create(driver: SqlDriver): QueryResult.Value<Unit> = generado.create(driver)

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
        // Primero las columnas: así las migraciones posteriores ya las encuentran.
        if (oldVersion < VERSION_CON_ESQUEMA_REPARADO) anadirColumnasFaltantes(driver)
        return generado.migrate(driver, oldVersion, newVersion, *callbacks)
    }

    /** Versión (`2.sqm`) a partir de la cual toda BD tiene las columnas posteriores a la v1. */
    private const val VERSION_CON_ESQUEMA_REPARADO = 3L

    /** Columnas añadidas tras la v1: (tabla, columna, definición). La definición debe coincidir con el `.sq`. */
    internal val COLUMNAS_POSTERIORES_A_V1 = listOf(
        Triple("Garment", "fit", "TEXT NOT NULL DEFAULT 'NORMAL' CHECK (fit IN ('SLIM', 'NORMAL', 'OVERSIZE'))"),
        Triple("Measurements", "foot_cm", "REAL"),
        Triple("TripGarment", "manual", "INTEGER NOT NULL DEFAULT 0 CHECK (manual IN (0, 1))"),
    )

    private fun anadirColumnasFaltantes(driver: SqlDriver) {
        for ((tabla, columna, definicion) in COLUMNAS_POSTERIORES_A_V1) {
            val existentes = columnasDe(driver, tabla)
            // Sin columnas la tabla no existe: no es una BD de FitArchive que podamos reparar aquí.
            if (existentes.isNotEmpty() && columna !in existentes) {
                driver.execute(null, "ALTER TABLE $tabla ADD COLUMN $columna $definicion", 0)
            }
        }
    }

    private fun columnasDe(driver: SqlDriver, tabla: String): Set<String> =
        driver.executeQuery(
            identifier = null,
            sql = "PRAGMA table_info($tabla)",
            mapper = { cursor ->
                val nombres = mutableSetOf<String>()
                while (cursor.next().value) cursor.getString(1)?.let(nombres::add)
                QueryResult.Value(nombres)
            },
            parameters = 0,
        ).value
}
