package app.luxion.fitarchiveapp.compartir

import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.LIENZO_ANCHO
import app.luxion.fitarchiveapp.avatar.Rect

/** Tarjeta para compartir: formato vertical 9:16 (stories), 1080 x 1920 px. */
const val TARJETA_ANCHO = 1080
const val TARJETA_ALTO = 1920

/** Nombre que se muestra si el outfit aún no tiene. */
const val NOMBRE_OUTFIT_POR_DEFECTO = "Mi outfit"

const val MARCA = "FitArchive"

/** Reparto vertical de la tarjeta, en píxeles. */
data class DisposicionTarjeta(
    val ancho: Int,
    val alto: Int,
    /** Ancho máximo del título. */
    val anchoTitulo: Float,
    /** Borde superior del título. */
    val tituloY: Float,
    /** Filete fino bajo el título. */
    val filete: Rect,
    /** Zona del avatar (mantiene la proporción del lienzo). */
    val avatar: Rect,
    /** Centro vertical de la marca. */
    val marcaY: Float,
)

/** Calcula la [DisposicionTarjeta] de una tarjeta de [ancho] x [alto]: título arriba, avatar centrado, marca abajo. */
fun disposicionTarjeta(ancho: Int = TARJETA_ANCHO, alto: Int = TARJETA_ALTO): DisposicionTarjeta {
    val h = alto.toFloat()
    val w = ancho.toFloat()
    val avatarAlto = h * 0.70f
    val avatarAncho = avatarAlto * LIENZO_ANCHO / LIENZO_ALTO
    return DisposicionTarjeta(
        ancho = ancho,
        alto = alto,
        anchoTitulo = w * 0.84f,
        tituloY = h * 0.05f,
        filete = Rect(w * 0.42f, h * 0.135f, w * 0.16f, 3f),
        avatar = Rect((w - avatarAncho) / 2f, h * 0.15f, avatarAncho, avatarAlto),
        marcaY = h * 0.95f,
    )
}

/** Título de la tarjeta: el nombre sin espacios sobrantes o [NOMBRE_OUTFIT_POR_DEFECTO] si está vacío. */
fun tituloTarjeta(nombre: String): String = nombre.trim().ifEmpty { NOMBRE_OUTFIT_POR_DEFECTO }

private const val MAX_NOMBRE_ARCHIVO = 40

/** Nombre de archivo PNG seguro derivado del nombre del outfit (sin acentos ni símbolos). */
fun nombreArchivoOutfit(nombre: String): String {
    val limpio = buildString {
        for (c in nombre.trim().lowercase()) {
            val l = c.sinAcento()
            if (l in 'a'..'z' || l in '0'..'9') append(l) else if (isNotEmpty() && last() != '-') append('-')
        }
    }.trim('-').take(MAX_NOMBRE_ARCHIVO).trim('-')
    return "fitarchive-${limpio.ifEmpty { "outfit" }}.png"
}

private fun Char.sinAcento(): Char = when (this) {
    'á', 'à', 'ä', 'â' -> 'a'
    'é', 'è', 'ë', 'ê' -> 'e'
    'í', 'ì', 'ï', 'î' -> 'i'
    'ó', 'ò', 'ö', 'ô' -> 'o'
    'ú', 'ù', 'ü', 'û' -> 'u'
    'ñ' -> 'n'
    'ç' -> 'c'
    else -> this
}
