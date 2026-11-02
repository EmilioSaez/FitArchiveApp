package app.luxion.fitarchiveapp.backup

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.posix.memcpy
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

/**
 * Fotos en el directorio Documents de la app ([baseDir]).
 *
 * Las rutas que devuelve [save] son relativas a [baseDir] (`fotos/prenda_<id>_<ms>.jpg`):
 * la ruta absoluta del contenedor de iOS cambia entre instalaciones/actualizaciones.
 * [read] y [delete] aceptan también rutas absolutas.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosPhotoStore(
    private val baseDir: String = documentsDirectory(),
    private val context: CoroutineContext = Dispatchers.Default,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : PhotoStore {
    private val files = NSFileManager.defaultManager

    private fun resolve(path: String) = if (path.startsWith("/")) path else "$baseDir/$path"

    override suspend fun read(path: String): ByteArray? = withContext(context) {
        files.contentsAtPath(resolve(path))?.toByteArray()
    }

    override suspend fun save(garmentId: Long, bytes: ByteArray): String = withContext(context) {
        val relative = "$PHOTOS_DIR/prenda_${garmentId}_${now()}.jpg"
        files.createDirectoryAtPath("$baseDir/$PHOTOS_DIR", withIntermediateDirectories = true, attributes = null, error = null)
        val ok = files.createFileAtPath(resolve(relative), contents = bytes.toNSData(), attributes = null)
        if (!ok) error("No se pudo guardar la foto de la prenda $garmentId")
        relative
    }

    override suspend fun delete(path: String) {
        withContext(context) { files.removeItemAtPath(resolve(path), error = null) }
    }

    private companion object {
        const val PHOTOS_DIR = "fotos"
    }
}

private fun documentsDirectory(): String =
    NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { destino ->
    if (destino.isNotEmpty()) destino.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    if (isEmpty()) NSData() else usePinned { NSData.create(bytes = it.addressOf(0), length = size.toULong()) }
