package app.luxion.fitarchiveapp.ia

/**
 * Aviso de privacidad que la usuaria acepta una vez antes de la primera llamada. Cada función de IA que envíe
 * datos distintos declara su propio aviso (otro [id]); [AvisosIa.GENERAL] cubre la configuración y lo básico.
 */
data class AvisoIa(val id: String, val titulo: String, val texto: String)

object AvisosIa {
    val GENERAL = AvisoIa(
        id = "general1",
        titulo = "Antes de usar la IA",
        texto = "Para usar la IA, FitArchive envía directamente desde tu móvil a Anthropic, con tu propia clave de API, " +
            "lo necesario para cada consulta: las fotos de las prendas que analices y los datos de tu armario que la " +
            "función indique (tipo, color, estación, uso…) y el texto que escribas.\n\n" +
            "No pasa por ningún servidor de FitArchive. No se envía tu nombre, correo, ubicación ni ningún " +
            "identificador. Anthropic trata esos datos según su propia política de privacidad.\n\n" +
            "Cada consulta se factura a tu cuenta de Anthropic. Las respuestas de la IA son orientativas y pueden " +
            "contener errores.",
    )
}
