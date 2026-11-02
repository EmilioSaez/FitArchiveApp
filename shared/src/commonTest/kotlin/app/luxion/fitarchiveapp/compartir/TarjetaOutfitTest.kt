package app.luxion.fitarchiveapp.compartir

import app.luxion.fitarchiveapp.avatar.LIENZO_ALTO
import app.luxion.fitarchiveapp.avatar.LIENZO_ANCHO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TarjetaOutfitTest {
    @Test
    fun formatoVertical9a16() {
        assertEquals(9f / 16f, TARJETA_ANCHO.toFloat() / TARJETA_ALTO, 0.001f)
    }

    @Test
    fun avatarCentradoYDentroDeLaTarjeta() {
        val d = disposicionTarjeta()
        val a = d.avatar
        assertEquals((TARJETA_ANCHO - (a.x + a.w)), a.x, 0.01f)
        assertTrue(a.x >= 0f && a.x + a.w <= TARJETA_ANCHO)
        assertTrue(a.y >= 0f && a.y + a.h <= TARJETA_ALTO)
    }

    @Test
    fun avatarMantieneLaProporcionDelLienzo() {
        val a = disposicionTarjeta().avatar
        assertEquals(LIENZO_ANCHO / LIENZO_ALTO, a.w / a.h, 0.001f)
    }

    @Test
    fun tituloFileteAvatarYMarcaVanEnOrdenVertical() {
        val d = disposicionTarjeta()
        assertTrue(d.tituloY < d.filete.y)
        assertTrue(d.filete.y < d.avatar.y)
        assertTrue(d.avatar.y + d.avatar.h < d.marcaY)
        assertTrue(d.marcaY < TARJETA_ALTO)
    }

    @Test
    fun tituloUsaNombreORespaldo() {
        assertEquals("Look de lunes", tituloTarjeta("  Look de lunes "))
        assertEquals(NOMBRE_OUTFIT_POR_DEFECTO, tituloTarjeta(""))
        assertEquals(NOMBRE_OUTFIT_POR_DEFECTO, tituloTarjeta("   "))
    }

    @Test
    fun nombreDeArchivoSeguro() {
        assertEquals("fitarchive-cena-de-viernes.png", nombreArchivoOutfit("Cena de Viernes"))
        assertEquals("fitarchive-camiseta-y-vaqueros.png", nombreArchivoOutfit("  Camiseta y vaqueros!! "))
        assertEquals("fitarchive-nino-cafe.png", nombreArchivoOutfit("Niño/Café"))
        assertEquals("fitarchive-outfit.png", nombreArchivoOutfit(""))
        assertEquals("fitarchive-outfit.png", nombreArchivoOutfit("¡¡¿?!!"))
        assertEquals("fitarchive-outfit.png", nombreArchivoOutfit("///"))
    }

    @Test
    fun nombreDeArchivoNoContieneSeparadoresYSeAcorta() {
        val n = nombreArchivoOutfit("../../etc/passwd " + "x".repeat(100))
        assertTrue('/' !in n && '\\' !in n && ".." !in n.removeSuffix(".png"))
        assertTrue(n.length <= "fitarchive-".length + 40 + ".png".length)
        assertTrue(n.endsWith(".png"))
    }
}
