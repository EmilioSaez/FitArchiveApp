package com.emilio.fitarchive

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
import com.emilio.fitarchive.ui.navigation.Destino
import com.emilio.fitarchive.ui.screens.PantallaVacia
import com.emilio.fitarchive.ui.theme.FitArchiveTheme

@Composable
fun App() {
    FitArchiveTheme {
        var destino by remember { mutableStateOf(Destino.Hoy) }
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
            PantallaVacia(destino.titulo, Modifier.padding(padding))
        }
    }
}
