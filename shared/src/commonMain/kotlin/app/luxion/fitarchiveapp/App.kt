package app.luxion.fitarchiveapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.luxion.fitarchiveapp.data.Repositories
import app.luxion.fitarchiveapp.ui.navigation.Destino
import app.luxion.fitarchiveapp.ui.components.SeccionUbicacionTiempo
import app.luxion.fitarchiveapp.ui.screens.MedidasScreen
import app.luxion.fitarchiveapp.ui.screens.PantallaCarga
import app.luxion.fitarchiveapp.ui.screens.PantallaVacia
import app.luxion.fitarchiveapp.ui.theme.FitArchiveTheme
import app.luxion.fitarchiveapp.weather.ClienteHttpKtor
import app.luxion.fitarchiveapp.weather.SeleccionCiudad
import app.luxion.fitarchiveapp.weather.ServicioTiempoOpenMeteo
import app.luxion.fitarchiveapp.weather.rememberAlmacenCiudad
import app.luxion.fitarchiveapp.weather.rememberProveedorUbicacion

@Composable
fun App(repositories: Repositories) {
    FitArchiveTheme {
        var cargando by remember { mutableStateOf(true) }
        if (cargando) {
            PantallaCarga(onTerminado = { cargando = false })
            return@FitArchiveTheme
        }
        var destino by remember { mutableStateOf(Destino.Hoy) }
        val ubicacion = rememberProveedorUbicacion()
        val almacenCiudad = rememberAlmacenCiudad()
        val seleccionCiudad = remember(ubicacion, almacenCiudad) {
            SeleccionCiudad(ServicioTiempoOpenMeteo(ClienteHttpKtor(), ubicacion), almacenCiudad)
        }
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
                Destino.Perfil -> MedidasScreen(repositories.measurements, Modifier.padding(padding)) {
                    SeccionUbicacionTiempo(seleccionCiudad, ubicacion)
                }
                else -> PantallaVacia(destino.titulo, Modifier.padding(padding))
            }
        }
    }
}
