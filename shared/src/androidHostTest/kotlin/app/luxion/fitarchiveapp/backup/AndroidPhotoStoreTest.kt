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
    fun noTempFilesLeftBehind() = runTest {
        store.save(1, byteArrayOf(1))
        assertTrue(File(dir, "fotos").listFiles()!!.none { it.name.endsWith(".tmp") })
    }
}
