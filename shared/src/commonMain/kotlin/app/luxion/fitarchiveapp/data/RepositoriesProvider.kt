package app.luxion.fitarchiveapp.data

import androidx.compose.runtime.Composable

/** Abre (una vez) la BD local de la plataforma y devuelve los repositorios. */
@Composable
expect fun rememberRepositories(): Repositories
