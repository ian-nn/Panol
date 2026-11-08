package com.panolapp.domain

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.Normalizer

enum class QrError { VACIO, FORMATO_DESCONOCIDO, SIN_SERIE, SERIE_INVALIDA, LECTURA_FALLIDA }

sealed interface QrResult {
    data class Ok(val specs: TabletSpecs) : QrResult
    data class Invalid(val error: QrError) : QrResult
}

/**
 * Codifica/decodifica el contenido del QR de una tablet (consigna 2.A).
 *
 * Formatos aceptados al leer:
 *  1. JSON:        {"v":1,"serie":"TAB-0001","procesador":"...","ram":"4 GB",...}
 *  2. Delimitado:  PANOL|serie|procesador|ram|almacenamiento|pantalla|puertos|conectividad
 *                  (puertos y conectividad separados por coma o punto y coma)
 *  3. Solo la serie (p. ej. una etiqueta vieja): "TAB-0001". Sirve como flujo de emergencia.
 *
 * Al generar se usa siempre JSON.
 */
object QrParser {
    const val VERSION = 1
    private const val ENCABEZADO = "PANOL"

    fun parse(raw: String?): QrResult {
        val texto = raw?.trim().orEmpty()
        if (texto.isEmpty()) return QrResult.Invalid(QrError.VACIO)
        return when {
            texto.startsWith("{") -> parseJson(texto)
            texto.contains('|') -> parseDelimitado(texto)
            else -> parseSerieSola(texto)
        }
    }

    private fun parseJson(texto: String): QrResult {
        val json = try {
            JSONObject(texto)
        } catch (e: JSONException) {
            return QrResult.Invalid(QrError.FORMATO_DESCONOCIDO)
        }
        val serie = Reglas.normalizarSerie(json.texto("serie", "numero_serie", "numeroSerie", "sn", "serial"))
        val error = validarSerie(serie)
        if (error != null) return QrResult.Invalid(error)
        return QrResult.Ok(
            TabletSpecs(
                serie = serie,
                procesador = json.texto("procesador", "cpu"),
                ram = json.texto("ram", "memoria"),
                almacenamiento = json.texto("almacenamiento", "storage", "rom"),
                pantalla = json.texto("pantalla", "display", "screen"),
                puertos = json.lista("puertos", "ports"),
                conectividad = json.lista("conectividad", "connectivity"),
            ),
        )
    }

    private fun parseDelimitado(texto: String): QrResult {
        val partes = texto.split('|').map { it.trim() }.toMutableList()
        if (partes.firstOrNull().equals(ENCABEZADO, ignoreCase = true)) partes.removeAt(0)
        val serie = Reglas.normalizarSerie(partes.getOrElse(0) { "" })
        val error = validarSerie(serie)
        if (error != null) return QrResult.Invalid(error)
        return QrResult.Ok(
            TabletSpecs(
                serie = serie,
                procesador = partes.getOrElse(1) { "" },
                ram = partes.getOrElse(2) { "" },
                almacenamiento = partes.getOrElse(3) { "" },
                pantalla = partes.getOrElse(4) { "" },
                puertos = separar(partes.getOrElse(5) { "" }),
                conectividad = separar(partes.getOrElse(6) { "" }),
            ),
        )
    }

    /** Un QR cualquiera (una URL, un texto) no debe confundirse con una serie: exige un dígito. */
    private fun parseSerieSola(texto: String): QrResult {
        val serie = Reglas.normalizarSerie(texto)
        return if (Reglas.serieValida(serie) && serie.any { it.isDigit() }) {
            QrResult.Ok(TabletSpecs(serie = serie))
        } else {
            QrResult.Invalid(QrError.FORMATO_DESCONOCIDO)
        }
    }

    private fun validarSerie(serie: String): QrError? = when {
        serie.isEmpty() -> QrError.SIN_SERIE
        !Reglas.serieValida(serie) -> QrError.SERIE_INVALIDA
        else -> null
    }

    /** Genera el contenido del QR. Solo ASCII: evita problemas de codificación al escanear. */
    fun aJson(specs: TabletSpecs): String = JSONObject().apply {
        put("v", VERSION)
        put("serie", ascii(specs.serie))
        put("procesador", ascii(specs.procesador))
        put("ram", ascii(specs.ram))
        put("almacenamiento", ascii(specs.almacenamiento))
        put("pantalla", ascii(specs.pantalla))
        put("puertos", JSONArray(specs.puertos.map { ascii(it) }))
        put("conectividad", JSONArray(specs.conectividad.map { ascii(it) }))
    }.toString()

    /** Quita tildes/diacríticos y cualquier carácter no imprimible ASCII. */
    fun ascii(texto: String): String =
        Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .filter { it.code in 32..126 }

    private fun JSONObject.texto(vararg claves: String): String {
        for (clave in claves) {
            if (has(clave) && !isNull(clave)) {
                val valor = opt(clave)?.toString()?.trim().orEmpty()
                if (valor.isNotEmpty()) return valor
            }
        }
        return ""
    }

    private fun JSONObject.lista(vararg claves: String): List<String> {
        for (clave in claves) {
            if (!has(clave) || isNull(clave)) continue
            return when (val valor = opt(clave)) {
                is JSONArray -> (0 until valor.length())
                    .map { valor.optString(it).trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                else -> separar(valor?.toString().orEmpty())
            }
        }
        return emptyList()
    }

    private fun separar(texto: String): List<String> =
        texto.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
}
