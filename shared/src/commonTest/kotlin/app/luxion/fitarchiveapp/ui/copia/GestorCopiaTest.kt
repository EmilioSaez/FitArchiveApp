package app.luxion.fitarchiveapp.ui.copia

import app.luxion.fitarchiveapp.backup.BackupCodec
import app.luxion.fitarchiveapp.backup.BackupManifest
import app.luxion.fitarchiveapp.backup.BackupService
import app.luxion.fitarchiveapp.backup.PhotoStore
import app.luxion.fitarchiveapp.backup.RestoreResult
import app.luxion.fitarchiveapp.backup.WardrobeBackupStore
import app.luxion.fitarchiveapp.backup.WardrobeSnapshot
import app.luxion.fitarchiveapp.model.Fit
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Almacen(var datos: WardrobeSnapshot = WardrobeSnapshot(), var falla: Boolean = false) : WardrobeBackupStore {
    override suspend fun readAll(): WardrobeSnapshot {
        if (falla) error("fallo de BD")
        return datos
    }

    override suspend fun replaceAll(snapshot: WardrobeSnapshot) {
        if (falla) error("fallo de BD")
        datos = snapshot
    }
}

private class Fotos(val archivos: MutableMap<String, ByteArray> = mutableMapOf()) : PhotoStore {
    override suspend fun read(path: String) = archivos[path]
    override suspend fun save(garmentId: Long, bytes: ByteArray) = "fotos/$garmentId.jpg".also { archivos[it] = bytes }
    override suspend fun delete(path: String) {
        archivos.remove(path)
    }
}

private fun prenda(id: Long, foto: String) = Garment(
    id, foto, GarmentType.DRESS, GarmentColor.BURGUNDY, Season.WINTER, Usage.PARTY, null, "M",
    favorite = false, dirty = false, wearsSinceWash = 0, totalWears = 0, washAfterWears = 3, createdAt = id, fit = Fit.NORMAL,
)

private fun armarioConDosPrendas() = WardrobeSnapshot(
    garments = listOf(prenda(1, "a.jpg"), prenda(2, "b.jpg")),
    outfits = listOf(Outfit(10, "Cena", null, false, 5)),
)

class GestorCopiaTest {
    private fun gestor(almacen: Almacen = Almacen(), fotos: Fotos = Fotos()) =
        GestorCopia(BackupService(almacen, fotos) { 0L })

    private suspend fun copiaDe(datos: WardrobeSnapshot, fotos: Map<String, ByteArray> = emptyMap()): ByteArray =
        BackupService(Almacen(datos), Fotos(fotos.toMutableMap())) { 0L }.export()

    @Test
    fun empiezaInactivo() {
        assertEquals(EstadoCopia.Inactivo, gestor().estado.value)
    }

    @Test
    fun prepararExportacionDevuelveUnaCopiaValidaYVuelveAInactivo() = runTest {
        val g = gestor(Almacen(armarioConDosPrendas()), Fotos(mutableMapOf("a.jpg" to byteArrayOf(1, 2), "b.jpg" to byteArrayOf(3))))

        val bytes = g.prepararExportacion()

        assertEquals(EstadoCopia.Inactivo, g.estado.value)
        val contenido = BackupCodec.decode(checkNotNull(bytes))
        assertEquals(2, contenido.manifest.garments.size)
        assertEquals(2, contenido.photos.size)
    }

    @Test
    fun prepararExportacionConFalloDaErrorYNull() = runTest {
        val g = gestor(Almacen(falla = true))

        assertNull(g.prepararExportacion())

        assertIs<EstadoCopia.Error>(g.estado.value)
    }

    @Test
    fun alGuardarOkMuestraExito() {
        val g = gestor()
        g.alGuardar(ResultadoSelector.Ok(Unit))
        assertIs<EstadoCopia.Exito>(g.estado.value)
    }

    @Test
    fun alGuardarCanceladoNoMuestraMensaje() {
        val g = gestor()
        g.alGuardar(ResultadoSelector.Cancelado)
        assertEquals(EstadoCopia.Inactivo, g.estado.value)
    }

    @Test
    fun alGuardarConErrorMuestraError() {
        val g = gestor()
        g.alGuardar(ResultadoSelector.Error)
        assertIs<EstadoCopia.Error>(g.estado.value)
    }

    @Test
    fun elegirArchivoPideConfirmacionSinTocarNada() = runTest {
        val almacen = Almacen(armarioConDosPrendas())
        val g = gestor(almacen)
        val bytes = copiaDe(WardrobeSnapshot())

        g.alElegirArchivo(ResultadoSelector.Ok(bytes))

        val estado = assertIs<EstadoCopia.ConfirmarImportar>(g.estado.value)
        assertContentEquals(bytes, estado.bytes)
        assertEquals(2, almacen.datos.garments.size) // todavía no se ha reemplazado nada
    }

    @Test
    fun cancelarLaConfirmacionDejaTodoIgual() = runTest {
        val almacen = Almacen(armarioConDosPrendas())
        val g = gestor(almacen)
        g.alElegirArchivo(ResultadoSelector.Ok(copiaDe(WardrobeSnapshot())))

        g.cancelarImportacion()

        assertEquals(EstadoCopia.Inactivo, g.estado.value)
        assertEquals(2, almacen.datos.garments.size)
    }

    @Test
    fun elegirArchivoCanceladoOConErrorSeGestiona() {
        val g = gestor()
        g.alElegirArchivo(ResultadoSelector.Cancelado)
        assertEquals(EstadoCopia.Inactivo, g.estado.value)
        g.alElegirArchivo(ResultadoSelector.Error)
        assertIs<EstadoCopia.Error>(g.estado.value)
    }

    @Test
    fun importarReemplazaYCuentaPrendasYOutfits() = runTest {
        val bytes = copiaDe(armarioConDosPrendas(), mapOf("a.jpg" to byteArrayOf(1), "b.jpg" to byteArrayOf(2)))
        val destino = Almacen(WardrobeSnapshot(garments = listOf(prenda(99, ""))))
        val g = gestor(destino)

        g.importar(bytes)

        val estado = assertIs<EstadoCopia.Exito>(g.estado.value)
        assertEquals("Copia importada: 2 prendas y 1 outfit.", estado.mensaje)
        assertEquals(listOf(1L, 2L), destino.datos.garments.map { it.id })
    }

    @Test
    fun importarAvisaDePrendasSinFoto() = runTest {
        val bytes = copiaDe(armarioConDosPrendas(), mapOf("a.jpg" to byteArrayOf(1))) // la b no tiene foto
        val g = gestor()

        g.importar(bytes)

        assertTrue("1 prenda venía sin foto" in assertIs<EstadoCopia.Exito>(g.estado.value).mensaje)
    }

    @Test
    fun importarUnArchivoQueNoEsCopiaDaErrorYNoCambiaNada() = runTest {
        val almacen = Almacen(armarioConDosPrendas())
        val g = gestor(almacen)

        g.importar(ByteArray(64) { 7 })

        assertTrue("no es una copia" in assertIs<EstadoCopia.Error>(g.estado.value).mensaje)
        assertEquals(2, almacen.datos.garments.size)
    }

    @Test
    fun importarUnaCopiaTruncadaDaErrorDeDanada() = runTest {
        val almacen = Almacen(armarioConDosPrendas())
        val g = gestor(almacen)
        val completa = copiaDe(armarioConDosPrendas(), mapOf("a.jpg" to ByteArray(50), "b.jpg" to ByteArray(50)))

        g.importar(completa.copyOf(completa.size - 20))

        assertTrue("dañada" in assertIs<EstadoCopia.Error>(g.estado.value).mensaje)
        assertEquals(2, almacen.datos.garments.size)
    }

    @Test
    fun importarUnaCopiaDeVersionPosteriorPideActualizar() = runTest {
        val almacen = Almacen(armarioConDosPrendas())
        val g = gestor(almacen)
        val futura = BackupCodec.encode(
            BackupManifest(
                formatVersion = BackupManifest.FORMAT_VERSION + 1, createdAt = 0, garments = emptyList(), outfits = emptyList(),
                outfitGarments = emptyList(), measurements = null, calendar = emptyList(), trips = emptyList(),
                tripOutfits = emptyList(), tripGarments = emptyList(), wears = emptyList(), photos = emptyList(),
            ),
            emptyList(),
        )

        g.importar(futura)

        assertTrue("Actualiza la app" in assertIs<EstadoCopia.Error>(g.estado.value).mensaje)
        assertEquals(2, almacen.datos.garments.size)
    }

    @Test
    fun fallosInesperadosDanErrorGenerico() = runTest {
        val bytes = copiaDe(armarioConDosPrendas())
        val g = gestor(Almacen(falla = true))

        g.importar(bytes)

        assertTrue("No se pudo importar" in assertIs<EstadoCopia.Error>(g.estado.value).mensaje)
    }

    @Test
    fun mensajeConcuerdaEnSingular() {
        assertEquals("Copia importada: 1 prenda y 0 outfits.", mensajeImportada(RestoreResult(1, 0, emptyList())))
        assertEquals(
            "Copia importada: 3 prendas y 1 outfit. 2 prendas venían sin foto en la copia.",
            mensajeImportada(RestoreResult(3, 1, listOf(1, 2))),
        )
    }
}
