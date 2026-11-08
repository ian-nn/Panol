package com.panolapp.data

import androidx.room.withTransaction
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.EstadoFisico
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.Movimiento
import com.panolapp.domain.OpResult
import com.panolapp.domain.Reglas
import com.panolapp.domain.Tablet
import com.panolapp.domain.TabletSpecs
import com.panolapp.domain.TipoMovimiento
import com.panolapp.domain.TipoUso
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * Única puerta de entrada a los datos. Cada operación de negocio corre en UNA transacción:
 * se relee la tablet, se revalidan las reglas con el estado real de la base (no el de la
 * pantalla, que pudo quedar desactualizado) y recién entonces se escribe.
 */
class PanolRepository(
    private val db: PanolDatabase,
    private val ahora: () -> Long = { System.currentTimeMillis() },
) {
    private val dao: PanolDao get() = db.dao()

    // ---------------------------------------------------------------- lecturas

    fun observarConteos(): Flow<Map<EstadoTablet, Int>> =
        dao.observarConteos().map { filas ->
            EstadoTablet.entries.associateWith { estado ->
                filas.firstOrNull { it.estado == estado.name }?.cantidad ?: 0
            }
        }

    fun observarInventario(estado: EstadoTablet?, consulta: String): Flow<List<Tablet>> =
        dao.observarInventario(estado?.name, consulta.trim()).map { filas -> filas.map { it.aDominio() } }

    fun observarTablet(serie: String): Flow<Tablet?> =
        dao.observarTablet(Reglas.normalizarSerie(serie)).map { it?.aDominio() }

    fun observarHistorial(serie: String, limite: Int = 50): Flow<List<Movimiento>> =
        dao.observarMovimientos(Reglas.normalizarSerie(serie), limite).map { filas ->
            filas.map { it.aDominio() }
        }

    suspend fun buscarTablet(serie: String): Tablet? =
        dao.buscarTablet(Reglas.normalizarSerie(serie))?.aDominio()

    suspend fun nombreDeEmpleado(legajo: String): String? {
        val limpio = legajo.trim().uppercase(Locale.ROOT)
        return if (Reglas.legajoValido(limpio)) dao.nombreDeEmpleado(limpio) else null
    }

    // ---------------------------------------------------------------- escrituras

    /** CU-01: alta de una tablet nueva en el pañol. */
    suspend fun alta(specs: TabletSpecs): OpResult = transaccion {
        val limpia = specs.limpiar()
        val errores = Reglas.validarAlta(limpia)
        if (errores.isNotEmpty()) return@transaccion OpResult.Error(errores)
        if (dao.tablet(limpia.serie) != null) {
            return@transaccion fallo(ErrorNegocio.SERIE_DUPLICADA)
        }
        val t = ahora()
        dao.insertarTablet(
            TabletEntity(
                serie = limpia.serie,
                procesador = limpia.procesador,
                ram = limpia.ram,
                almacenamiento = limpia.almacenamiento,
                pantalla = limpia.pantalla,
                puertos = ListaCodec.codificar(limpia.puertos),
                conectividad = ListaCodec.codificar(limpia.conectividad),
                estado = EstadoTablet.EN_PANOL.name,
                legajoActual = null,
                tareaActual = null,
                estadoDesde = t,
                creadoEn = t,
            ),
        )
        dao.insertarMovimiento(
            MovimientoEntity(
                serie = limpia.serie,
                tipo = TipoMovimiento.ALTA.name,
                estadoOrigen = null,
                estadoDestino = EstadoTablet.EN_PANOL.name,
                legajo = null,
                tarea = null,
                observaciones = null,
                fecha = t,
            ),
        )
        OpResult.Ok
    }

    /** CU-02: despacho a un empleado (oficina u Home Office con tarea). */
    suspend fun despachar(
        serie: String,
        uso: TipoUso,
        legajo: String,
        nombre: String,
        tarea: String,
    ): OpResult = transaccion {
        val actual = dao.tablet(Reglas.normalizarSerie(serie))
            ?: return@transaccion fallo(ErrorNegocio.TABLET_NO_ENCONTRADA)
        val estadoActual = EstadoTablet.desdeNombre(actual.estado)
        val legajoLimpio = legajo.trim().uppercase(Locale.ROOT)
        val nombreLimpio = nombre.trim()
        val tareaLimpia = tarea.trim().ifEmpty { null }

        val errores = Reglas.validarDespacho(estadoActual, uso, legajoLimpio, nombreLimpio, tarea)
        if (errores.isNotEmpty()) return@transaccion OpResult.Error(errores)

        val t = ahora()
        val destino = uso.estadoDestino
        dao.guardarEmpleado(EmpleadoEntity(legajoLimpio, nombreLimpio))
        dao.actualizarTablet(
            actual.copy(
                estado = destino.name,
                legajoActual = legajoLimpio,
                tareaActual = tareaLimpia,
                estadoDesde = t,
            ),
        )
        dao.insertarMovimiento(
            MovimientoEntity(
                serie = actual.serie,
                tipo = if (uso == TipoUso.OFICINA) {
                    TipoMovimiento.DESPACHO_LOCAL.name
                } else {
                    TipoMovimiento.DESPACHO_EXTERNO.name
                },
                estadoOrigen = estadoActual.name,
                estadoDestino = destino.name,
                legajo = legajoLimpio,
                tarea = tareaLimpia,
                observaciones = null,
                fecha = t,
            ),
        )
        OpResult.Ok
    }

    /** CU-03: devolución. Sin daños vuelve al pañol; con daños pasa a servicio técnico. */
    suspend fun devolver(serie: String, fisico: EstadoFisico, observaciones: String): OpResult =
        transaccion {
            val actual = dao.tablet(Reglas.normalizarSerie(serie))
                ?: return@transaccion fallo(ErrorNegocio.TABLET_NO_ENCONTRADA)
            val estadoActual = EstadoTablet.desdeNombre(actual.estado)
            val errores = Reglas.validarDevolucion(estadoActual, fisico, observaciones)
            if (errores.isNotEmpty()) return@transaccion OpResult.Error(errores)

            val destino = if (fisico == EstadoFisico.SIN_DANOS) {
                EstadoTablet.EN_PANOL
            } else {
                EstadoTablet.SERVICIO_TECNICO
            }
            val t = ahora()
            dao.actualizarTablet(
                actual.copy(
                    estado = destino.name,
                    legajoActual = null,
                    tareaActual = null,
                    estadoDesde = t,
                ),
            )
            dao.insertarMovimiento(
                MovimientoEntity(
                    serie = actual.serie,
                    tipo = TipoMovimiento.DEVOLUCION.name,
                    estadoOrigen = estadoActual.name,
                    estadoDestino = destino.name,
                    legajo = actual.legajoActual, // quién la devolvió
                    tarea = actual.tareaActual,
                    observaciones = observaciones.trim().ifEmpty { null },
                    fecha = t,
                ),
            )
            OpResult.Ok
        }

    /** Envío manual a servicio técnico de una tablet que está en el pañol. */
    suspend fun enviarAServicio(serie: String, motivo: String): OpResult = transaccion {
        val actual = dao.tablet(Reglas.normalizarSerie(serie))
            ?: return@transaccion fallo(ErrorNegocio.TABLET_NO_ENCONTRADA)
        val estadoActual = EstadoTablet.desdeNombre(actual.estado)
        val errores = Reglas.validarEnvioServicio(estadoActual, motivo)
        if (errores.isNotEmpty()) return@transaccion OpResult.Error(errores)

        val t = ahora()
        dao.actualizarTablet(actual.copy(estado = EstadoTablet.SERVICIO_TECNICO.name, estadoDesde = t))
        dao.insertarMovimiento(
            MovimientoEntity(
                serie = actual.serie,
                tipo = TipoMovimiento.ENVIO_SERVICIO.name,
                estadoOrigen = estadoActual.name,
                estadoDestino = EstadoTablet.SERVICIO_TECNICO.name,
                legajo = null,
                tarea = null,
                observaciones = motivo.trim(),
                fecha = t,
            ),
        )
        OpResult.Ok
    }

    /** Vuelta del servicio técnico al pañol. */
    suspend fun reingresarDeServicio(serie: String, observaciones: String): OpResult = transaccion {
        val actual = dao.tablet(Reglas.normalizarSerie(serie))
            ?: return@transaccion fallo(ErrorNegocio.TABLET_NO_ENCONTRADA)
        val estadoActual = EstadoTablet.desdeNombre(actual.estado)
        val errores = Reglas.validarReingreso(estadoActual)
        if (errores.isNotEmpty()) return@transaccion OpResult.Error(errores)

        val t = ahora()
        dao.actualizarTablet(actual.copy(estado = EstadoTablet.EN_PANOL.name, estadoDesde = t))
        dao.insertarMovimiento(
            MovimientoEntity(
                serie = actual.serie,
                tipo = TipoMovimiento.REINGRESO_SERVICIO.name,
                estadoOrigen = estadoActual.name,
                estadoDestino = EstadoTablet.EN_PANOL.name,
                legajo = null,
                tarea = null,
                observaciones = observaciones.trim().ifEmpty { null },
                fecha = t,
            ),
        )
        OpResult.Ok
    }

    // ---------------------------------------------------------------- internos

    private fun fallo(e: ErrorNegocio): OpResult = OpResult.Error(listOf(e))

    /**
     * Ejecuta [bloque] en una transacción de Room. Si algo lanza una excepción, se hace rollback
     * y se informa [ErrorNegocio.ERROR_INTERNO] en vez de cerrar la app.
     */
    private suspend fun transaccion(bloque: suspend () -> OpResult): OpResult =
        try {
            db.withTransaction(bloque)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallo(ErrorNegocio.ERROR_INTERNO)
        }

    private fun TabletSpecs.limpiar(): TabletSpecs = copy(
        serie = Reglas.normalizarSerie(serie),
        procesador = procesador.trim(),
        ram = ram.trim(),
        almacenamiento = almacenamiento.trim(),
        pantalla = pantalla.trim(),
        puertos = puertos.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
        conectividad = conectividad.map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
    )
}

// -------------------------------------------------------------------- mapeos

internal fun TabletConNombre.aDominio(): Tablet = Tablet(
    specs = TabletSpecs(
        serie = tablet.serie,
        procesador = tablet.procesador,
        ram = tablet.ram,
        almacenamiento = tablet.almacenamiento,
        pantalla = tablet.pantalla,
        puertos = ListaCodec.decodificar(tablet.puertos),
        conectividad = ListaCodec.decodificar(tablet.conectividad),
    ),
    estado = EstadoTablet.desdeNombre(tablet.estado),
    legajoActual = tablet.legajoActual,
    nombreActual = nombreEmpleado,
    tareaActual = tablet.tareaActual,
    estadoDesde = tablet.estadoDesde,
    creadoEn = tablet.creadoEn,
)

internal fun MovimientoConNombre.aDominio(): Movimiento = Movimiento(
    id = movimiento.id,
    serie = movimiento.serie,
    tipo = TipoMovimiento.desdeNombre(movimiento.tipo),
    estadoOrigen = movimiento.estadoOrigen?.let { EstadoTablet.desdeNombre(it) },
    estadoDestino = EstadoTablet.desdeNombre(movimiento.estadoDestino),
    legajo = movimiento.legajo,
    nombre = nombreEmpleado,
    tarea = movimiento.tarea,
    observaciones = movimiento.observaciones,
    fecha = movimiento.fecha,
)
