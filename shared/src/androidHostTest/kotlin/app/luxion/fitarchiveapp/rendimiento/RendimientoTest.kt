package app.luxion.fitarchiveapp.rendimiento

import app.luxion.fitarchiveapp.data.GarmentFilter
import app.luxion.fitarchiveapp.data.Repositories
import app.luxion.fitarchiveapp.data.createTestDatabase
import app.luxion.fitarchiveapp.model.CalendarDay
import app.luxion.fitarchiveapp.model.Garment
import app.luxion.fitarchiveapp.model.GarmentColor
import app.luxion.fitarchiveapp.model.GarmentType
import app.luxion.fitarchiveapp.model.Occasion
import app.luxion.fitarchiveapp.model.Outfit
import app.luxion.fitarchiveapp.model.OutfitGarment
import app.luxion.fitarchiveapp.model.Season
import app.luxion.fitarchiveapp.model.Usage
import app.luxion.fitarchiveapp.suggestions.AfinidadAprendida
import app.luxion.fitarchiveapp.suggestions.MotorSugerenciasPorReglas
import app.luxion.fitarchiveapp.suggestions.OutfitGuardado
import app.luxion.fitarchiveapp.suggestions.PedidoSugerencia
import app.luxion.fitarchiveapp.ui.armario.ArmarioViewModel
import app.luxion.fitarchiveapp.ui.hoy.OutfitDelDiaViewModel
import app.luxion.fitarchiveapp.ui.outfit.ListaOutfitsViewModel
import app.luxion.fitarchiveapp.weather.Ciudad
import app.luxion.fitarchiveapp.weather.CondicionTiempo
import app.luxion.fitarchiveapp.weather.ServicioTiempo
import app.luxion.fitarchiveapp.weather.TiempoDia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Benchmarks sintéticos de T47 (armario grande). Miden en la JVM del host con SQLite en memoria, así que sirven
 * para comparar antes/después, no como tiempos absolutos de un móvil. Escriben `build/rendimiento.txt`; los topes
 * son holgados (detectan regresiones graves, no ruido). Resultados y método: docs/rendimiento.md.
 */
class RendimientoTest {
    private val hoy = "2026-11-03"
    private val prendasN = 1000
    private val outfitsN = 400
    private val diasN = 3 * 365

    private val informe = StringBuilder()

    private fun linea(nombre: String, ms: Double) = informe.appendLine(String.format("%-60s %9.2f ms", nombre, ms))

    private fun medir(nombre: String, repeticiones: Int = 5, calentar: Int = 2, bloque: () -> Unit): Double {
        repeat(calentar) { bloque() }
        val tiempos = List(repeticiones) {
            val t0 = System.nanoTime()
            bloque()
            (System.nanoTime() - t0) / 1e6
        }.sorted()
        return tiempos[tiempos.size / 2].also { linea(nombre, it) }
    }

    private fun medirUna(nombre: String, bloque: () -> Unit): Double {
        val t0 = System.nanoTime()
        bloque()
        return ((System.nanoTime() - t0) / 1e6).also { linea(nombre, it) }
    }

    private fun armario(n: Int, rnd: Random): List<Garment> {
        val marcas = listOf("Zara", "Mango", "H&M", "Uniqlo", "Massimo Dutti", null, "Bershka", "Cos")
        return List(n) { i ->
            Garment(
                id = 0, photoPath = "/data/fotos/p$i.png",
                type = GarmentType.entries[rnd.nextInt(GarmentType.entries.size)],
                color = GarmentColor.entries[rnd.nextInt(GarmentColor.entries.size)],
                season = Season.entries[rnd.nextInt(3)], usage = Usage.entries[rnd.nextInt(5)],
                brand = marcas[rnd.nextInt(marcas.size)], size = "M", favorite = rnd.nextInt(10) == 0, dirty = rnd.nextInt(8) == 0,
                wearsSinceWash = 0, totalWears = 0, washAfterWears = null, createdAt = 1_700_000_000_000 + i,
            )
        }
    }

    private class Datos(val repos: Repositories, val outfitIds: List<Long>)

    private fun sembrar(): Datos = runBlocking {
        val rnd = Random(47)
        val repos = Repositories(createTestDatabase(), Dispatchers.Default)
        val ids = armario(prendasN, rnd).map { repos.garments.add(it) }
        val outfitIds = (0 until outfitsN).map { i ->
            val piezas = ids.shuffled(rnd).take(3 + rnd.nextInt(2)).mapIndexed { z, g -> OutfitGarment(0, g, 0.5f, 0.1f * z, 1f, z) }
            repos.outfits.create(Outfit(0, "Outfit $i", Occasion.entries[rnd.nextInt(5)], rnd.nextInt(6) == 0, 1_700_000_000_000 + i), piezas)
        }
        // Años de usos: un outfit asignado casi cada día.
        val inicio = LocalDate.parse(hoy).minus(DatePeriod(days = diasN))
        for (d in 0 until diasN) {
            if (rnd.nextInt(10) == 0) continue
            repos.calendar.set(CalendarDay(inicio.plus(DatePeriod(days = d)).toString(), outfitIds[rnd.nextInt(outfitIds.size)], null))
        }
        Datos(repos, outfitIds)
    }

    private val tiempoFijo = object : ServicioTiempo {
        override suspend fun tiempoDeHoy(ciudadElegida: Ciudad?) =
            Result.success(TiempoDia(14.0, 18.0, 10.0, CondicionTiempo.NUBLADO))

        override suspend fun buscarCiudades(texto: String) = Result.success(emptyList<Ciudad>())
    }

    @Test
    fun benchmarks() = runBlocking {
        val d = sembrar()
        val r = d.repos
        informe.appendLine("Armario: $prendasN prendas, $outfitsN outfits, $diasN días de calendario")

        // --- Arranque: contar usos de los días pasados ---
        medirUna("Arranque: registrarHasta (primera vez, ~3 años pendientes)") { runBlocking { r.usos.registrarHasta(hoy) } }
        medir("Arranque: registrarHasta (ya al día, cada apertura)") { runBlocking { r.usos.registrarHasta(hoy) } }

        // --- Datos ---
        medir("Garment.observeAll().first()") { runBlocking { r.garments.observeAll().first() } }
        medir("Garment.observeFiltered(tipo+estación).first()") {
            runBlocking { r.garments.observeFiltered(GarmentFilter(type = GarmentType.JEANS, season = Season.SUMMER)).first() }
        }
        val historial = medir("Usos.todos() (historial completo)") { runBlocking { r.usos.todos() } }
        val usos = r.usos.todos()
        informe.appendLine("  (filas de WearLog: ${usos.size})")

        // --- Armario: estado de la pantalla ---
        medir("ArmarioViewModel: primer estado") {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            runBlocking { withTimeout(30_000) { ArmarioViewModel(r.garments, scope).estado.first { !it.cargando } } }
            scope.cancel()
        }
        run {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val vm = ArmarioViewModel(r.garments, scope)
            runBlocking {
                vm.estado.first { !it.cargando }
                // Pestaña con prendas: Favoritas.
                vm.seleccionarPestana(vm.estado.value.pestanas.lastIndex)
            }
            medir("ArmarioViewModel: marcar favorita -> nuevo estado", repeticiones = 7) {
                runBlocking {
                    val antes = vm.estado.value
                    val p = antes.prendas.first()
                    vm.alternarFavorita(p)
                    withTimeout(30_000) { vm.estado.first { e -> e.prendas.size != antes.prendas.size } }
                }
            }
            scope.cancel()
        }

        // --- Lista de outfits (N+1) ---
        medir("ListaOutfitsViewModel: primera lista (N outfits)", repeticiones = 3, calentar = 1) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            runBlocking {
                withTimeout(120_000) {
                    ListaOutfitsViewModel(r.outfits, r.garments, scope).estado.first { !it.cargando && it.outfits.size == outfitsN }
                }
            }
            scope.cancel()
        }
        run {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val vm = ListaOutfitsViewModel(r.outfits, r.garments, scope)
            runBlocking { vm.estado.first { !it.cargando && it.outfits.size == outfitsN } }
            medir("ListaOutfitsViewModel: cambiar favorito -> lista nueva", repeticiones = 5, calentar = 1) {
                runBlocking {
                    val antes = vm.estado.value
                    val o = antes.outfits.first().outfit
                    vm.alternarFavorito(o)
                    withTimeout(120_000) { vm.estado.first { e -> e.outfits.firstOrNull()?.outfit?.favorite == !o.favorite } }
                }
            }
            scope.cancel()
        }

        // --- Outfit del día completo (entorno + motor) ---
        medir("OutfitDelDiaViewModel: de cero a sugerencias", repeticiones = 3, calentar = 1) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            runBlocking {
                withTimeout(120_000) {
                    OutfitDelDiaViewModel(
                        MotorSugerenciasPorReglas(), r.garments, r.usos, r.calendar, r.outfits, tiempoFijo,
                        { null }, scope, hoy, { 0L },
                    ).estado.first { !it.cargando && it.sugerencias.isNotEmpty() }
                }
            }
            scope.cancel()
        }

        // --- Motor y afinidad puros ---
        val prendas = r.garments.observeAll().first()
        val guardados = d.outfitIds.mapNotNull { r.outfits.get(it) }.map { OutfitGuardado.de(it.outfit, it.garments) }
        medir("AfinidadAprendida.desde(armario, outfits, usos)") { AfinidadAprendida.desde(prendas, guardados, usos) }
        val afinidad = AfinidadAprendida.desde(prendas, guardados, usos)
        val motor = MotorSugerenciasPorReglas()
        val tiempo = TiempoDia(14.0, 18.0, 10.0, CondicionTiempo.NUBLADO)
        medir("Motor.sugerir (sin afinidad)") { motor.sugerir(PedidoSugerencia(prendas, hoy, tiempo, usos = usos)) }
        medir("Motor.sugerir (con afinidad)") { motor.sugerir(PedidoSugerencia(prendas, hoy, tiempo, usos = usos, afinidad = afinidad)) }
        medir("Motor.sugerir (sin tiempo ni ocasión, con afinidad)") { motor.sugerir(PedidoSugerencia(prendas, hoy, usos = usos, afinidad = afinidad)) }

        File("build").mkdirs()
        File("build/rendimiento.txt").writeText(informe.toString())
        println(informe)

        // Topes holgados: solo cazan regresiones graves.
        assertTrue(historial < 2_000, "Usos.todos() tarda demasiado: $historial ms")
    }
}
