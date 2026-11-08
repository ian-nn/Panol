package com.panolapp.domain

/** Los 4 estados exclusivos del ciclo de vida de una tablet (consigna 2.B). */
enum class EstadoTablet {
    EN_PANOL,
    USO_LOCAL,
    USO_EXTERNO,
    SERVICIO_TECNICO;

    /** `true` si la tablet está en manos de un empleado. */
    val enUso: Boolean get() = this == USO_LOCAL || this == USO_EXTERNO

    companion object {
        /** Tolerante: un valor desconocido en la base nunca debe tumbar la app. */
        fun desdeNombre(nombre: String?): EstadoTablet =
            entries.firstOrNull { it.name == nombre } ?: EN_PANOL
    }
}

/** Dónde se usará la tablet al despacharla. */
enum class TipoUso {
    OFICINA,
    HOME_OFFICE;

    val estadoDestino: EstadoTablet
        get() = if (this == OFICINA) EstadoTablet.USO_LOCAL else EstadoTablet.USO_EXTERNO
}

/** Resultado de la verificación física al recibir una devolución (CU-03). */
enum class EstadoFisico { SIN_DANOS, CON_DANOS }

enum class TipoMovimiento {
    ALTA,
    DESPACHO_LOCAL,
    DESPACHO_EXTERNO,
    DEVOLUCION,
    ENVIO_SERVICIO,
    REINGRESO_SERVICIO;

    companion object {
        fun desdeNombre(nombre: String?): TipoMovimiento =
            entries.firstOrNull { it.name == nombre } ?: ALTA
    }
}

/** Ficha técnica: es lo que se codifica en el QR pegado en el reverso de la tablet. */
data class TabletSpecs(
    val serie: String,
    val procesador: String = "",
    val ram: String = "",
    val almacenamiento: String = "",
    val pantalla: String = "",
    val puertos: List<String> = emptyList(),
    val conectividad: List<String> = emptyList(),
) {
    /** Datos mínimos para poder dar de alta la tablet (CU-01). */
    val completa: Boolean
        get() = procesador.isNotBlank() && ram.isNotBlank() &&
            almacenamiento.isNotBlank() && pantalla.isNotBlank()
}

data class Tablet(
    val specs: TabletSpecs,
    val estado: EstadoTablet,
    val legajoActual: String?,
    val nombreActual: String?,
    val tareaActual: String?,
    /** Instante (epoch ms) en que la tablet entró en su estado actual. */
    val estadoDesde: Long,
    val creadoEn: Long,
) {
    val serie: String get() = specs.serie
}

data class Movimiento(
    val id: Long,
    val serie: String,
    val tipo: TipoMovimiento,
    val estadoOrigen: EstadoTablet?,
    val estadoDestino: EstadoTablet,
    val legajo: String?,
    val nombre: String?,
    val tarea: String?,
    val observaciones: String?,
    val fecha: Long,
)

/** Violaciones de reglas de negocio. La UI las traduce a texto (ver ui/Mappings.kt). */
enum class ErrorNegocio {
    SERIE_INVALIDA,
    SERIE_DUPLICADA,
    ESPECIFICACIONES_INCOMPLETAS,
    TABLET_NO_ENCONTRADA,
    TABLET_NO_DISPONIBLE,
    TABLET_NO_EN_USO,
    LEGAJO_INVALIDO,
    NOMBRE_INVALIDO,
    HOME_OFFICE_SIN_TAREA,
    DANOS_SIN_OBSERVACION,
    MOTIVO_REQUERIDO,
    TRANSICION_INVALIDA,
    ERROR_INTERNO,
}

sealed interface OpResult {
    data object Ok : OpResult
    data class Error(val errores: List<ErrorNegocio>) : OpResult {
        val principal: ErrorNegocio get() = errores.first()
    }
}
