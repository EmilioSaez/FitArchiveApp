package app.luxion.fitarchiveapp.foto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

actual suspend fun reducirParaIa(bytes: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, medidas)
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return@runCatching null
        var muestreo = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / (muestreo * 2) >= LADO_IA) muestreo *= 2
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestreo })
            ?: return@runCatching null

        // El EXIF se descarta al recodificar, así que la orientación se aplica a los píxeles antes.
        val orientacion = runCatching {
            ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matriz.postRotate(90f); matriz.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matriz.postRotate(270f); matriz.postScale(-1f, 1f) }
        }
        val orientada = if (matriz.isIdentity) original
        else Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)

        val (ancho, alto) = dimensionesReducidas(orientada.width, orientada.height)
        val final = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val lienzo = Canvas(final)
        lienzo.drawColor(Color.WHITE) // JPEG no tiene transparencia: una foto sin fondo (PNG) quedaría negra.
        lienzo.drawBitmap(orientada, null, android.graphics.Rect(0, 0, ancho, alto), Paint(Paint.FILTER_BITMAP_FLAG))

        ByteArrayOutputStream().also { final.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG_IA, it) }.toByteArray()
    }.getOrNull()
}
