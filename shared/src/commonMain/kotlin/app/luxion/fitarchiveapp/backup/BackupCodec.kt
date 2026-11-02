package app.luxion.fitarchiveapp.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Error al leer o validar un archivo de copia. */
sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No es un archivo de copia de FitArchive. */
    class NotABackup : BackupException("El archivo no es una copia de FitArchive")

    /** Archivo truncado, manifiesto ilegible o referencias rotas. */
    class Corrupt(detail: String, cause: Throwable? = null) : BackupException("Copia dañada: $detail", cause)

    /** Creado por una versión más nueva de la app. */
    class UnsupportedVersion(val found: Int) :
        BackupException("La copia es de una versión más nueva ($found); actualiza la app")
}

/** Manifiesto ya leído más los bytes de cada foto (misma posición que `manifest.photos`). */
class BackupContent(val manifest: BackupManifest, val photos: List<ByteArray>)

/**
 * Contenedor binario de un solo archivo (extensión sugerida `.fitarchive`):
 *
 * ```
 * "FITARCH" + 0x01  8 bytes: magia
 * u32 BE            versión del formato (= formatVersion)
 * u32 BE            longitud N del manifiesto
 * N bytes           manifiesto JSON UTF-8 (BackupManifest)
 * ...               bytes de cada foto, en el orden de manifest.photos
 * ```
 * Sin compresión: las fotos ya son JPEG/PNG.
 */
object BackupCodec {
    private val MAGIC = "FITARCH".encodeToByteArray() + byteArrayOf(1)
    private const val HEADER = 16
    private const val MAX_MANIFEST = 64 * 1024 * 1024

    val json = Json {
        ignoreUnknownKeys = true // campos añadidos por versiones menores
        encodeDefaults = true
    }

    fun encode(manifest: BackupManifest, photos: List<ByteArray>): ByteArray {
        require(manifest.photos.size == photos.size) { "photos no coincide con manifest.photos" }
        manifest.photos.forEachIndexed { i, p -> require(p.size == photos[i].size) { "tamaño de foto incorrecto" } }
        val body = json.encodeToString(BackupManifest.serializer(), manifest).encodeToByteArray()
        val out = ByteArray(HEADER + body.size + photos.sumOf { it.size })
        MAGIC.copyInto(out)
        putInt(out, 8, manifest.formatVersion)
        putInt(out, 12, body.size)
        body.copyInto(out, HEADER)
        var pos = HEADER + body.size
        for (p in photos) {
            p.copyInto(out, pos)
            pos += p.size
        }
        return out
    }

    fun decode(data: ByteArray): BackupContent {
        if (data.size < HEADER || !MAGIC.contentEquals(data.copyOfRange(0, 8))) throw BackupException.NotABackup()
        val version = getInt(data, 8)
        if (version > BackupManifest.FORMAT_VERSION) throw BackupException.UnsupportedVersion(version)
        if (version < 1) throw BackupException.Corrupt("versión $version")
        val len = getInt(data, 12)
        if (len < 0 || len > MAX_MANIFEST || HEADER.toLong() + len > data.size) throw BackupException.Corrupt("manifiesto truncado")
        val manifest = try {
            json.decodeFromString(BackupManifest.serializer(), data.decodeToString(HEADER, HEADER + len))
        } catch (e: SerializationException) {
            throw BackupException.Corrupt("manifiesto ilegible", e)
        } catch (e: IllegalArgumentException) {
            throw BackupException.Corrupt("manifiesto ilegible", e)
        }
        if (manifest.formatVersion != version) throw BackupException.Corrupt("versiones incoherentes")
        var pos = HEADER + len
        val photos = manifest.photos.map { entry ->
            if (entry.size < 0 || pos.toLong() + entry.size > data.size) throw BackupException.Corrupt("fotos truncadas")
            data.copyOfRange(pos, pos + entry.size).also { pos += entry.size }
        }
        if (pos != data.size) throw BackupException.Corrupt("bytes sobrantes")
        return BackupContent(manifest, photos)
    }

    private fun putInt(b: ByteArray, at: Int, v: Int) {
        for (i in 0..3) b[at + i] = (v ushr (24 - 8 * i)).toByte()
    }

    private fun getInt(b: ByteArray, at: Int): Int =
        (0..3).fold(0) { acc, i -> (acc shl 8) or (b[at + i].toInt() and 0xFF) }
}
