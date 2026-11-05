package app.luxion.fitarchiveapp.data

import app.luxion.fitarchiveapp.avatar.AspectoAvatar
import app.luxion.fitarchiveapp.avatar.EstiloPelo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AvatarRepositoryTest {
    private val repo = Repositories(createTestDatabase(), now = { 0L }).avatar

    @Test
    fun sinGuardarDevuelveAspectoPorDefecto() = runTest {
        assertEquals(AspectoAvatar(), repo.get())
        assertEquals(AspectoAvatar(), repo.observe().first())
    }

    @Test
    fun guardaYRecuperaAspecto() = runTest {
        val a = AspectoAvatar(0x8D5A3B, 0xB0361F, EstiloPelo.RECOGIDO, "fotos/yo.jpg", usarFoto = true)
        repo.save(a)
        assertEquals(a, repo.get())
        assertTrue(repo.get().conFoto)
    }

    @Test
    fun guardarReemplazaElAnterior() = runTest {
        repo.save(AspectoAvatar(estiloPelo = EstiloPelo.CORTO))
        repo.save(AspectoAvatar(estiloPelo = EstiloPelo.SIN_PELO, fotoRostro = "x.jpg"))
        val a = repo.get()
        assertEquals(EstiloPelo.SIN_PELO, a.estiloPelo)
        assertFalse(a.conFoto) // hay ruta pero no se pidió usarla
    }
}
