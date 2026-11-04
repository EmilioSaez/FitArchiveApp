package app.luxion.fitarchiveapp.ui.copia

import androidx.compose.runtime.Composable

/** Resultado de un selector de documentos del sistema. */
sealed interface ResultadoSelector<out T> {
    /** La usuaria cerró el selector sin elegir. */
    data object Cancelado : ResultadoSelector<Nothing>

    /** El selector funcionó pero no se pudo escribir o leer el archivo. */
    data object Error : ResultadoSelector<Nothing>

    class Ok<T>(val valor: T) : ResultadoSelector<T>
}

/**
 * Selector de documentos del sistema (sin permisos de almacenamiento): `CreateDocument` / `OpenDocument`
 * en Android, `UIDocumentPickerViewController` en iOS. Los callbacks llegan en el hilo principal.
 */
interface SelectorCopia {
    /** Pide dónde guardar y escribe [bytes] en el archivo elegido, con [nombre] sugerido. */
    fun guardar(nombre: String, bytes: ByteArray, alTerminar: (ResultadoSelector<Unit>) -> Unit)

    /** Pide un archivo y devuelve su contenido. */
    fun abrir(alTerminar: (ResultadoSelector<ByteArray>) -> Unit)
}

@Composable
expect fun rememberSelectorCopia(): SelectorCopia
