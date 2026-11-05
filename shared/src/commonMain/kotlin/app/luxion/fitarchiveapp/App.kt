package app.luxion.fitarchiveapp

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.luxion.fitarchiveapp.color.detectarColorDeFoto
import app.luxion.fitarchiveapp.data.Repositories
import app.luxion.fitarchiveapp.fondo.crearQuitaFondo
import app.luxion.fitarchiveapp.foto.FotoCapturada
import app.luxion.fitarchiveapp.ui.anadir.AnadirPrendaFlow
import app.luxion.fitarchiveapp.ui.armario.ArmarioScreen
import app.luxion.fitarchiveapp.ui.calendario.CalendarioScreen
import app.luxion.fitarchiveapp.ui.carpetas.CarpetasScreen
import app.luxion.fitarchiveapp.ui.detalle.DetallePrendaScreen
import app.luxion.fitarchiveapp.ui.hoy.OutfitDelDiaScreen
import app.luxion.fitarchiveapp.ui.navigation.Destino
import app.luxion.fitarchiveapp.ui.outfit.EditorOutfitScreen
import app.luxion.fitarchiveapp.ui.outfit.OutfitsScreen
import app.luxion.fitarchiveapp.ui.components.SeccionUbicacionTiempo
import app.luxion.fitarchiveapp.ui.copia.SeccionCopia
import app.luxion.fitarchiveapp.ui.screens.MedidasScreen
import app.luxion.fitarchiveapp.ui.screens.PantallaCarga
import app.luxion.fitarchiveapp.ui.theme.FitArchiveTheme
import app.luxion.fitarchiveapp.ui.navigation.BarraNavegacion
import app.luxion.fitarchiveapp.ui.portada.PortadaScreen
import app.luxion.fitarchiveapp.ui.viaje.ViajesScreen
import app.luxion.fitarchiveapp.weather.CacheTiempo
import app.luxion.fitarchiveapp.weather.ClienteHttpKtor
import app.luxion.fitarchiveapp.weather.SeleccionCiudad
import app.luxion.fitarchiveapp.weather.ServicioTiempoOpenMeteo
import app.luxion.fitarchiveapp.weather.rememberAlmacenCiudad
import app.luxion.fitarchiveapp.weather.rememberProveedorUbicacion
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
fun App(repositories: Repositories) {
    FitArchiveTheme {
        // Fuera de las ramas con return: el servicio del tiempo se recrea al volver de otras pantallas, la caché no.
        val cacheTiempo = remember { CacheTiempo() }
        var cargando by remember { mutableStateOf(true) }
        if (cargando) {
            PantallaCarga(onTerminado = { cargando = false })
            return@FitArchiveTheme
        }
        var anadiendo by remember { mutableStateOf(false) }
        var portada by remember { mutableStateOf(true) }
        if (portada) {
            val prendas by repositories.garments.observeAll().collectAsState(emptyList())
            val looks by repositories.outfits.observeAll().collectAsState(emptyList())
            PortadaScreen(
                prendas,
                looks.size,
                onVerOutfitDeHoy = { portada = false },
                onAnadirPrenda = {
                    portada = false
                    anadiendo = true
                },
            )
            return@FitArchiveTheme
        }
        // Cada día que pasa con un outfit asignado suma un uso a sus prendas (T33).
        LaunchedEffect(Unit) {
            runCatching { repositories.usos.registrarHasta(Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()) }
        }
        var destino by remember { mutableStateOf(Destino.Hoy) }
        // "Configurar ubicación" abre Perfil ya en "Ubicación y tiempo"; entrar por la barra inferior empieza arriba.
        var perfilEnUbicacion by remember { mutableStateOf(false) }
        if (anadiendo) {
            val quitaFondo = remember { crearQuitaFondo() }
            AnadirPrendaFlow(
                repositories.garments,
                onTerminado = { anadiendo = false },
                quitarFondo = { foto -> FotoCapturada(quitaFondo.quitar(foto).bytesPng) },
                detectarColor = ::detectarColorDeFoto,
            )
            return@FitArchiveTheme
        }
        var detalleId by remember { mutableStateOf<Long?>(null) }
        detalleId?.let { id ->
            DetallePrendaScreen(
                repositories.garments,
                repositories.measurements,
                repositories.usos,
                id,
                onVolver = { detalleId = null },
                onAbrirPerfil = {
                    destino = Destino.Perfil
                    detalleId = null
                },
            )
            return@FitArchiveTheme
        }
        var viendoViajes by remember { mutableStateOf(false) }
        var viajeInicialId by remember { mutableStateOf<Long?>(null) }
        var viendoCarpetas by remember { mutableStateOf(false) }
        var prendasIniciales by remember { mutableStateOf(emptyList<Long>()) }
        var editandoOutfit by remember { mutableStateOf(false) }
        var outfitEditadoId by remember { mutableStateOf<Long?>(null) }
        if (editandoOutfit) {
            EditorOutfitScreen(
                repositories.outfits,
                repositories.garments,
                onTerminado = {
                    editandoOutfit = false
                    outfitEditadoId = null
                    prendasIniciales = emptyList()
                },
                outfitId = outfitEditadoId,
                prendasIniciales = prendasIniciales,
            )
            return@FitArchiveTheme
        }
        if (viendoViajes) {
            ViajesScreen(
                repositories.trips,
                repositories.outfits,
                repositories.garments,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                viajeInicialId = viajeInicialId,
                onVolver = {
                    viendoViajes = false
                    viajeInicialId = null
                },
            )
            return@FitArchiveTheme
        }
        if (viendoCarpetas) {
            CarpetasScreen(
                repositories.garments,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
                onVolver = { viendoCarpetas = false },
                onAbrirPrenda = { detalleId = it.id },
            )
            return@FitArchiveTheme
        }
        val ubicacion = rememberProveedorUbicacion()
        val almacenCiudad = rememberAlmacenCiudad()
        val servicioTiempo = remember(ubicacion) { ServicioTiempoOpenMeteo(ClienteHttpKtor(), cacheTiempo, ubicacion) }
        val seleccionCiudad = remember(servicioTiempo, almacenCiudad) { SeleccionCiudad(servicioTiempo, almacenCiudad) }
        Scaffold(
            bottomBar = {
                BarraNavegacion(destino) {
                    destino = it
                    perfilEnUbicacion = false
                }
            },
        ) { padding ->
            when (destino) {
                Destino.Hoy -> OutfitDelDiaScreen(
                    repositories,
                    servicioTiempo,
                    ciudad = seleccionCiudad::ciudadGuardada,
                    onAbrirPerfil = {
                        perfilEnUbicacion = true
                        destino = Destino.Perfil
                    },
                    onAnadirPrendas = { anadiendo = true },
                    modifier = Modifier.padding(padding),
                )
                Destino.Perfil -> MedidasScreen(
                    repositories.measurements,
                    Modifier.padding(padding),
                    irAlContenidoExtra = perfilEnUbicacion,
                ) {
                    SeccionUbicacionTiempo(seleccionCiudad, ubicacion)
                    repositories.copia?.let { SeccionCopia(it) }
                    Text(
                        "FitArchive ${rememberVersionApp()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Destino.Armario -> ArmarioScreen(
                    repositories.garments,
                    onAnadirPrenda = { anadiendo = true },
                    onAbrirPrenda = { detalleId = it.id },
                    onCrearOutfit = {
                        prendasIniciales = it
                        outfitEditadoId = null
                        editandoOutfit = true
                    },
                    onAbrirCarpetas = { viendoCarpetas = true },
                    modifier = Modifier.padding(padding),
                )
                Destino.Outfits -> OutfitsScreen(
                    repositories.outfits,
                    repositories.garments,
                    onNuevoOutfit = { editandoOutfit = true },
                    onAbrirOutfit = {
                        outfitEditadoId = it.id
                        editandoOutfit = true
                    },
                    modifier = Modifier.padding(padding),
                )
                Destino.Calendario -> CalendarioScreen(
                    repositories.calendar,
                    repositories.outfits,
                    repositories.trips,
                    Modifier.padding(padding),
                    onAbrirViaje = {
                        viajeInicialId = it.id
                        viendoViajes = true
                    },
                    onAbrirViajes = { viendoViajes = true },
                )
            }
        }
    }
}
