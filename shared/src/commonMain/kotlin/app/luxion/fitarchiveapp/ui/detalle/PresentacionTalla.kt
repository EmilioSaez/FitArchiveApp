package app.luxion.fitarchiveapp.ui.detalle

import app.luxion.fitarchiveapp.tallas.AjusteTalla
import app.luxion.fitarchiveapp.tallas.EstadoAjuste
import app.luxion.fitarchiveapp.tallas.EtiquetaAjuste
import app.luxion.fitarchiveapp.tallas.Medida
import app.luxion.fitarchiveapp.tallas.ResultadoAjuste
import app.luxion.fitarchiveapp.tallas.TablaTallas

/** Una zona del cuerpo con su etiqueta ya redactada. */
data class ZonaUi(val zona: String, val etiqueta: EtiquetaAjuste, val texto: String)

/** Lo que muestra el hueco de talla del detalle (T30), sin depender de Compose. */
sealed interface TallaUi {
    /** Tipo de prenda sin talla corporal (bolso, accesorio): no se muestra nada. */
    data object Oculta : TallaUi

    /** Aún no hay medidas de la usuaria: invitar a rellenarlas en Perfil. */
    data object SinMedidas : TallaUi

    /** Sin tabla aplicable o talla no encontrada: mensaje corto, con talla ideal si se pudo calcular. */
    data class Aviso(val mensaje: String, val textoTallaIdeal: String?, val nota: String?) : TallaUi

    data class Resultado(
        val etiqueta: EtiquetaAjuste,
        val titulo: String,
        val zonas: List<ZonaUi>,
        val tallaIdeal: String?,
        /** "Talla ideal en Zara: M"; null si no hay talla ideal. */
        val textoTallaIdeal: String?,
        /** Aviso discreto (tabla genérica o marca pendiente de datos); null si no aplica. */
        val nota: String?,
    ) : TallaUi
}

object PresentacionTalla {
    fun textoEtiqueta(e: EtiquetaAjuste): String = when (e) {
        EtiquetaAjuste.AJUSTADA -> "Ajustada"
        EtiquetaAjuste.BIEN -> "Te queda bien"
        EtiquetaAjuste.HOLGADA -> "Holgada"
    }

    fun nombreZona(m: Medida): String = when (m) {
        Medida.PECHO -> "Pecho"
        Medida.CINTURA -> "Cintura"
        Medida.CADERA -> "Cadera"
        Medida.PIE -> "Pie"
    }

    private fun textoZona(e: EtiquetaAjuste): String = when (e) {
        EtiquetaAjuste.AJUSTADA -> "ajustada"
        EtiquetaAjuste.BIEN -> "bien"
        EtiquetaAjuste.HOLGADA -> "holgada"
    }

    /**
     * Aviso discreto cuando la tabla usada no es la de la marca. Distingue marca sin tabla, pendiente
     * de datos y marca que simplemente no tiene ese tipo de prenda.
     */
    fun nota(r: ResultadoAjuste, marca: String?, tablas: Map<String, TablaTallas>): String? {
        if (!r.esGenerica) return null
        val nombre = marca?.trim().orEmpty()
        val buscada = AjusteTalla.normalizarMarca(nombre)
        if (buscada.isEmpty()) return "Tabla genérica europea: orientativo."
        val tabla = tablas.entries.firstOrNull {
            it.key != AjusteTalla.ID_GENERICA && AjusteTalla.normalizarMarca(it.key) == buscada
        }?.value
        return when {
            tabla == null -> "Sin tabla de $nombre: usamos la genérica europea, orientativo."
            tabla.estaVacia -> "Tabla de $nombre pendiente de datos: usamos la genérica europea, orientativo."
            else -> "$nombre no tiene este tipo de prenda en su tabla: usamos la genérica europea, orientativo."
        }
    }

    fun presentar(r: ResultadoAjuste, marca: String?, tablas: Map<String, TablaTallas>): TallaUi {
        val nota = nota(r, marca, tablas)
        val tablaNombre = r.tablaUsada?.takeIf { it.isNotBlank() } ?: "la tabla"
        val ideal = r.tallaIdeal?.let { "Talla ideal en $tablaNombre: $it" }
        return when (r.estado) {
            EstadoAjuste.NO_APLICA -> TallaUi.Oculta
            EstadoAjuste.SIN_MEDIDAS -> TallaUi.SinMedidas
            EstadoAjuste.SIN_TABLA -> TallaUi.Aviso("No hay tabla de tallas para este tipo de prenda.", null, null)
            EstadoAjuste.TALLA_NO_ENCONTRADA -> TallaUi.Aviso(
                "No encontramos esa talla en la tabla, así que no sabemos cómo te queda.",
                ideal,
                nota,
            )
            EstadoAjuste.OK -> {
                val global = r.global
                    ?: return TallaUi.Aviso("No hay datos suficientes para valorar el ajuste.", ideal, nota)
                TallaUi.Resultado(
                    etiqueta = global,
                    titulo = textoEtiqueta(global),
                    zonas = r.porZona.entries.sortedBy { it.key.ordinal }
                        .map { (m, e) -> ZonaUi(nombreZona(m), e, textoZona(e)) },
                    tallaIdeal = r.tallaIdeal,
                    textoTallaIdeal = ideal,
                    nota = nota,
                )
            }
        }
    }
}
