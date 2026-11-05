package app.luxion.fitarchiveapp.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Modo claro/oscuro: [SISTEMA] sigue el del dispositivo. */
enum class ModoTema(val etiqueta: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro"),
    ;

    fun esOscuro(sistemaOscuro: Boolean) = when (this) {
        SISTEMA -> sistemaOscuro
        CLARO -> false
        OSCURO -> true
    }
}

/** Colores de acento disponibles; [Burdeos] es el original de la app. */
enum class Acento(val etiqueta: String, val color: Color) {
    BURDEOS("Burdeos", Burdeos),
    AZUL("Azul", Color(0xFF2F5D9B)),
    VERDE("Verde", Color(0xFF2F6B4F)),
    VIOLETA("Violeta", Color(0xFF6A3FA0)),
    TERRACOTA("Terracota", Color(0xFFB4532A)),
    GRAFITO("Grafito", Color(0xFF3D4249)),
}

data class AjustesTema(
    val modo: ModoTema = ModoTema.SISTEMA,
    val acento: Acento = Acento.BURDEOS,
)

/** Serialización estable para guardar los ajustes como texto; valores desconocidos caen al defecto. */
object AjustesTemaCodec {
    fun codificar(a: AjustesTema) = "${a.modo.name}|${a.acento.name}"

    fun decodificar(texto: String?): AjustesTema {
        val partes = texto?.split('|') ?: return AjustesTema()
        return AjustesTema(
            modo = ModoTema.entries.firstOrNull { it.name == partes.getOrNull(0) } ?: ModoTema.SISTEMA,
            acento = Acento.entries.firstOrNull { it.name == partes.getOrNull(1) } ?: Acento.BURDEOS,
        )
    }
}

/** Persistencia de los ajustes de tema. */
interface AlmacenTema {
    fun cargar(): AjustesTema
    fun guardar(ajustes: AjustesTema)
}

/** Almacén de los ajustes de tema de la plataforma. */
@Composable
expect fun rememberAlmacenTema(): AlmacenTema
