package app.luxion.fitarchiveapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import app.luxion.fitarchiveapp.ui.detalle.DetallePrendaScreen
import app.luxion.fitarchiveapp.ui.hoy.OutfitDelDiaScreen
import app.luxion.fitarchiveapp.ui.navigation.Destino
import app.luxion.fitarchiveapp.ui.outfit.EditorOutfitScreen
import app.luxion.fitarchiveapp.ui.outfit.OutfitsScreen
import app.luxion.fitarchiveapp.ui.components.SeccionUbicacionTiempo
import app.luxion.fitarchiveapp.ui.screens.MedidasScreen
import app.luxion.fitarchiveapp.ui.screens.PantallaCarga
import app.luxion.fitarchiveapp.ui.theme.FitArchiveTheme
import app.luxion.fitarchiveapp.ui.viaje.ViajesScreen
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
        var cargando by remember { mutableStateOf(true) }
        if (cargando) {
            PantallaCarga(onTerminado = { cargando = false })
            return@FitArchiveTheme
        }
        // Cada día que pasa con un outfit asignado suma un uso a sus prendas (T33).
        LaunchedEffect(Unit) {
            runCatching { repositories.usos.registrarHasta(Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()) }
        }
        var destino by remember { mutableStateOf(Destino.Hoy) }
        var anadiendo by remember { mutableStateOf(false) }
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
        var editandoOutfit by remember { mutableStateOf(false) }
        var outfitEditadoId by remember { mutableStateOf<Long?>(null) }
        if (editandoOutfit) {
            EditorOutfitScreen(
                repositories.outfits,
                repositories.garments,
                onTerminado = {
                    editandoOutfit = false
                    outfitEditadoId = null
                },
                outfitId = outfitEditadoId,
            )
            return@FitArchiveTheme
        }
        val ubicacion = rememberProveedorUbicacion()
        val almacenCiudad = rememberAlmacenCiudad()
        val servicioTiempo = remember(ubicacion) { ServicioTiempoOpenMeteo(ClienteHttpKtor(), ubicacion) }
        val seleccionCiudad = remember(servicioTiempo, almacenCiudad) { SeleccionCiudad(servicioTiempo, almacenCiudad) }
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Destino.entries.forEach { item ->
                        NavigationBarItem(
                            selected = item == destino,
                            onClick = { destino = item },
                            icon = { Box {} },
                            label = { Text(item.titulo) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            },
        ) { padding ->
            when (destino) {
                Destino.Hoy -> OutfitDelDiaScreen(
                    repositories,
                    servicioTiempo,
                    ciudad = seleccionCiudad::ciudadGuardada,
                    onAbrirPerfil = { destino = Destino.Perfil },
                    onAnadirPrendas = { anadiendo = true },
                    modifier = Modifier.padding(padding),
                )
                Destino.Perfil -> MedidasScreen(repositories.measurements, Modifier.padding(padding)) {
                    SeccionUbicacionTiempo(seleccionCiudad, ubicacion)
                }
                Destino.Armario -> ArmarioScreen(
                    repositories.garments,
                    onAnadirPrenda = { anadiendo = true },
                    onAbrirPrenda = { detalleId = it.id },
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
                    Modifier.padding(padding),
                )
                Destino.Viajes -> ViajesScreen(
                    repositories.trips,
                    repositories.outfits,
                    repositories.garments,
                    Modifier.padding(padding),
                )
            }
        }
    }
}
