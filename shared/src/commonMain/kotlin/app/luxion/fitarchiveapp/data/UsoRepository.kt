package app.luxion.fitarchiveapp.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.luxion.fitarchiveapp.db.FitArchiveDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/** Historial de usos de las prendas (T33). Las fechas son ISO-8601 (yyyy-MM-dd). */
interface UsoRepository {
    /** Fechas en que se usó la prenda, la más reciente primero. */
    fun observeFechas(garmentId: Long): Flow<List<String>>

    /**
     * Cuenta los días del calendario anteriores a [hoy] con outfit asignado: suma un uso
     * a cada prenda del outfit y lo anota en el historial. Es idempotente: un día ya contado
     * no vuelve a sumar. Devuelve cuántos usos nuevos se han registrado.
     */
    suspend fun registrarHasta(hoy: String): Int
}

internal class SqlUsoRepository(
    private val db: FitArchiveDatabase,
    private val context: CoroutineContext = Dispatchers.Default,
) : UsoRepository {
    private val queries = db.wearLogQueries

    override fun observeFechas(garmentId: Long) =
        queries.selectDatesByGarment(garmentId).asFlow().mapToList(context)

    override suspend fun registrarHasta(hoy: String): Int = withContext(context) {
        db.transactionWithResult {
            val pendientes = queries.selectPending(hoy).executeAsList()
            pendientes.forEach {
                queries.insert(it.date, it.garment_id)
                db.garmentQueries.registrarUso(it.garment_id)
            }
            pendientes.size
        }
    }
}
