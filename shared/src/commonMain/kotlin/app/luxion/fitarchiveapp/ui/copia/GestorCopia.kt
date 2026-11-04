package app.luxion.fitarchiveapp.ui.copia

import app.luxion.fitarchiveapp.backup.BackupException
import app.luxion.fitarchiveapp.backup.BackupService
import app.luxion.fitarchiveapp.backup.RestoreResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado de la sección "Copia de seguridad" de Perfil. */
sealed interface EstadoCopia {
    data object Inactivo : EstadoCopia
    data object Trabajando : EstadoCopia

    /** Archivo elegido a la espera de que la usuaria confirme que se reemplaza todo. */
    class ConfirmarImportar(val bytes: ByteArray) : EstadoCopia

    data class Exito(val mensaje: String) : EstadoCopia
    data class Error(val mensaje: String) : EstadoCopia
}

/**
 * Lógica de exportar e importar la copia desde Perfil, sin UI ni plataforma: la pantalla le pasa los
 * resultados del selector de documentos y muestra [estado].
 */
class GestorCopia(private val servicio: BackupService) {
    private val _estado = MutableStateFlow<EstadoCopia>(EstadoCopia.Inactivo)
    val estado: StateFlow<EstadoCopia> = _estado.asStateFlow()

    /** Genera los bytes de la copia, o null (con [estado] en error) si falla. */
    suspend fun prepararExportacion(): ByteArray? {
        _estado.value = EstadoCopia.Trabajando
        return try {
            servicio.export().also { _estado.value = EstadoCopia.Inactivo }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            _estado.value = EstadoCopia.Error("No se pudo preparar la copia. Inténtalo de nuevo.")
            null
        }
    }

    fun alGuardar(resultado: ResultadoSelector<Unit>) {
        _estado.value = when (resultado) {
            is ResultadoSelector.Ok ->
                EstadoCopia.Exito("Copia guardada. Guárdala en un lugar seguro: contiene todas tus prendas y fotos.")
            ResultadoSelector.Cancelado -> EstadoCopia.Inactivo
            ResultadoSelector.Error -> EstadoCopia.Error("No se pudo guardar el archivo. Prueba con otra ubicación.")
        }
    }

    fun alElegirArchivo(resultado: ResultadoSelector<ByteArray>) {
        _estado.value = when (resultado) {
            is ResultadoSelector.Ok -> EstadoCopia.ConfirmarImportar(resultado.valor)
            ResultadoSelector.Cancelado -> EstadoCopia.Inactivo
            ResultadoSelector.Error -> EstadoCopia.Error("No se pudo leer el archivo elegido.")
        }
    }

    fun cancelarImportacion() {
        if (_estado.value is EstadoCopia.ConfirmarImportar) _estado.value = EstadoCopia.Inactivo
    }

    /** Importa [bytes] (REEMPLAZA todo). Si falla, el servicio garantiza que no se ha cambiado nada. */
    suspend fun importar(bytes: ByteArray) {
        _estado.value = EstadoCopia.Trabajando
        _estado.value = try {
            EstadoCopia.Exito(mensajeImportada(servicio.import(bytes)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupException.NotABackup) {
            EstadoCopia.Error("El archivo elegido no es una copia de FitArchive. No se ha cambiado nada.")
        } catch (e: BackupException.UnsupportedVersion) {
            EstadoCopia.Error(
                "Esta copia es de una versión más nueva de FitArchive. Actualiza la app para importarla. " +
                    "No se ha cambiado nada.",
            )
        } catch (e: BackupException.Corrupt) {
            EstadoCopia.Error("La copia está dañada o incompleta. No se ha cambiado nada.")
        } catch (e: Throwable) {
            EstadoCopia.Error("No se pudo importar la copia. No se ha cambiado nada.")
        }
    }
}

internal fun mensajeImportada(r: RestoreResult): String {
    val base = "Copia importada: ${r.garments} ${if (r.garments == 1) "prenda" else "prendas"} y " +
        "${r.outfits} ${if (r.outfits == 1) "outfit" else "outfits"}."
    val sinFoto = r.missingPhotos.size
    return if (sinFoto == 0) base
    else "$base ${if (sinFoto == 1) "1 prenda venía sin foto" else "$sinFoto prendas venían sin foto"} en la copia."
}
