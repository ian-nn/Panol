package com.panolapp.domain

import java.util.Locale

/**
 * Reglas de negocio puras (sin Android): se reutilizan desde la UI (validación en vivo),
 * desde el repositorio (validación definitiva dentro de la transacción) y desde los tests.
 */
object Reglas {
    const val MIN_TAREA = 5
    const val MIN_MOTIVO = 3
    const val MIN_NOMBRE = 2

    // Serie: letras/dígitos, guiones, puntos y guion bajo; 3 a 40 caracteres.
    private val SERIE_REGEX = Regex("^[A-Z0-9][A-Z0-9._-]{2,39}$")
    private val LEGAJO_REGEX = Regex("^[A-Za-z0-9]{1,10}$")

    /**
     * Matriz de transiciones permitidas entre estados (consigna 2.B).
     * Una tablet nunca puede pasar directo de "En uso" a otro "En uso".
     */
    private val TRANSICIONES: Map<EstadoTablet, Set<EstadoTablet>> = mapOf(
        EstadoTablet.EN_PANOL to setOf(
            EstadoTablet.USO_LOCAL, EstadoTablet.USO_EXTERNO, EstadoTablet.SERVICIO_TECNICO,
        ),
        EstadoTablet.USO_LOCAL to setOf(EstadoTablet.EN_PANOL, EstadoTablet.SERVICIO_TECNICO),
        EstadoTablet.USO_EXTERNO to setOf(EstadoTablet.EN_PANOL, EstadoTablet.SERVICIO_TECNICO),
        EstadoTablet.SERVICIO_TECNICO to setOf(EstadoTablet.EN_PANOL),
    )

    fun normalizarSerie(raw: String): String = raw.trim().uppercase(Locale.ROOT)

    fun serieValida(serie: String): Boolean = SERIE_REGEX.matches(serie)

    fun legajoValido(legajo: String): Boolean = LEGAJO_REGEX.matches(legajo.trim())

    fun tareaValida(tarea: String): Boolean = tarea.trim().length >= MIN_TAREA

    fun transicionPermitida(origen: EstadoTablet, destino: EstadoTablet): Boolean =
        TRANSICIONES[origen]?.contains(destino) == true

    /** CU-01: alta de una tablet nueva. */
    fun validarAlta(specs: TabletSpecs): List<ErrorNegocio> = buildList {
        if (!serieValida(specs.serie)) add(ErrorNegocio.SERIE_INVALIDA)
        if (!specs.completa) add(ErrorNegocio.ESPECIFICACIONES_INCOMPLETAS)
    }

    /**
     * CU-02: despacho. Regla clave de la consigna: Home Office exige una tarea asignada
     * que lo justifique; si falta, se devuelve [ErrorNegocio.HOME_OFFICE_SIN_TAREA] para que
     * la UI muestre la alerta visual.
     */
    fun validarDespacho(
        estado: EstadoTablet,
        uso: TipoUso,
        legajo: String,
        nombre: String,
        tarea: String,
    ): List<ErrorNegocio> {
        if (estado != EstadoTablet.EN_PANOL) return listOf(ErrorNegocio.TABLET_NO_DISPONIBLE)
        return buildList {
            if (!legajoValido(legajo)) add(ErrorNegocio.LEGAJO_INVALIDO)
            if (nombre.trim().length < MIN_NOMBRE) add(ErrorNegocio.NOMBRE_INVALIDO)
            if (uso == TipoUso.HOME_OFFICE && !tareaValida(tarea)) {
                add(ErrorNegocio.HOME_OFFICE_SIN_TAREA)
            }
        }
    }

    /** CU-03: devolución. Si vuelve con daños, la observación es obligatoria. */
    fun validarDevolucion(
        estado: EstadoTablet,
        fisico: EstadoFisico,
        observaciones: String,
    ): List<ErrorNegocio> {
        if (!estado.enUso) return listOf(ErrorNegocio.TABLET_NO_EN_USO)
        return buildList {
            if (fisico == EstadoFisico.CON_DANOS && observaciones.trim().length < MIN_MOTIVO) {
                add(ErrorNegocio.DANOS_SIN_OBSERVACION)
            }
        }
    }

    /** Envío a servicio técnico desde el pañol (rotura / actualización). */
    fun validarEnvioServicio(estado: EstadoTablet, motivo: String): List<ErrorNegocio> = buildList {
        if (!transicionPermitida(estado, EstadoTablet.SERVICIO_TECNICO) || estado.enUso) {
            add(ErrorNegocio.TRANSICION_INVALIDA)
        } else if (motivo.trim().length < MIN_MOTIVO) {
            add(ErrorNegocio.MOTIVO_REQUERIDO)
        }
    }

    /** Vuelta del servicio técnico al pañol. */
    fun validarReingreso(estado: EstadoTablet): List<ErrorNegocio> = buildList {
        if (estado != EstadoTablet.SERVICIO_TECNICO) add(ErrorNegocio.TRANSICION_INVALIDA)
    }
}
