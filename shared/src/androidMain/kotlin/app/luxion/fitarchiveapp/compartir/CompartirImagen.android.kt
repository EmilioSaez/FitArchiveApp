package app.luxion.fitarchiveapp.compartir

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun rememberCompartirImagen(): CompartirImagen {
    val context = LocalContext.current
    return remember(context) { CompartirImagenAndroid(context) }
}

actual fun ImageBitmap.aPng(): ByteArray = ByteArrayOutputStream().also {
    asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
}.toByteArray()

/** Comparte con `Intent.ACTION_SEND`; el PNG vive en `cache/outfits/` (ver `file_paths.xml`) y se sirve con FileProvider. */
private class CompartirImagenAndroid(private val context: Context) : CompartirImagen {
    override suspend fun compartirPng(png: ByteArray, nombreArchivo: String) {
        val archivo = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "outfits").apply { mkdirs() }
            // Limpia exportaciones antiguas para no acumular imágenes en caché.
            dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > UN_DIA_MS }?.forEach { it.delete() }
            File(dir, nombreArchivo).apply { writeBytes(png) }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        val enviar = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val selector = Intent.createChooser(enviar, null)
        // Fuera de una Activity hace falta una tarea nueva.
        if (context !is android.app.Activity) selector.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(selector)
    }

    private companion object {
        const val UN_DIA_MS = 24L * 60 * 60 * 1000
    }
}
