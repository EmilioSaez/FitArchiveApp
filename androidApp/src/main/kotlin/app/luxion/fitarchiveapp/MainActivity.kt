package app.luxion.fitarchiveapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.luxion.fitarchiveapp.data.Repositories
import app.luxion.fitarchiveapp.data.crearRepositories

class MainActivity : ComponentActivity() {
    private val repositories: Repositories by lazy { crearRepositories(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App(repositories) }
    }
}
