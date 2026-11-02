package app.luxion.fitarchiveapp.weather

/** Persistencia simple de la ciudad elegida a mano (SharedPreferences / NSUserDefaults). */
interface AlmacenCiudad {
    fun cargar(): Ciudad?
    fun guardar(ciudad: Ciudad?)
}

/** Texto de justificación previa del permiso de ubicación (docs/permisos.md). */
const val TEXTO_PERMISO_UBICACION =
    "Usamos tu ubicación aproximada solo para saber el tiempo y sugerirte outfits. Si prefieres, elige una ciudad."

/** Nombre legible: "Madrid, España". */
fun Ciudad.etiqueta(): String = if (pais.isNullOrBlank()) nombre else "$nombre, $pais"

/** Lógica de la elección manual de ciudad: búsqueda con el servicio y ciudad guardada para la próxima vez. */
class SeleccionCiudad(
    private val servicio: ServicioTiempo,
    private val almacen: AlmacenCiudad,
) {
    fun ciudadGuardada(): Ciudad? = almacen.cargar()

    /** Busca ciudades; con menos de [MIN_CARACTERES] letras no consulta la red. Sin duplicados. */
    suspend fun buscar(texto: String): Result<List<Ciudad>> {
        val t = texto.trim()
        if (t.length < MIN_CARACTERES) return Result.success(emptyList())
        return servicio.buscarCiudades(t).map { it.distinct() }
    }

    fun elegir(ciudad: Ciudad) = almacen.guardar(ciudad)

    fun quitar() = almacen.guardar(null)

    companion object {
        const val MIN_CARACTERES = 2
    }
}

/** Serialización de [Ciudad] a una sola cadena para el almacén. */
internal object CiudadCodec {
    private const val SEP = '\u001F'

    fun codificar(c: Ciudad): String =
        listOf(c.coordenadas.latitud, c.coordenadas.longitud, c.pais.orEmpty(), c.nombre).joinToString(SEP.toString())

    fun decodificar(texto: String?): Ciudad? {
        if (texto.isNullOrEmpty()) return null
        val p = texto.split(SEP, limit = 4)
        if (p.size != 4) return null
        val lat = p[0].toDoubleOrNull() ?: return null
        val lon = p[1].toDoubleOrNull() ?: return null
        if (p[3].isBlank()) return null
        return Ciudad(p[3], p[2].ifEmpty { null }, Coordenadas(lat, lon))
    }
}

/** Ubicación del móvil con gestión del permiso. [ubicacionAproximada] nunca pide el permiso: eso lo hace [solicitarPermiso]. */
interface ProveedorUbicacionPermiso : ProveedorUbicacion {
    fun tienePermiso(): Boolean

    /** Pide el permiso (en Android, tras un diálogo de justificación propio). [alResultado] recibe si quedó concedido. */
    fun solicitarPermiso(alResultado: (Boolean) -> Unit)
}
