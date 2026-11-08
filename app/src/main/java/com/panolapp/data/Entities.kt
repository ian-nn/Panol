package com.panolapp.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Empleado al que se le asocia una tablet (por legajo). */
@Entity(tableName = "empleados")
data class EmpleadoEntity(
    @PrimaryKey val legajo: String,
    val nombre: String,
)

/**
 * Una fila por tablet. El estado vigente y quién la tiene viven acá (lectura rápida);
 * la historia completa vive en [MovimientoEntity].
 *
 * Invariantes (las garantiza PanolRepository dentro de una transacción):
 *  - estado USO_LOCAL / USO_EXTERNO  => legajo_actual != null
 *  - estado EN_PANOL / SERVICIO_TECNICO => legajo_actual == null y tarea_actual == null
 *  - estado USO_EXTERNO => tarea_actual con al menos Reglas.MIN_TAREA caracteres
 */
@Entity(
    tableName = "tablets",
    foreignKeys = [
        ForeignKey(
            entity = EmpleadoEntity::class,
            parentColumns = ["legajo"],
            childColumns = ["legajo_actual"],
        ),
    ],
    indices = [Index("legajo_actual"), Index("estado")],
)
data class TabletEntity(
    @PrimaryKey val serie: String,
    val procesador: String,
    val ram: String,
    val almacenamiento: String,
    val pantalla: String,
    /** Lista de puertos serializada como arreglo JSON. */
    val puertos: String,
    /** Lista de conectividad serializada como arreglo JSON. */
    val conectividad: String,
    /** Nombre de [com.panolapp.domain.EstadoTablet]. */
    val estado: String,
    @ColumnInfo(name = "legajo_actual") val legajoActual: String?,
    @ColumnInfo(name = "tarea_actual") val tareaActual: String?,
    @ColumnInfo(name = "estado_desde") val estadoDesde: Long,
    @ColumnInfo(name = "creado_en") val creadoEn: Long,
)

/** Bitácora inmutable de todo cambio de estado: es la trazabilidad del sistema. */
@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = TabletEntity::class,
            parentColumns = ["serie"],
            childColumns = ["serie"],
        ),
    ],
    indices = [Index(value = ["serie", "fecha"])],
)
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serie: String,
    /** Nombre de [com.panolapp.domain.TipoMovimiento]. */
    val tipo: String,
    @ColumnInfo(name = "estado_origen") val estadoOrigen: String?,
    @ColumnInfo(name = "estado_destino") val estadoDestino: String,
    val legajo: String?,
    val tarea: String?,
    val observaciones: String?,
    val fecha: Long,
)

/** Resultado de un JOIN: tablet + nombre de quien la tiene. */
data class TabletConNombre(
    @Embedded val tablet: TabletEntity,
    @ColumnInfo(name = "nombre_empleado") val nombreEmpleado: String?,
)

data class MovimientoConNombre(
    @Embedded val movimiento: MovimientoEntity,
    @ColumnInfo(name = "nombre_empleado") val nombreEmpleado: String?,
)

data class ConteoEstado(
    val estado: String,
    val cantidad: Int,
)
