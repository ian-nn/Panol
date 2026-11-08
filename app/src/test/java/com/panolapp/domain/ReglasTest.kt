package com.panolapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasTest {

    @Test
    fun serie_valida_acepta_formatos_tipicos_y_rechaza_basura() {
        assertTrue(Reglas.serieValida("TAB-0001"))
        assertTrue(Reglas.serieValida("SN.2024_X9"))
        assertFalse(Reglas.serieValida("AB"))
        assertFalse(Reglas.serieValida("tab-0001")) // se normaliza antes de validar
        assertFalse(Reglas.serieValida("TAB 0001"))
        assertFalse(Reglas.serieValida("https://ejemplo.com"))
        assertEquals("TAB-0001", Reglas.normalizarSerie("  tab-0001 "))
    }

    @Test
    fun home_office_sin_tarea_es_rechazado() {
        val errores = Reglas.validarDespacho(
            EstadoTablet.EN_PANOL, TipoUso.HOME_OFFICE, "1234", "Ana Gómez", "",
        )
        assertEquals(listOf(ErrorNegocio.HOME_OFFICE_SIN_TAREA), errores)
    }

    @Test
    fun home_office_con_tarea_muy_corta_es_rechazado() {
        val errores = Reglas.validarDespacho(
            EstadoTablet.EN_PANOL, TipoUso.HOME_OFFICE, "1234", "Ana Gómez", "  ab ",
        )
        assertTrue(ErrorNegocio.HOME_OFFICE_SIN_TAREA in errores)
    }

    @Test
    fun home_office_con_tarea_valida_es_aceptado() {
        val errores = Reglas.validarDespacho(
            EstadoTablet.EN_PANOL, TipoUso.HOME_OFFICE, "1234", "Ana Gómez", "Relevamiento en planta Sur",
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun oficina_no_exige_tarea() {
        val errores = Reglas.validarDespacho(
            EstadoTablet.EN_PANOL, TipoUso.OFICINA, "1234", "Ana Gómez", "",
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun solo_se_despacha_desde_el_panol() {
        for (estado in EstadoTablet.entries.filter { it != EstadoTablet.EN_PANOL }) {
            val errores = Reglas.validarDespacho(estado, TipoUso.OFICINA, "1234", "Ana Gómez", "")
            assertEquals(listOf(ErrorNegocio.TABLET_NO_DISPONIBLE), errores)
        }
    }

    @Test
    fun legajo_y_nombre_son_obligatorios() {
        val errores = Reglas.validarDespacho(
            EstadoTablet.EN_PANOL, TipoUso.OFICINA, "  ", "A", "",
        )
        assertTrue(ErrorNegocio.LEGAJO_INVALIDO in errores)
        assertTrue(ErrorNegocio.NOMBRE_INVALIDO in errores)
        assertFalse(Reglas.legajoValido("12 34"))
        assertTrue(Reglas.legajoValido(" 12345 "))
    }

    @Test
    fun devolucion_con_danos_exige_observacion() {
        val sin = Reglas.validarDevolucion(EstadoTablet.USO_LOCAL, EstadoFisico.CON_DANOS, " ")
        assertEquals(listOf(ErrorNegocio.DANOS_SIN_OBSERVACION), sin)
        val con = Reglas.validarDevolucion(EstadoTablet.USO_EXTERNO, EstadoFisico.CON_DANOS, "Pantalla rota")
        assertTrue(con.isEmpty())
        val ok = Reglas.validarDevolucion(EstadoTablet.USO_EXTERNO, EstadoFisico.SIN_DANOS, "")
        assertTrue(ok.isEmpty())
    }

    @Test
    fun solo_se_devuelve_una_tablet_en_uso() {
        for (estado in listOf(EstadoTablet.EN_PANOL, EstadoTablet.SERVICIO_TECNICO)) {
            val errores = Reglas.validarDevolucion(estado, EstadoFisico.SIN_DANOS, "")
            assertEquals(listOf(ErrorNegocio.TABLET_NO_EN_USO), errores)
        }
    }

    @Test
    fun matriz_de_transiciones() {
        assertTrue(Reglas.transicionPermitida(EstadoTablet.EN_PANOL, EstadoTablet.USO_EXTERNO))
        assertTrue(Reglas.transicionPermitida(EstadoTablet.USO_LOCAL, EstadoTablet.EN_PANOL))
        assertTrue(Reglas.transicionPermitida(EstadoTablet.SERVICIO_TECNICO, EstadoTablet.EN_PANOL))
        assertFalse(Reglas.transicionPermitida(EstadoTablet.USO_LOCAL, EstadoTablet.USO_EXTERNO))
        assertFalse(Reglas.transicionPermitida(EstadoTablet.SERVICIO_TECNICO, EstadoTablet.USO_LOCAL))
        assertFalse(Reglas.transicionPermitida(EstadoTablet.EN_PANOL, EstadoTablet.EN_PANOL))
    }

    @Test
    fun envio_a_servicio_y_reingreso() {
        assertTrue(Reglas.validarEnvioServicio(EstadoTablet.EN_PANOL, "No enciende").isEmpty())
        assertEquals(
            listOf(ErrorNegocio.MOTIVO_REQUERIDO),
            Reglas.validarEnvioServicio(EstadoTablet.EN_PANOL, ""),
        )
        // Una tablet en uso primero se devuelve; no puede ir directo a servicio desde esta acción.
        assertEquals(
            listOf(ErrorNegocio.TRANSICION_INVALIDA),
            Reglas.validarEnvioServicio(EstadoTablet.USO_LOCAL, "Rota"),
        )
        assertEquals(
            listOf(ErrorNegocio.TRANSICION_INVALIDA),
            Reglas.validarEnvioServicio(EstadoTablet.SERVICIO_TECNICO, "Rota"),
        )
        assertTrue(Reglas.validarReingreso(EstadoTablet.SERVICIO_TECNICO).isEmpty())
        assertEquals(
            listOf(ErrorNegocio.TRANSICION_INVALIDA),
            Reglas.validarReingreso(EstadoTablet.EN_PANOL),
        )
    }

    @Test
    fun alta_exige_serie_valida_y_especificaciones() {
        val completa = TabletSpecs("TAB-0001", "Snapdragon 680", "4 GB", "64 GB", "10.1\"")
        assertTrue(Reglas.validarAlta(completa).isEmpty())
        val incompleta = TabletSpecs("TAB-0001", "Snapdragon 680")
        assertEquals(listOf(ErrorNegocio.ESPECIFICACIONES_INCOMPLETAS), Reglas.validarAlta(incompleta))
        val mala = TabletSpecs("x", "a", "b", "c", "d")
        assertEquals(listOf(ErrorNegocio.SERIE_INVALIDA), Reglas.validarAlta(mala))
    }

    @Test
    fun estado_desde_nombre_es_tolerante() {
        assertEquals(EstadoTablet.USO_LOCAL, EstadoTablet.desdeNombre("USO_LOCAL"))
        assertEquals(EstadoTablet.EN_PANOL, EstadoTablet.desdeNombre("ALGO_RARO"))
        assertEquals(EstadoTablet.EN_PANOL, EstadoTablet.desdeNombre(null))
        assertEquals(EstadoTablet.USO_EXTERNO, TipoUso.HOME_OFFICE.estadoDestino)
        assertEquals(EstadoTablet.USO_LOCAL, TipoUso.OFICINA.estadoDestino)
    }
}
