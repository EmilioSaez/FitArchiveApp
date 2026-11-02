package com.emilio.fitarchive.foto

import androidx.compose.runtime.Composable

/** Imagen obtenida de la cámara o la galería, codificada (JPEG/PNG) para el siguiente paso (T07/T08). */
class FotoCapturada(val bytes: ByteArray)

/** Abre la cámara o el selector de galería del sistema. El resultado llega por el callback de [rememberSelectorFoto]. */
interface SelectorFoto {
    fun tomarFoto()
    fun elegirDeGaleria()
}

/** Texto de justificación previa del permiso de cámara (docs/permisos.md). */
const val TEXTO_PERMISO_CAMARA =
    "Necesitamos la cámara para que fotografíes tus prendas. Las fotos se guardan solo en tu móvil."

/**
 * Crea un [SelectorFoto]. [onResultado] recibe la foto, o `null` si la usuaria cancela
 * o deniega el permiso de cámara (la galería sigue disponible).
 */
@Composable
expect fun rememberSelectorFoto(onResultado: (FotoCapturada?) -> Unit): SelectorFoto
