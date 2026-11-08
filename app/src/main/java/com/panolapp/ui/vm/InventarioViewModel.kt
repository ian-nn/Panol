package com.panolapp.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.panolapp.PanolApp
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.Tablet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class InventarioViewModel(app: Application, handle: SavedStateHandle) : AndroidViewModel(app) {

    private val repo = getApplication<PanolApp>().repository

    val filtro = MutableStateFlow(
        handle.get<String>("estado")?.let { nombre -> EstadoTablet.entries.firstOrNull { it.name == nombre } },
    )

    val consulta = MutableStateFlow("")

    /** `null` mientras carga la primera vez. */
    val tablets: StateFlow<List<Tablet>?> = combine(
        filtro,
        // Sin espera para el valor inicial; 250 ms mientras el usuario tipea.
        consulta.debounce { texto -> if (texto.isEmpty()) 0L else 250L },
    ) { estado, texto -> estado to texto }
        .flatMapLatest { (estado, texto) -> repo.observarInventario(estado, texto) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun elegirFiltro(estado: EstadoTablet?) {
        filtro.value = estado
    }

    fun buscar(texto: String) {
        consulta.value = texto
    }
}
