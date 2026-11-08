package com.panolapp.ui.vm

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.panolapp.PanolApp
import com.panolapp.R
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.EstadoFisico
import com.panolapp.domain.Movimiento
import com.panolapp.domain.OpResult
import com.panolapp.domain.Reglas
import com.panolapp.domain.Tablet
import com.panolapp.domain.TipoUso
import com.panolapp.ui.UiMensaje
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TabletUi {
    data object Cargando : TabletUi
    data object NoEncontrada : TabletUi
    data class Lista(val tablet: Tablet) : TabletUi
}

sealed interface EventoOperacion {
    data object Exito : EventoOperacion
    data class Fallo(val errores: List<ErrorNegocio>) : EventoOperacion
}

/**
 * ViewModel de todas las pantallas que operan sobre UNA tablet (ficha, despacho, devolución,
 * etiqueta QR). La serie llega como argumento de navegación.
 */
class TabletViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val panol = getApplication<PanolApp>()
    private val repo = panol.repository

    val serie: String = Reglas.normalizarSerie(checkNotNull(handle.get<String>("serie")))

    val tablet: StateFlow<TabletUi> = repo.observarTablet(serie)
        .map<Tablet?, TabletUi> { if (it == null) TabletUi.NoEncontrada else TabletUi.Lista(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TabletUi.Cargando)

    val historial: StateFlow<List<Movimiento>> = repo.observarHistorial(serie)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Evita doble toque mientras una operación está en curso. */
    var ocupado by mutableStateOf(false)
        private set

    private val _eventos = Channel<EventoOperacion>(Channel.BUFFERED)
    val eventos = _eventos.receiveAsFlow()

    /** Para autocompletar el nombre cuando el legajo ya fue cargado antes. */
    suspend fun nombreDeEmpleado(legajo: String): String? = repo.nombreDeEmpleado(legajo)

    fun despachar(uso: TipoUso, legajo: String, nombre: String, tarea: String) {
        ejecutar(R.string.msg_despacho_ok) { repo.despachar(serie, uso, legajo, nombre, tarea) }
    }

    fun devolver(fisico: EstadoFisico, observaciones: String) {
        val mensaje = if (fisico == EstadoFisico.SIN_DANOS) {
            R.string.msg_devolucion_ok
        } else {
            R.string.msg_devolucion_servicio
        }
        ejecutar(mensaje) { repo.devolver(serie, fisico, observaciones) }
    }

    fun enviarAServicio(motivo: String) {
        ejecutar(R.string.msg_envio_servicio_ok) { repo.enviarAServicio(serie, motivo) }
    }

    fun reingresarDeServicio(observaciones: String) {
        ejecutar(R.string.msg_reingreso_ok) { repo.reingresarDeServicio(serie, observaciones) }
    }

    private fun ejecutar(@StringRes mensajeOk: Int, operacion: suspend () -> OpResult) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            try {
                when (val resultado = operacion()) {
                    OpResult.Ok -> {
                        panol.mensajes.tryEmit(UiMensaje(mensajeOk, listOf(serie)))
                        _eventos.send(EventoOperacion.Exito)
                    }
                    is OpResult.Error -> _eventos.send(EventoOperacion.Fallo(resultado.errores))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _eventos.send(EventoOperacion.Fallo(listOf(ErrorNegocio.ERROR_INTERNO)))
            } finally {
                ocupado = false
            }
        }
    }
}
