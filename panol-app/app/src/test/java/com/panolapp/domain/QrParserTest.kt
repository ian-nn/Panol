package com.panolapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrParserTest {

    private fun ok(raw: String?): TabletSpecs {
        val r = QrParser.parse(raw)
        assertTrue("se esperaba QR válido para: $raw", r is QrResult.Ok)
        return (r as QrResult.Ok).specs
    }

    private fun error(raw: String?): QrError {
        val r = QrParser.parse(raw)
        assertTrue("se esperaba QR inválido para: $raw", r is QrResult.Invalid)
        return (r as QrResult.Invalid).error
    }

    @Test
    fun json_completo() {
        val specs = ok(
            """{"v":1,"serie":"tab-0001","procesador":"Snapdragon 680","ram":"4 GB",
               "almacenamiento":"64 GB","pantalla":"10.1\"",
               "puertos":["USB-C","Jack 3.5 mm"],"conectividad":["Wi-Fi","Bluetooth","4G"]}""",
        )
        assertEquals("TAB-0001", specs.serie)
        assertEquals("Snapdragon 680", specs.procesador)
        assertEquals("4 GB", specs.ram)
        assertEquals("64 GB", specs.almacenamiento)
        assertEquals("10.1\"", specs.pantalla)
        assertEquals(listOf("USB-C", "Jack 3.5 mm"), specs.puertos)
        assertEquals(listOf("Wi-Fi", "Bluetooth", "4G"), specs.conectividad)
        assertTrue(specs.completa)
    }

    @Test
    fun json_con_alias_y_listas_como_texto() {
        val specs = ok("""{"sn":"X-100","cpu":"A15","puertos":"USB-C; Jack 3.5","conectividad":"Wi-Fi, 5G"}""")
        assertEquals("X-100", specs.serie)
        assertEquals("A15", specs.procesador)
        assertEquals(listOf("USB-C", "Jack 3.5"), specs.puertos)
        assertEquals(listOf("Wi-Fi", "5G"), specs.conectividad)
    }

    @Test
    fun delimitado_con_y_sin_encabezado() {
        val a = ok("PANOL|TAB-0002|Helio G99|6 GB|128 GB|11\"|USB-C,Jack 3.5|Wi-Fi,Bluetooth")
        assertEquals("TAB-0002", a.serie)
        assertEquals("11\"", a.pantalla)
        assertEquals(listOf("USB-C", "Jack 3.5"), a.puertos)
        val b = ok("TAB-0003|Helio G99|6 GB|128 GB|11\"|USB-C|Wi-Fi")
        assertEquals("TAB-0003", b.serie)
        assertEquals("Helio G99", b.procesador)
        assertEquals(listOf("Wi-Fi"), b.conectividad)
    }

    @Test
    fun solo_serie_es_flujo_de_emergencia() {
        val specs = ok("  tab-0004 ")
        assertEquals("TAB-0004", specs.serie)
        assertEquals(false, specs.completa)
    }

    @Test
    fun qr_ajeno_o_danado_se_rechaza() {
        assertEquals(QrError.VACIO, error(null))
        assertEquals(QrError.VACIO, error("   "))
        assertEquals(QrError.FORMATO_DESCONOCIDO, error("https://ejemplo.com/promo"))
        assertEquals(QrError.FORMATO_DESCONOCIDO, error("hola"))        // sin dígito: no es una serie
        assertEquals(QrError.FORMATO_DESCONOCIDO, error("{\"serie\":\"TAB-1\""))  // JSON truncado
        assertEquals(QrError.SIN_SERIE, error("""{"procesador":"A15"}"""))
        assertEquals(QrError.SERIE_INVALIDA, error("""{"serie":"a b"}"""))
        assertEquals(QrError.SERIE_INVALIDA, error("PANOL|??|a|b|c|d"))
    }

    @Test
    fun ida_y_vuelta_json() {
        val original = TabletSpecs(
            serie = "TAB-0005",
            procesador = "Snapdragon 680",
            ram = "4 GB",
            almacenamiento = "64 GB",
            pantalla = "10.1\"",
            puertos = listOf("USB-C", "Jack 3.5 mm"),
            conectividad = listOf("Wi-Fi", "4G"),
        )
        assertEquals(original, ok(QrParser.aJson(original)))
    }

    @Test
    fun el_contenido_generado_es_solo_ascii() {
        val json = QrParser.aJson(TabletSpecs("TAB-0006", "Procesador Ñandú", "4 GB", "64 GB", "10\"", listOf("Cámara")))
        assertTrue(json.all { it.code in 32..126 })
        assertEquals("Procesador Nandu", ok(json).procesador)
        assertEquals("Camara", QrParser.ascii("Cámara"))
    }
}
