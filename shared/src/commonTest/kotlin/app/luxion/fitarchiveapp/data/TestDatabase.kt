package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.db.FitArchiveDatabase

/** BD en memoria con el esquema creado y claves foráneas activas. */
expect fun createTestDatabase(): FitArchiveDatabase
