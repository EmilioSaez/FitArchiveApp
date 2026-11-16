package app.luxion.fitarchiveapp.ia

/**
 * Errores de la IA, siempre con un mensaje en español para la usuaria. Ningún mensaje contiene la clave de API.
 * [reintentable] indica si tiene sentido volver a intentar la misma llamada automáticamente.
 */
sealed class ErrorIa(
    val mensaje: String,
    val reintentable: Boolean = false,
    causa: Throwable? = null,
) : Exception(mensaje, causa) {
    class SinClave : ErrorIa("Falta tu clave de API. Añádela en Perfil → Inteligencia artificial.")
    class Desactivada : ErrorIa("La IA está desactivada. Actívala en Perfil → Inteligencia artificial.")
    class AvisoPendiente(val aviso: AvisoIa) : ErrorIa("Antes de usar la IA tienes que aceptar el aviso de privacidad.")
    class TopeAlcanzado(val tope: Int) : ErrorIa("Has llegado al tope de $tope consultas de hoy. Puedes subirlo en Perfil.")
    class ClaveNoValida : ErrorIa("La clave de API no es válida o ha sido revocada.")
    class SinSaldo : ErrorIa("Tu cuenta de Anthropic no tiene saldo suficiente.")
    class SinPermiso : ErrorIa("Tu clave no tiene permiso para usar este modelo.")
    class ModeloNoEncontrado : ErrorIa("El modelo elegido no existe o no está disponible para tu clave.")
    class SolicitudInvalida : ErrorIa("La solicitud a la IA no es válida (400).")
    class DemasiadoGrande : ErrorIa("La imagen o el texto son demasiado grandes.")
    class Limite(val reintentarEnSegundos: Long?) : ErrorIa("Has superado el límite de uso de tu clave. Prueba en un rato.", reintentable = true)
    class Servidor(val codigo: Int) : ErrorIa("El servicio de IA no está disponible ahora mismo ($codigo). Prueba en un rato.", reintentable = true)
    class SinRed(causa: Throwable? = null) : ErrorIa("No se pudo conectar con la IA. Revisa tu conexión.", reintentable = true, causa = causa)
    class Rechazo : ErrorIa("La IA no ha querido responder a esta petición.")
    class Truncada : ErrorIa("La respuesta de la IA se cortó antes de terminar.")
    class RespuestaInvalida(causa: Throwable? = null) : ErrorIa("La respuesta de la IA no tiene el formato esperado.", causa = causa)
    class Almacen(causa: Throwable? = null) : ErrorIa("No se pudo guardar la clave de forma segura en este dispositivo.", causa = causa)
}
