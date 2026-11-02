package app.luxion.fitarchiveapp.backup

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidPhotoStoreTest {
    private val dir: File = Files.createTempDirectory("photos").toFile()
    private var clock = 1L
    private val store = AndroidPhotoStore(dir, now = { clock++ })

    @AfterTest
    fun cleanup() {
        dir.deleteRecursively()
    }

    @Test
    fun saveThenReadReturnsSameBytes() = runTest {
        val bytes = byteArrayOf(1, 2, 3, -1, 0)
        val path = store.save(7, bytes)
        assertTrue(File(path).isAbsolute.not())
        assertContentEquals(bytes, store.read(path))
        assertTrue(File(dir, path).isFile)
    }

    @Test
    fun saveTwiceKeepsBothFilesUntilDeleted() = runTest {
        val first = store.save(7, byteArrayOf(1))
        val second = store.save(7, byteArrayOf(2))
        assertNotEquals(first, second)
        assertContentEquals(byteArrayOf(1), store.read(first))
        store.delete(first)
        assertNull(store.read(first))
        assertContentEquals(byteArrayOf(2), store.read(second))
    }

    @Test
    fun missingFileReadsAsNullAndDeleteIsHarmless() = runTest {
        assertNull(store.read("fotos/nada.jpg"))
        store.delete("fotos/nada.jpg")
    }

    @Test
    fun absolutePathsAreAccepted() = runTest {
        val file = File(dir, "ext.jpg").apply { writeBytes(byteArrayOf(9)) }
        assertContentEquals(byteArrayOf(9), store.read(file.absolutePath))
        store.delete(file.absolutePath)
        assertTrue(!file.exists())
    }

    @Test
    fun extensionFollowsRealImageFormat() = runTest {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        assertTrue(store.save(1, png).endsWith(".png"))
        assertTrue(store.save(1, jpeg).endsWith(".jpg"))
    }

    @Test
    fun legacyJpgPathWithPngContentStillReads() = runTest {
        // Fotos guardadas antes del cambio: PNG con alfa dentro de prenda_<uuid>.jpg.
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1)
        val legacy = File(dir, "prendas/prenda_abc.jpg").apply { parentFile!!.mkdirs(); writeBytes(png) }
        assertContentEquals(png, store.read(legacy.absolutePath))
        // Y al restaurarla en una copia se guarda con la extensión correcta.
        assertTrue(store.save(2, store.read(legacy.absolutePath)!!).endsWith(".png"))
    }

    @Test
    fun noTempFilesLeftBehind() = runTest {
        store.save(1, byteArrayOf(1))
        assertTrue(File(dir, "fotos").listFiles()!!.none { it.name.endsWith(".tmp") })
    }
}
