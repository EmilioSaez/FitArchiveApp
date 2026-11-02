package app.luxion.fitarchiveapp.fondo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import app.luxion.fitarchiveapp.foto.FotoCapturada
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal fun modeloDescargandose(e: Throwable): Boolean =
    e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE

/** Ejecuta [bloque]; si falla con un error reintentable, espera [espera] ms y repite hasta [maximo] intentos. */
internal suspend fun <T> conReintentos(
    maximo: Int = 45,
    espera: Long = 2_000,
    esReintentable: (Throwable) -> Boolean,
    bloque: suspend () -> T,
): T {
    var intento = 1
    while (true) {
        try {
            return bloque()
        } catch (e: Exception) {
            if (intento >= maximo || !esReintentable(e)) throw e
        }
        intento++
        delay(espera)
    }
}

actual fun crearQuitaFondo(): QuitaFondo = QuitaFondoMlKit()

/**
 * ML Kit Subject Segmentation: se ejecuta en el móvil. El modelo lo gestiona Google Play services
 * y se descarga una vez (meta-data `com.google.mlkit.vision.DEPENDENCIES` en el manifiesto);
 * después funciona sin red.
 */
private class QuitaFondoMlKit : QuitaFondo {
    private val segmentador by lazy {
        SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundBitmap().build(),
        )
    }

    override suspend fun quitar(foto: FotoCapturada): ImagenSinFondo {
        val entrada = withContext(Dispatchers.Default) { decodificar(foto.bytes) }
        val recorte = try {
            // La primera vez el modelo se descarga en segundo plano: ML Kit falla con UNAVAILABLE
            // hasta que termina; reintentamos en vez de guardar la foto sin recortar.
            val resultado = conReintentos(esReintentable = ::modeloDescargandose) {
                suspendCancellableCoroutine { cont ->
                    segmentador.process(InputImage.fromBitmap(entrada, 0))
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resumeWithException(it) }
                }
            }
            resultado.foregroundBitmap
        } catch (e: Exception) {
            throw QuitaFondoException(
                QuitaFondoException.Motivo.NO_DISPONIBLE,
                "ML Kit no pudo segmentar la imagen",
                e,
            )
        } ?: throw QuitaFondoException(QuitaFondoException.Motivo.SIN_SUJETO, "No se detectó ninguna prenda")
        return withContext(Dispatchers.Default) { aPng(recorte) }
    }

    private fun decodificar(bytes: ByteArray): Bitmap {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        if (limites.outWidth <= 0 || limites.outHeight <= 0) throw imagenInvalida()
        val (an, al) = Geometria.ajustar(limites.outWidth, limites.outHeight)
        var muestra = 1
        while (limites.outWidth / (muestra * 2) >= an && limites.outHeight / (muestra * 2) >= al) muestra *= 2
        val opciones = BitmapFactory.Options().apply { inSampleSize = muestra }
        var bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones) ?: throw imagenInvalida()
        if (bmp.width != an || bmp.height != al) {
            bmp = Bitmap.createScaledBitmap(bmp, an, al, true)
        }
        return aplicarOrientacion(bmp, bytes)
    }

    /** `BitmapFactory` ignora EXIF: las fotos de cámara saldrían giradas. */
    private fun aplicarOrientacion(bmp: Bitmap, bytes: ByteArray): Bitmap {
        val orientacion = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val m = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                m.postRotate(90f)
                m.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                m.postRotate(270f)
                m.postScale(-1f, 1f)
            }
            else -> return bmp
        }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
    }

    /** Recorta al sujeto (con margen) y codifica como PNG con alfa. */
    private fun aPng(bmp: Bitmap): ImagenSinFondo {
        val pixeles = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixeles, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val caja = Geometria.cajaOpaca(pixeles, bmp.width, bmp.height)
            ?: throw QuitaFondoException(QuitaFondoException.Motivo.SIN_SUJETO, "No se detectó ninguna prenda")
        val c = Geometria.conMargen(caja, margen = 8, ancho = bmp.width, alto = bmp.height)
        val salida = Bitmap.createBitmap(bmp, c.izq, c.arr, c.ancho, c.alto)
        val flujo = ByteArrayOutputStream()
        salida.compress(Bitmap.CompressFormat.PNG, 100, flujo)
        return ImagenSinFondo(flujo.toByteArray(), salida.width, salida.height)
    }

    private fun imagenInvalida() =
        QuitaFondoException(QuitaFondoException.Motivo.IMAGEN_INVALIDA, "No se pudo leer la imagen")
}
