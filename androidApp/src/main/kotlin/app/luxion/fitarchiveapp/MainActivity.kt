package app.luxion.fitarchiveapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.luxion.fitarchiveapp.data.Repositories
import app.luxion.fitarchiveapp.data.crearRepositories

class MainActivity : ComponentActivity() {
    private val repositories: Repositories by lazy { crearRepositories(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Iconos de la barra de estado oscuros sobre fondo claro (y al revés en modo oscuro).
        enableEdgeToEdge()
        setContent { App(repositories) }
    }
}
