package app.luxion.fitarchiveapp.tallas

import app.luxion.fitarchiveapp.resources.Res
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Lee las tablas de tallas (formato en docs/formato-tallas.md).
 *
 * Es tolerante: acepta las variantes de esquema de los JSON, ignora lo que no entiende
 * (dejando un aviso en [TablaTallas.avisos]) y una tabla vacía (`"prendas": {}`) es válida.
 * Solo lanza excepción si el texto no es JSON o no es un objeto.
 */
object CargadorTallas {
    /** Ids de las tablas incluidas como recurso (`files/tallas/<id>.json`). */
    val IDS: List<String> = listOf(
        "generica-eu", "zara", "pullbear", "bershka", "stradivarius", "massimo_dutti",
        "mango", "hm", "brownie", "scalpers", "hollister", "brandy_melville",
    )

    suspend fun cargar(id: String): TablaTallas =
        parsear(Res.readBytes("files/tallas/$id.json").decodeToString())

    suspend fun cargarTodas(): Map<String, TablaTallas> = IDS.associateWith { cargar(it) }

    fun parsear(texto: String): TablaTallas {
        val raiz = Json.parseToJsonElement(texto)
        require(raiz is JsonObject) { "La tabla de tallas debe ser un objeto JSON" }
        val avisos = mutableListOf<String>()
        val prendas = LinkedHashMap<String, TablaPrenda>()
        (raiz["prendas"] as? JsonObject)?.forEach { (clave, valor) ->
            if (valor is JsonObject) {
                prendas[clave] = parsearPrenda(clave, valor, avisos)
            } else {
                avisos += "prendas.$clave: se esperaba un objeto"
            }
        }
        return TablaTallas(
            marca = raiz.texto("marca") ?: "",
            fuentes = raiz["fuente"].textos(),
            fecha = raiz.texto("fecha"),
            unidad = raiz.texto("unidad") ?: raiz.texto("unidades") ?: "cm",
            tipoValor = raiz.texto("tipo_valor"),
            tallaUnica = raiz.texto("talla_unica") == "true",
            notas = raiz["notas"].textos() + raiz["nota"].textos(),
            pendiente = raiz["pendiente"].textos(),
            equivalencias = parsearEquivalencias(raiz["equivalencias"]),
            prendas = prendas,
            avisos = avisos,
        )
    }

    private fun parsearPrenda(clave: String, objeto: JsonObject, avisos: MutableList<String>): TablaPrenda {
        val tallas = objeto.mapNotNull { (etiqueta, valor) ->
            if (valor !is JsonObject) {
                avisos += "$clave.$etiqueta: se esperaba un objeto"
                return@mapNotNull null
            }
            val medidas = LinkedHashMap<Medida, Rango>()
            for ((campo, v) in valor) {
                val medida = Medida.deClave(campo) ?: continue
                val rango = parsearRango(v)
                if (rango == null) avisos += "$clave.$etiqueta.$campo: valor no válido" else medidas[medida] = rango
            }
            val eu = (valor["eur"] ?: valor["talla_eu"]) as? JsonPrimitive
            TallaMarca(etiqueta, medidas, eu?.contentOrNull)
        }
        return TablaPrenda(clave, tallas)
    }

    /** `[min, max]`, `[v, v]`, `[v]` o un número suelto. */
    private fun parsearRango(e: JsonElement): Rango? {
        val numeros = when (e) {
            is JsonArray -> e.map { (it as? JsonPrimitive)?.doubleOrNull ?: return null }
            is JsonPrimitive -> listOf(e.doubleOrNull ?: return null)
            else -> return null
        }
        return when (numeros.size) {
            1 -> Rango(numeros[0], numeros[0])
            2 -> if (numeros[0] <= numeros[1]) Rango(numeros[0], numeros[1]) else null
            else -> null
        }
    }

    private fun parsearEquivalencias(e: JsonElement?): Map<String, List<Int>> {
        val objeto = e as? JsonObject ?: return emptyMap()
        return objeto.mapNotNull { (letra, v) ->
            val numeros = (v as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.intOrNull } ?: return@mapNotNull null
            letra to numeros
        }.toMap()
    }

    private fun JsonObject.texto(clave: String): String? = (this[clave] as? JsonPrimitive)?.contentOrNull

    /** Una cadena o una lista de cadenas. */
    private fun JsonElement?.textos(): List<String> = when (this) {
        is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> listOfNotNull(contentOrNull)
        else -> emptyList()
    }
}
