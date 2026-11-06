package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Migración a la v3: una BD creada con el esquema original (v1) o con el de la v2 (v1 + `Avatar`, de `1.sqm`)
 * y con cualquier mezcla de partes del esquema posterior debe acabar idéntica a una BD nueva y conservar los
 * datos. Regresión del crash de un móvil con la BD antigua ("no such table: WearLog",
 * "no such column: TripGarment.manual").
 */
class MigracionEsquemaTest {
    @Test
    fun `la version del esquema es la 3 para que las BD antiguas se migren`() {
        assertEquals(3L, EsquemaFitArchive.version)
    }

    @Test
    fun `una BD v1 o v2 acaba con las mismas tablas, columnas e indices que una nueva`() {
        paraCadaVersion { desde ->
            val migrada = bdAntigua(desde)
            migrar(migrada, desde)
            assertEquals(esquemaDe(bdNueva()), esquemaDe(migrada), "desde v$desde")
        }
    }

    @Test
    fun `conserva los datos y rellena los valores por defecto de lo nuevo`() {
        paraCadaVersion { desde ->
            val driver = bdAntigua(desde)
            sembrarDatosAntiguos(driver)

            migrar(driver, desde)

            val db = FitArchiveDatabase(driver)
            val prenda = db.garmentQueries.selectAll().executeAsList().single()
            assertEquals(1L, prenda.id)
            assertEquals("a.png", prenda.photo_path)
            assertEquals("Zara", prenda.brand)
            assertEquals("M", prenda.size)
            assertEquals(1L, prenda.favorite)
            assertEquals(2L, prenda.wears_since_wash)
            assertEquals(5L, prenda.total_wears)
            assertEquals(100L, prenda.created_at)
            assertEquals("NORMAL", prenda.fit)

            val medidas = db.measurementsQueries.select().executeAsOne()
            assertEquals(170.0, medidas.height_cm)
            assertEquals(41.0, medidas.leg_cm)
            assertEquals(200L, medidas.updated_at)
            assertNull(medidas.foot_cm)

            // Los dos fallos del móvil: la maleta (TripGarment.manual) y el historial (WearLog).
            val maleta = db.tripQueries.selectPacking(1).executeAsList().single()
            assertEquals(1L, maleta.packed)
            assertEquals(0L, maleta.manual)
            assertTrue(db.wearLogQueries.selectDatesByGarment(1).executeAsList().isEmpty())

            val dia = db.calendarDayQueries.selectBetween("2026-11-01", "2026-11-30").executeAsList().single()
            assertEquals("2026-11-05", dia.date)
            assertEquals(1L, dia.outfit_id)
            assertEquals("nota", dia.note)
            assertEquals("Look", db.outfitQueries.selectAll().executeAsList().single().name)
            assertEquals(1, db.outfitQueries.selectGarments(1).executeAsList().size)
        }
    }

    @Test
    fun `tras migrar se puede escribir y leer lo nuevo`() {
        paraCadaVersion { desde ->
            val driver = bdAntigua(desde)
            sembrarDatosAntiguos(driver)
            migrar(driver, desde)
            val db = FitArchiveDatabase(driver)

            db.wearLogQueries.insert("2026-11-04", 1)
            assertEquals(listOf("2026-11-04"), db.wearLogQueries.selectDatesByGarment(1).executeAsList())
            db.tripQueries.markManual(1, 1)
            assertEquals(1L, db.tripQueries.selectPacking(1).executeAsList().single().manual)
            driver.execute(null, "UPDATE Garment SET fit = 'SLIM' WHERE id = 1", 0)
            assertEquals("SLIM", db.garmentQueries.selectById(1).executeAsOne().fit)
            driver.execute(null, "UPDATE Measurements SET foot_cm = 25.5 WHERE id = 1", 0)
            assertEquals(25.5, db.measurementsQueries.select().executeAsOne().foot_cm)
            db.avatarQueries.select().executeAsOneOrNull() // la tabla Avatar existe
        }
    }

    @Test
    fun `las restricciones CHECK de las columnas nuevas se conservan`() {
        paraCadaVersion { desde ->
            val driver = bdAntigua(desde)
            sembrarDatosAntiguos(driver)
            migrar(driver, desde)

            assertFails { driver.execute(null, "UPDATE Garment SET fit = 'ENORME' WHERE id = 1", 0) }
            assertFails { driver.execute(null, "UPDATE TripGarment SET manual = 2", 0) }
        }
    }

    @Test
    fun `es idempotente en cualquier estado intermedio de una BD antigua`() {
        val estados = mapOf(
            "puro" to emptyList(),
            "con WearLog" to WEARLOG,
            "con fit" to listOf(ALTER_FIT),
            "con foot_cm" to listOf(ALTER_FOOT),
            "con manual" to listOf(ALTER_MANUAL),
            "con todas las columnas" to listOf(ALTER_FIT, ALTER_FOOT, ALTER_MANUAL),
            "con todas las columnas y WearLog" to listOf(ALTER_FIT, ALTER_FOOT, ALTER_MANUAL) + WEARLOG,
            "mezcla" to listOf(ALTER_MANUAL) + WEARLOG,
        )
        val esperado = esquemaDe(bdNueva())
        paraCadaVersion { desde ->
            for ((nombre, extra) in estados) {
                val driver = bdAntigua(desde, extra)
                sembrarDatosAntiguos(driver)
                migrar(driver, desde)
                assertEquals(esperado, esquemaDe(driver), "desde v$desde, estado: $nombre")
                val prenda = FitArchiveDatabase(driver).garmentQueries.selectAll().executeAsList().single()
                assertEquals("a.png", prenda.photo_path, "desde v$desde, estado: $nombre")
            }
        }
    }

    @Test
    fun `volver a aplicar la reparacion v2 a v3 no falla ni cambia nada`() {
        paraCadaVersion { desde ->
            val driver = bdAntigua(desde)
            sembrarDatosAntiguos(driver)
            migrar(driver, desde)
            migrar(driver, 2) // `1.sqm` es el del avatar y no es idempotente: solo se repite lo nuestro
            assertEquals(esquemaDe(bdNueva()), esquemaDe(driver), "desde v$desde")
        }
    }

    @Test
    fun `una BD migrada y una nueva, con distinto orden fisico de columnas, devuelven lo mismo`() {
        val nueva = bdNueva()
        FitArchiveDatabase(nueva).garmentQueries.insert("a.png", "SHIRT", "#ffffff", "ALL", "CASUAL", "Zara", "M", "NORMAL", 1, 0, 2, 5, null, 100)
        nueva.execute(null, "INSERT INTO Measurements (id, height_cm, bust_cm, waist_cm, hip_cm, shoulders_cm, arm_cm, leg_cm, updated_at) VALUES (1, 170.0, 90.0, 70.0, 95.0, 40.0, 60.0, 41.0, 200)", 0)
        val dbNueva = FitArchiveDatabase(nueva)

        paraCadaVersion { desde ->
            val migrada = bdAntigua(desde)
            sembrarDatosAntiguos(migrada)
            migrar(migrada, desde)
            // `fit` está en medio en la BD nueva y al final en la migrada; las lecturas no deben notarlo.
            assertEquals(columnasEnOrden(nueva, "Garment").indexOf("size") + 1, columnasEnOrden(nueva, "Garment").indexOf("fit"))
            assertEquals("fit", columnasEnOrden(migrada, "Garment").last())

            val db = FitArchiveDatabase(migrada)
            assertEquals(dbNueva.garmentQueries.selectAll().executeAsList(), db.garmentQueries.selectAll().executeAsList(), "selectAll, desde v$desde")
            assertEquals(dbNueva.garmentQueries.selectById(1).executeAsOne(), db.garmentQueries.selectById(1).executeAsOne(), "selectById, desde v$desde")
            assertEquals(dbNueva.garmentQueries.selectFavorites().executeAsList(), db.garmentQueries.selectFavorites().executeAsList(), "selectFavorites, desde v$desde")
            assertEquals(dbNueva.garmentQueries.selectFiltered(null, null, null, null, null, null).executeAsList(), db.garmentQueries.selectFiltered(null, null, null, null, null, null).executeAsList(), "selectFiltered, desde v$desde")
            assertEquals(dbNueva.measurementsQueries.select().executeAsOne(), db.measurementsQueries.select().executeAsOne(), "measurements, desde v$desde")
        }
    }

    @Test
    fun `una BD v2 que ya tiene el esquema actual migra sin cambios`() {
        val driver = bdNueva()
        FitArchiveDatabase(driver).garmentQueries.insert("b.png", "SHIRT", "#000", "ALL", "CASUAL", null, null, "SLIM", 0, 0, 0, 0, null, 1)
        migrar(driver, 2)
        assertEquals(esquemaDe(bdNueva()), esquemaDe(driver))
        assertEquals("SLIM", FitArchiveDatabase(driver).garmentQueries.selectAll().executeAsList().single().fit)
    }

    @Test
    fun `COLUMNAS_POSTERIORES_A_V1 cubre todas las columnas que v1 no tenia`() {
        val v1 = esquemaDe(bdAntigua(1))
        val actual = esquemaDe(bdNueva())
        val nuevas = actual.tablas.flatMap { (tabla, columnas) ->
            val antes = v1.tablas[tabla]?.map { it.nombre }?.toSet() ?: return@flatMap emptyList()
            columnas.filter { it.nombre !in antes }.map { tabla to it.nombre }
        }.toSet()
        assertEquals(nuevas, EsquemaFitArchive.COLUMNAS_POSTERIORES_A_V1.map { it.first to it.second }.toSet())
    }

    // --- utilidades ---

    private data class Columna(val nombre: String, val tipo: String, val notNull: Boolean, val defecto: String?, val pk: Int)

    /** Las columnas se comparan como conjunto: una BD migrada las tiene en distinto orden físico que una nueva. */
    private data class Esquema(val tablas: Map<String, Set<Columna>>, val indices: Set<String>)

    /** Versiones de esquema de las que puede venir una BD instalada. */
    private fun paraCadaVersion(bloque: (desde: Long) -> Unit) = VERSIONES_ANTIGUAS.forEach(bloque)

    private fun migrar(driver: JdbcSqliteDriver, desde: Long) {
        EsquemaFitArchive.migrate(driver, desde, EsquemaFitArchive.version)
    }

    private fun nuevoDriver() = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })

    private fun bdNueva() = nuevoDriver().also { FitArchiveDatabase.Schema.create(it) }

    /**
     * BD tal como la dejó la versión [desde] más, opcionalmente, partes posteriores ya aplicadas ([extra]).
     * La v2 es la v1 con `Avatar` (lo que crea `1.sqm`).
     */
    private fun bdAntigua(desde: Long, extra: List<String> = emptyList()) = nuevoDriver().also { driver ->
        val avatar = if (desde >= 2) AVATAR else emptyList()
        (DDL_V1 + avatar + extra).forEach { driver.execute(null, it, 0) }
    }

    private fun sembrarDatosAntiguos(driver: JdbcSqliteDriver) {
        listOf(
            "INSERT INTO Garment (id, photo_path, type, color, season, usage, brand, size, favorite, dirty, wears_since_wash, total_wears, wash_after_wears, created_at) " +
                "VALUES (1, 'a.png', 'SHIRT', '#ffffff', 'ALL', 'CASUAL', 'Zara', 'M', 1, 0, 2, 5, NULL, 100)",
            "INSERT INTO Measurements (id, height_cm, bust_cm, waist_cm, hip_cm, shoulders_cm, arm_cm, leg_cm, updated_at) VALUES (1, 170.0, 90.0, 70.0, 95.0, 40.0, 60.0, 41.0, 200)",
            "INSERT INTO Outfit (id, name, occasion, favorite, created_at) VALUES (1, 'Look', NULL, 0, 150)",
            "INSERT INTO OutfitGarment (outfit_id, garment_id) VALUES (1, 1)",
            "INSERT INTO Trip (id, name, destination, start_date, end_date, created_at) VALUES (1, 'Roma', NULL, '2026-11-12', '2026-11-14', 160)",
            "INSERT INTO TripGarment (trip_id, garment_id, packed) VALUES (1, 1, 1)",
            "INSERT INTO CalendarDay (date, outfit_id, note) VALUES ('2026-11-05', 1, 'nota')",
        ).forEach { driver.execute(null, it, 0) }
    }

    private fun esquemaDe(driver: JdbcSqliteDriver): Esquema {
        val tablas = lista(driver, "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name")
            .associateWith { tabla ->
                driver.executeQuery(
                    null, "PRAGMA table_info($tabla)",
                    { c ->
                        val columnas = mutableSetOf<Columna>()
                        while (c.next().value) {
                            columnas += Columna(c.getString(1)!!, c.getString(2)!!, c.getLong(3) == 1L, c.getString(4), c.getLong(5)!!.toInt())
                        }
                        QueryResult.Value(columnas.toSet())
                    },
                    0,
                ).value
            }
        val indices = lista(driver, "SELECT name FROM sqlite_master WHERE type = 'index' AND name NOT LIKE 'sqlite_%'").toSet()
        return Esquema(tablas, indices)
    }

    private fun columnasEnOrden(driver: JdbcSqliteDriver, tabla: String): List<String> = lista(driver, "SELECT name FROM pragma_table_info('$tabla') ORDER BY cid")

    private fun lista(driver: JdbcSqliteDriver, sql: String): List<String> = driver.executeQuery(
        null, sql,
        { c ->
            val filas = mutableListOf<String>()
            while (c.next().value) filas += c.getString(0)!!
            QueryResult.Value(filas.toList())
        },
        0,
    ).value

    private companion object {
        val VERSIONES_ANTIGUAS = listOf(1L, 2L)

        /** Esquema original (commit b623b6d, T03): lo que tienen las instalaciones antiguas. */
        val DDL_V1 = listOf(
            "CREATE TABLE CalendarDay (date TEXT NOT NULL PRIMARY KEY, outfit_id INTEGER REFERENCES Outfit(id) ON DELETE SET NULL, note TEXT)",
            "CREATE INDEX calendar_day_outfit ON CalendarDay(outfit_id)",
            """CREATE TABLE Garment (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                photo_path TEXT NOT NULL,
                type TEXT NOT NULL,
                color TEXT NOT NULL,
                season TEXT NOT NULL,
                usage TEXT NOT NULL,
                brand TEXT,
                size TEXT,
                favorite INTEGER NOT NULL DEFAULT 0 CHECK (favorite IN (0, 1)),
                dirty INTEGER NOT NULL DEFAULT 0 CHECK (dirty IN (0, 1)),
                wears_since_wash INTEGER NOT NULL DEFAULT 0,
                total_wears INTEGER NOT NULL DEFAULT 0,
                wash_after_wears INTEGER,
                created_at INTEGER NOT NULL
            )""",
            "CREATE INDEX garment_type ON Garment(type)",
            "CREATE INDEX garment_color ON Garment(color)",
            "CREATE INDEX garment_season ON Garment(season)",
            """CREATE TABLE Measurements (
                id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
                height_cm REAL,
                bust_cm REAL,
                waist_cm REAL,
                hip_cm REAL,
                shoulders_cm REAL,
                arm_cm REAL,
                leg_cm REAL,
                updated_at INTEGER NOT NULL
            )""",
            """CREATE TABLE Outfit (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                occasion TEXT,
                favorite INTEGER NOT NULL DEFAULT 0 CHECK (favorite IN (0, 1)),
                created_at INTEGER NOT NULL
            )""",
            """CREATE TABLE OutfitGarment (
                outfit_id INTEGER NOT NULL REFERENCES Outfit(id) ON DELETE CASCADE,
                garment_id INTEGER NOT NULL REFERENCES Garment(id) ON DELETE CASCADE,
                x REAL NOT NULL DEFAULT 0,
                y REAL NOT NULL DEFAULT 0,
                scale REAL NOT NULL DEFAULT 1,
                z_index INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (outfit_id, garment_id)
            )""",
            "CREATE INDEX outfit_garment_garment ON OutfitGarment(garment_id)",
            """CREATE TABLE Trip (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                destination TEXT,
                start_date TEXT,
                end_date TEXT,
                created_at INTEGER NOT NULL
            )""",
            """CREATE TABLE TripOutfit (
                trip_id INTEGER NOT NULL REFERENCES Trip(id) ON DELETE CASCADE,
                outfit_id INTEGER NOT NULL REFERENCES Outfit(id) ON DELETE CASCADE,
                PRIMARY KEY (trip_id, outfit_id)
            )""",
            """CREATE TABLE TripGarment (
                trip_id INTEGER NOT NULL REFERENCES Trip(id) ON DELETE CASCADE,
                garment_id INTEGER NOT NULL REFERENCES Garment(id) ON DELETE CASCADE,
                packed INTEGER NOT NULL DEFAULT 0 CHECK (packed IN (0, 1)),
                PRIMARY KEY (trip_id, garment_id)
            )""",
        )

        const val ALTER_FIT = "ALTER TABLE Garment ADD COLUMN fit TEXT NOT NULL DEFAULT 'NORMAL' CHECK (fit IN ('SLIM', 'NORMAL', 'OVERSIZE'))"
        const val ALTER_FOOT = "ALTER TABLE Measurements ADD COLUMN foot_cm REAL"
        const val ALTER_MANUAL = "ALTER TABLE TripGarment ADD COLUMN manual INTEGER NOT NULL DEFAULT 0 CHECK (manual IN (0, 1))"

        val WEARLOG = listOf(
            "CREATE TABLE WearLog (date TEXT NOT NULL, garment_id INTEGER NOT NULL REFERENCES Garment(id) ON DELETE CASCADE, PRIMARY KEY (date, garment_id))",
            "CREATE INDEX wear_log_garment ON WearLog(garment_id)",
        )

        /** Lo que crea `1.sqm` (avatar 2D): la diferencia entre la v1 y la v2. */
        val AVATAR = listOf(
            """CREATE TABLE Avatar (
                id INTEGER NOT NULL PRIMARY KEY CHECK (id = 1),
                skin_rgb INTEGER NOT NULL,
                hair_rgb INTEGER NOT NULL,
                hair_style TEXT NOT NULL,
                face_photo_path TEXT,
                use_photo INTEGER NOT NULL DEFAULT 0 CHECK (use_photo IN (0, 1))
            )""",
        )
    }
}
