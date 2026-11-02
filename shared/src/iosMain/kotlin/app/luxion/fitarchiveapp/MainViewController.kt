package app.luxion.fitarchiveapp

import androidx.compose.ui.window.ComposeUIViewController
import app.luxion.fitarchiveapp.data.crearRepositories

private val repositories by lazy { crearRepositories() }

fun MainViewController() = ComposeUIViewController { App(repositories) }