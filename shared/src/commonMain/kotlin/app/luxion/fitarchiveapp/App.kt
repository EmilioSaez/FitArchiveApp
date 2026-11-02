package app.luxion.fitarchiveapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.luxion.fitarchiveapp.data.MedidasRepositoryEnMemoria
import app.luxion.fitarchiveapp.data.rememberRepositories
import app.luxion.fitarchiveapp.ui.anadir.AnadirPrendaFlow
import app.luxion.fitarchiveapp.ui.anadir.BotonAnadirPrenda
import app.luxion.fitarchiveapp.ui.navigation.Destino
import app.luxion.fitarchiveapp.ui.screens.MedidasScreen
import app.luxion.fitarchiveapp.ui.screens.PantallaCarga
import app.luxion.fitarchiveapp.ui.screens.PantallaVacia
import app.luxion.fitarchiveapp.ui.theme.FitArchiveTheme

@Composable
fun App() {
    FitArchiveTheme {
        var cargando by remember { mutableStateOf(true) }
        if (cargando) {
            PantallaCarga(onTerminado = { cargando = false })
            return@FitArchiveTheme
        }
        var destino by remember { mutableStateOf(Destino.Hoy) }
        val medidasRepo = remember { MedidasRepositoryEnMemoria() }
        val repos = rememberRepositories()
        var anadiendo by remember { mutableStateOf(false) }
        if (anadiendo) {
            AnadirPrendaFlow(repos.garments, onTerminado = { anadiendo = false })
            return@FitArchiveTheme
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
                Destino.Perfil -> MedidasScreen(medidasRepo, Modifier.padding(padding))
                // T10 sustituye el contenido; el botón es el punto de entrada de T08.
                Destino.Armario -> Box(Modifier.fillMaxSize().padding(padding)) {
                    PantallaVacia(destino.titulo)
                    BotonAnadirPrenda(
                        onClick = { anadiendo = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    )
                }
                else -> PantallaVacia(destino.titulo, Modifier.padding(padding))
            }
        }
    }
}
