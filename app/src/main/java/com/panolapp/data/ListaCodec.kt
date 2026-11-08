package com.panolapp.data

import org.json.JSONArray
import org.json.JSONException

/** Guarda listas de texto (puertos, conectividad) como un arreglo JSON en una sola columna. */
object ListaCodec {
    fun codificar(lista: List<String>): String = JSONArray(lista).toString()

    fun decodificar(texto: String?): List<String> {
        if (texto.isNullOrBlank()) return emptyList()
        return try {
            val arreglo = JSONArray(texto)
            (0 until arreglo.length()).map { arreglo.optString(it) }.filter { it.isNotBlank() }
        } catch (e: JSONException) {
            emptyList()
        }
    }
}
