package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.model.Measurements

/**
 * Acceso a las medidas de la usuaria. La implementación real llegará con la
 * capa de datos (T04) y debe fijar [Measurements.updatedAt] al guardar.
 */
interface MedidasRepository {
    fun cargar(): Measurements?
    fun guardar(medidas: Measurements)
}

/** Implementación provisional en memoria hasta que exista la de T04. */
class MedidasRepositoryEnMemoria : MedidasRepository {
    private var actual: Measurements? = null
    override fun cargar(): Measurements? = actual
    override fun guardar(medidas: Measurements) {
        actual = medidas
    }
}
