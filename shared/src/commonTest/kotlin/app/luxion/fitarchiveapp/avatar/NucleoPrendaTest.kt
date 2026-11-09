package app.luxion.fitarchiveapp.avatar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NucleoPrendaTest {
    /** Máscara [ancho] x [alto] con los rectángulos opacos dados como (colIni, filaIni, colFin, filaFin). */
    private fun mascara(ancho: Int, alto: Int, vararg rects: List<Int>): ByteArray {
        val m = ByteArray(ancho * alto)
        rects.forEach { (c0, f0, c1, f1) ->
            for (f in f0 until f1) for (c in c0 until c1) m[f * ancho + c] = 255.toByte()
        }
        return m
    }

    // Top de 40 x 100: cuerpo de 40 de ancho entre las filas 40 y 100, con una tira de 2 px que sube hasta la fila 0.
    private val ancho = 40
    private val alto = 100
    private val topConTira = mascara(ancho, alto, listOf(0, 40, 40, 100), listOf(19, 0, 21, 40))
    private val topSinTira = mascara(ancho, alto, listOf(0, 40, 40, 100))

    @Test
    fun laTiraFinaQuedaFueraDelNucleo() {
        assertEquals(Nucleo(0, 40, 40, 100), nucleoPrenda(topConTira, ancho, alto))
    }

    @Test
    fun sinTiraElNucleoEsElBboxCompleto() {
        assertEquals(Nucleo(0, 40, 40, 100), nucleoPrenda(topSinTira, ancho, alto))
        val lleno = mascara(ancho, alto, listOf(0, 0, 40, 100))
        assertEquals(Nucleo(0, 0, 40, 100), nucleoPrenda(lleno, ancho, alto))
    }

    @Test
    fun unLazoFinoPorDebajoQuedaFuera() {
        val conLazo = mascara(60, 70, listOf(10, 0, 50, 60), listOf(25, 60, 27, 70))
        assertEquals(Nucleo(10, 0, 50, 60), nucleoPrenda(conLazo, 60, 70))
    }

    @Test
    fun hombrosSeparadosPorElEscoteEntranEnElNucleo() {
        // Dos hombros de 15 px con escote de 10 en medio (30 de 40 = 75 %) por encima del cuerpo.
        val m = mascara(40, 60, listOf(0, 0, 15, 20), listOf(25, 0, 40, 20), listOf(0, 20, 40, 60))
        assertEquals(Nucleo(0, 0, 40, 60), nucleoPrenda(m, 40, 60))
    }

    @Test
    fun elUmbralSeAjusta() {
        // Filas de 40 px (cuerpo) y de 12 px (30 %) por arriba.
        val m = mascara(40, 60, listOf(10, 0, 22, 20), listOf(0, 20, 40, 60))
        assertEquals(Nucleo(0, 0, 40, 60), nucleoPrenda(m, 40, 60, umbral = 0.25f))
        assertEquals(Nucleo(0, 20, 40, 60), nucleoPrenda(m, 40, 60, umbral = 0.5f))
    }

    @Test
    fun elHaloTranslucidoNoCuenta() {
        val m = topSinTira.copyOf()
        for (c in 0 until ancho) m[c] = 10 // fila 0 casi transparente
        assertEquals(Nucleo(0, 40, 40, 100), nucleoPrenda(m, ancho, alto))
    }

    @Test
    fun mascaraVaciaDevuelveNull() {
        assertNull(nucleoPrenda(ByteArray(40 * 60), 40, 60))
    }

    @Test
    fun dimensionesInvalidasDevuelvenNull() {
        assertNull(nucleoPrenda(ByteArray(0), 0, 0))
        assertNull(nucleoPrenda(ByteArray(10), 40, 60))
    }

    @Test
    fun unaSolaFila() {
        val m = mascara(10, 1, listOf(2, 0, 8, 1))
        assertEquals(Nucleo(2, 0, 8, 1), nucleoPrenda(m, 10, 1))
    }

    @Test
    fun unSoloPixel() {
        val m = mascara(5, 5, listOf(3, 2, 4, 3))
        val n = assertNotNull(nucleoPrenda(m, 5, 5))
        assertEquals(Nucleo(3, 2, 4, 3), n)
    }

    // --- encajar con núcleo ---

    private val caja = Caja(100f, 200f, 80f, 120f)

    @Test
    fun encajarConElBboxEnteroComoNucleoEsLaReglaDeSiempre() {
        val entero = Nucleo(0, 0, ancho, alto)
        Alineacion.entries.forEach {
            assertEquals(encajar(caja, ancho, alto, it), encajar(caja, ancho, alto, it, entero))
            assertEquals(encajar(caja, ancho, alto, it), encajar(caja, ancho, alto, it, null))
        }
    }

    @Test
    fun encajarSinTiraNoCambiaNadaRespectoAlRecorteAjustado() {
        // Sin tira el recorte "real" empieza en la fila 40: coincide con encajar la imagen ya recortada a 40 x 60.
        val nucleo = nucleoPrenda(topSinTira, ancho, alto)
        val recortada = encajar(caja, 40, 60, Alineacion.ARRIBA_CENTRO)
        val r = encajar(caja, ancho, alto, Alineacion.ARRIBA_CENTRO, nucleo)
        assertEquals(recortada.w, r.w) // misma escala (2)
        assertEquals(recortada.y, r.y + 40 * (r.w / ancho))
        assertEquals(recortada.x, r.x)
    }

    @Test
    fun encajarConTiraEscalaYAlineaPorElNucleo() {
        val r = encajar(caja, ancho, alto, Alineacion.ARRIBA_CENTRO, nucleoPrenda(topConTira, ancho, alto))
        val s = r.w / ancho
        assertEquals(2f, s) // el núcleo (40 x 60) llena la caja de 80 x 120
        assertEquals(caja.y, r.y + 40 * s) // el borde superior del cuerpo, no el de la tira, toca arriba de la caja
        assertEquals(caja.y - 80f, r.y) // la tira sobresale por encima de la caja
        assertEquals(caja.x, r.x)
        // Antes (bbox entero) la tira mandaba: escala menor y cuerpo descolocado.
        val antiguo = encajar(caja, ancho, alto, Alineacion.ARRIBA_CENTRO)
        assertEquals(1.2f, antiguo.w / ancho)
    }

    @Test
    fun encajarNucleoDegeneradoCaeAlBboxEntero() {
        assertEquals(
            encajar(caja, 10, 10, Alineacion.CENTRO),
            encajar(caja, 10, 10, Alineacion.CENTRO, Nucleo(3, 3, 3, 3)),
        )
    }

    @Test
    fun encajarNucleoDescentradoCentraElNucleo() {
        // Núcleo de columnas 0..20 en una imagen de 40 de ancho: es el núcleo el que se centra en la caja.
        val r = encajar(Caja(0f, 0f, 40f, 40f), 40, 20, Alineacion.ARRIBA_CENTRO, Nucleo(0, 0, 20, 20))
        assertEquals(2f, r.w / 40)
        assertEquals(0f, r.x) // núcleo (20 x 2 = 40) llena la caja; empieza en x = 0
    }
}
