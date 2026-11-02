package com.emilio.fitarchive.data

import com.emilio.fitarchive.db.FitArchiveDatabase

/** BD en memoria con el esquema creado y claves foráneas activas. */
expect fun createTestDatabase(): FitArchiveDatabase
