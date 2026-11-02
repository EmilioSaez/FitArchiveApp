package app.luxion.fitarchiveapp.foto

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual suspend fun cargarImagen(ruta: String): ImageBitmap? = withContext(Dispatchers.Default) {
    runCatching {
        val datos = NSData.dataWithContentsOfFile(ruta) ?: return@runCatching null
        val bytes = ByteArray(datos.length.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), datos.bytes, datos.length) }
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.getOrNull()
}
