package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import app.luxion.fitarchiveapp.model.Measurements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

interface MeasurementsRepository {
    /** Emite null mientras no se hayan guardado medidas. */
    fun observe(): Flow<Measurements?>
    suspend fun get(): Measurements?

    /** Guarda las medidas, reemplazando las anteriores (solo hay un juego). Fija `updatedAt` con la hora actual e ignora el valor recibido. */
    suspend fun save(measurements: Measurements)
    suspend fun clear()
}

internal class SqlMeasurementsRepository(
    db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
    private val now: () -> Long,
) : MeasurementsRepository {
    private val queries = db.measurementsQueries

    override fun observe() = queries.select().asFlow().mapToOneOrNull(context).map { it?.toModel() }

    override suspend fun get() = withContext(context) { queries.select().executeAsOneOrNull()?.toModel() }

    override suspend fun save(measurements: Measurements) = withContext(context) {
        queries.upsert(
            height_cm = measurements.heightCm?.toDouble(),
            bust_cm = measurements.bustCm?.toDouble(),
            waist_cm = measurements.waistCm?.toDouble(),
            hip_cm = measurements.hipCm?.toDouble(),
            shoulders_cm = measurements.shouldersCm?.toDouble(),
            arm_cm = measurements.armCm?.toDouble(),
            leg_cm = measurements.legCm?.toDouble(),
            foot_cm = measurements.footCm?.toDouble(),
            updated_at = now(),
        )
        Unit
    }

    override suspend fun clear() = withContext(context) { queries.delete(); Unit }
}
