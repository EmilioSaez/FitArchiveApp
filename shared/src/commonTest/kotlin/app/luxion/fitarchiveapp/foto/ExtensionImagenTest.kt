package app.luxion.fitarchiveapp.foto

import kotlin.test.Test
import kotlin.test.assertEquals

class ExtensionImagenTest {
    private val cabeceraPng = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)
    private val cabeceraJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())

    @Test
    fun pngConAlfaSeGuardaComoPng() = assertEquals("png", extensionDeImagen(cabeceraPng))

    @Test
    fun jpegSeGuardaComoJpg() = assertEquals("jpg", extensionDeImagen(cabeceraJpeg))

    @Test
    fun desconocidoOVacioCaeEnJpg() {
        assertEquals("jpg", extensionDeImagen(byteArrayOf()))
        assertEquals("jpg", extensionDeImagen(byteArrayOf(1, 2, 3)))
        assertEquals("jpg", extensionDeImagen(cabeceraPng.copyOf(4)))
    }
}
