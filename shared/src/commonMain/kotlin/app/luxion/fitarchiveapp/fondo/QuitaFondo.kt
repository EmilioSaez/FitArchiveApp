package app.luxion.fitarchiveapp.fondo

import app.luxion.fitarchiveapp.foto.FotoCapturada

/** Prenda recortada: PNG con canal alfa (fondo transparente). */
class ImagenSinFondo(val bytesPng: ByteArray, val ancho: Int, val alto: Int)

/** Fallo al quitar el fondo. [motivo] permite a la UI decidir el mensaje. */
class QuitaFondoException(val motivo: Motivo, mensaje: String, causa: Throwable? = null) :
    Exception(mensaje, causa) {
    enum class Motivo {
        /** Los bytes no son una imagen legible. */
        IMAGEN_INVALIDA,

        /** No se detectó ningún objeto en primer plano. */
        SIN_SUJETO,

        /** El sistema no puede segmentar (versión del SO, modelo sin descargar…). */
        NO_DISPONIBLE,
    }
}

/** Quita el fondo de una foto en el propio móvil, sin red ni APIs de pago. */
interface QuitaFondo {
    /** @throws QuitaFondoException si no se puede recortar. */
    suspend fun quitar(foto: FotoCapturada): ImagenSinFondo
}

/** Implementación de la plataforma: ML Kit Subject Segmentation (Android), Vision (iOS). */
expect fun crearQuitaFondo(): QuitaFondo
