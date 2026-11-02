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
import app.luxion.fitarchiveapp.data.GarmentRepository
import app.luxion.fitarchiveapp.data.GarmentRepositoryEnMemoria
import app.luxion.fitarchiveapp.data.MedidasRepositoryEnMemoria
import app.luxion.fitarchiveapp.ui.armario.ArmarioScreen
import app.luxion.fitarchiveapp.ui.navigation.Destino
import app.luxion.fitarchiveapp.ui.screens.MedidasScreen
import app.luxion.fitarchiveapp.ui.screens.PantallaCarga
import app.luxion.fitarchiveapp.ui.screens.PantallaVacia
import app.luxion.fitarchiveapp.ui.theme.FitArchiveTheme

@Composable
fun App(
    garments: GarmentRepository? = null,
    /** Punto de entrada para añadir una prenda; lo engancha T08. */
    onAnadirPrenda: () -> Unit = {},
) {
    FitArchiveTheme {
        var cargando by remember { mutableStateOf(true) }
        if (cargando) {
            PantallaCarga(onTerminado = { cargando = false })
            return@FitArchiveTheme
        }
        var destino by remember { mutableStateOf(Destino.Hoy) }
        val prendasRepo = garments ?: remember { GarmentRepositoryEnMemoria() }
        val medidasRepo = remember { MedidasRepositoryEnMemoria() }
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
                Destino.Armario -> ArmarioScreen(prendasRepo, onAnadirPrenda, Modifier.padding(padding))
                Destino.Perfil -> MedidasScreen(medidasRepo, Modifier.padding(padding))
                else -> PantallaVacia(destino.titulo, Modifier.padding(padding))
            }
        }
    }
}
