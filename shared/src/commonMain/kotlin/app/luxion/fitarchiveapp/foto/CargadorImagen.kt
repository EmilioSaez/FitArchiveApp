package app.luxion.fitarchiveapp.foto

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

/** Lado máximo al decodificar una foto a tamaño completo (detalle, avatar). */
const val LADO_COMPLETO = 1024

/** Lado máximo de las miniaturas de cuadrículas y listas: una celda de dos columnas no necesita más. */
const val LADO_MINIATURA = 512

/**
 * Lee y decodifica la foto guardada en [ruta], reducida si hace falta para que su lado mayor no pase de [ladoMaximo]
 * (aproximado: en Android se reduce por potencias de 2). Null si no existe o no se puede decodificar.
 */
expect suspend fun cargarImagen(ruta: String, ladoMaximo: Int = LADO_COMPLETO): ImageBitmap?

/** Memoria máxima de la caché de miniaturas. */
internal const val MEMORIA_MINIATURAS = 48L * 1024 * 1024

private val cacheMiniaturas = CacheLru<String, ImageBitmap>(MEMORIA_MINIATURAS) { it.width.toLong() * it.height * 4 }

/**
 * Miniatura de la foto ([LADO_MINIATURA]) con caché en memoria: al volver a una celda que salió de pantalla
 * no se vuelve a leer ni decodificar. Llamar desde el hilo de la interfaz (la caché no es concurrente).
 */
suspend fun cargarMiniatura(ruta: String): ImageBitmap? {
    cacheMiniaturas.get(ruta)?.let { return it }
    return cargarImagen(ruta, LADO_MINIATURA)?.also { cacheMiniaturas.put(ruta, it) }
}

/** Miniaturas de [rutas], cargadas bajo demanda y compartidas con las demás pantallas; se rellenan según llegan. */
@Composable
fun rememberMiniaturas(rutas: List<String>): Map<String, ImageBitmap> {
    val cargadas = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(rutas) {
        rutas.forEach { ruta -> if (ruta !in cargadas) cargarMiniatura(ruta)?.let { cargadas[ruta] = it } }
    }
    return cargadas
}

/** Caché LRU limitada por peso (bytes). No es segura entre hilos. */
internal class CacheLru<K, V : Any>(private val pesoMaximo: Long, private val peso: (V) -> Long) {
    private val mapa = LinkedHashMap<K, V>() // El orden de inserción es el de uso: al consultar se reinserta.
    private var total = 0L

    val tamano get() = mapa.size
    val pesoActual get() = total

    fun get(clave: K): V? {
        val v = mapa.remove(clave) ?: return null
        mapa[clave] = v
        return v
    }

    fun put(clave: K, valor: V) {
        mapa.remove(clave)?.let { total -= peso(it) }
        mapa[clave] = valor
        total += peso(valor)
        // Siempre se conserva la última, aunque por sí sola pase del máximo.
        while (total > pesoMaximo && mapa.size > 1) {
            val masAntigua = mapa.entries.iterator().next()
            total -= peso(masAntigua.value)
            mapa.remove(masAntigua.key)
        }
    }
}
