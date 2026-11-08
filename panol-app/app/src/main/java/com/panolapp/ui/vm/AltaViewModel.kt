package com.panolapp.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.panolapp.PanolApp
import com.panolapp.R
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.OpResult
import com.panolapp.domain.Reglas
import com.panolapp.domain.TabletSpecs
import com.panolapp.ui.UiMensaje
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** CU-01: alta de una tablet, con datos precargados si vienen de un QR no registrado. */
class AltaViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val panol = getApplication<PanolApp>()
    private val repo = panol.repository

    /** Datos leídos del QR (si el operador llegó escaneando). Se consumen una sola vez. */
    val prefill: TabletSpecs? = panol.altaPrefill.also { panol.altaPrefill = null }

    val serieInicial: String =
        prefill?.serie ?: Reglas.normalizarSerie(handle.get<String>("serie").orEmpty())

    var ocupado by mutableStateOf(false)
        private set

    /** Errores devueltos por la última validación del repositorio (p. ej. serie duplicada). */
    var errores by mutableStateOf<List<ErrorNegocio>>(emptyList())
        private set

    private val _altas = Channel<String>(Channel.BUFFERED)

    /** Emite la serie de cada tablet dada de alta con éxito. */
    val altas = _altas.receiveAsFlow()

    fun guardar(specs: TabletSpecs) {
        if (ocupado) return
        ocupado = true
        errores = emptyList()
        viewModelScope.launch {
            try {
                when (val resultado = repo.alta(specs)) {
                    OpResult.Ok -> {
                        val serie = Reglas.normalizarSerie(specs.serie)
                        panol.mensajes.tryEmit(UiMensaje(R.string.msg_alta_ok, listOf(serie)))
                        _altas.send(serie)
                    }
                    is OpResult.Error -> errores = resultado.errores
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errores = listOf(ErrorNegocio.ERROR_INTERNO)
            } finally {
                ocupado = false
            }
        }
    }

    fun limpiarErrores() {
        if (errores.isNotEmpty()) errores = emptyList()
    }
}
