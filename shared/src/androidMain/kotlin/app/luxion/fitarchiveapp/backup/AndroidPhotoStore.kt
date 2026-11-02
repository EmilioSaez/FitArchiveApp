package app.luxion.fitarchiveapp.backup

import app.luxion.fitarchiveapp.foto.extensionDeImagen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.time.Clock

/**
 * Fotos en el almacenamiento privado de la app ([baseDir], normalmente `context.filesDir`).
 *
 * Las rutas que devuelve [save] son relativas a [baseDir] (`fotos/prenda_<id>_<ms>.<ext>`, con la extensión del formato real: png o jpg);
 * [read] y [delete] aceptan también rutas absolutas.
 */
class AndroidPhotoStore(
    private val baseDir: File,
    private val context: CoroutineContext = Dispatchers.IO,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : PhotoStore {

    private fun resolve(path: String): File = File(path).let { if (it.isAbsolute) it else File(baseDir, path) }

    override suspend fun read(path: String): ByteArray? = withContext(context) {
        resolve(path).takeIf { it.isFile }?.let { runCatching { it.readBytes() }.getOrNull() }
    }

    override suspend fun save(garmentId: Long, bytes: ByteArray): String = withContext(context) {
        // Nombre único: no pisa la foto anterior de la prenda hasta que la importación se confirma.
        val relative = "$PHOTOS_DIR/prenda_${garmentId}_${now()}.${extensionDeImagen(bytes)}"
        val target = File(baseDir, relative)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(target)) error("No se pudo guardar la foto de la prenda $garmentId")
        } finally {
            tmp.delete()
        }
        relative
    }

    override suspend fun delete(path: String) {
        withContext(context) { resolve(path).delete() }
    }

    private companion object {
        const val PHOTOS_DIR = "fotos"
    }
}
