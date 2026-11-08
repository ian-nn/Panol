package com.panolapp.ui.vm

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panolapp.PanolApp
import com.panolapp.domain.QrError
import com.panolapp.domain.QrParser
import com.panolapp.domain.QrResult
import com.panolapp.domain.Reglas
import com.panolapp.domain.TabletSpecs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ResultadoEscaneo {
    /** La tablet ya está registrada: se puede operar con ella. */
    data class Encontrada(val serie: String) : ResultadoEscaneo

    /** La serie es válida pero la tablet no existe todavía: hay que darla de alta (CU-01). */
    data class NoRegistrada(val serie: String) : ResultadoEscaneo
}

/**
 * Resuelve lo que llega del lector (QR) o del teclado (flujo alternativo cuando el QR está
 * dañado) y decide si la tablet existe o hay que registrarla.
 */
class EscanerViewModel(app: Application) : AndroidViewModel(app) {

    private val panol = getApplication<PanolApp>()
    private val repo = panol.repository

    /** Último problema al leer el QR; mientras exista, el lector queda en pausa. */
    var problema by mutableStateOf<QrError?>(null)
        private set

    var errorManual by mutableStateOf(false)
        private set

    var procesando by mutableStateOf(false)
        private set

    private val _resultados = Channel<ResultadoEscaneo>(Channel.BUFFERED)
    val resultados = _resultados.receiveAsFlow()

    fun alLeerQr(crudo: String?) {
        if (procesando || problema != null) return
        procesando = true
        when (val resultado = QrParser.parse(crudo)) {
            is QrResult.Invalid -> {
                problema = resultado.error
                procesando = false
            }
            is QrResult.Ok -> resolver(resultado.specs)
        }
    }

    /** Flujo alternativo: el operador tipea el número de serie impreso en la etiqueta. */
    fun buscarManual(texto: String) {
        if (procesando) return
        val serie = Reglas.normalizarSerie(texto)
        if (!Reglas.serieValida(serie)) {
            errorManual = true
            return
        }
        procesando = true
        resolver(TabletSpecs(serie = serie))
    }

    fun fallaCamara() {
        if (problema == null) problema = QrError.LECTURA_FALLIDA
    }

    fun reintentar() {
        problema = null
        procesando = false
    }

    fun limpiarErrorManual() {
        errorManual = false
    }

    private fun resolver(specs: TabletSpecs) {
        viewModelScope.launch {
            try {
                if (repo.buscarTablet(specs.serie) != null) {
                    _resultados.send(ResultadoEscaneo.Encontrada(specs.serie))
                } else {
                    panol.altaPrefill = specs
                    _resultados.send(ResultadoEscaneo.NoRegistrada(specs.serie))
                }
                // Se deja `procesando` en true: la pantalla navega y este ViewModel se descarta.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problema = QrError.LECTURA_FALLIDA
                procesando = false
            }
        }
    }
}
