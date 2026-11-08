package com.panolapp.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.panolapp.PanolApp
import com.panolapp.domain.EstadoTablet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InicioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = getApplication<PanolApp>().repository

    /** Cantidad de tablets por estado; se actualiza sola cuando cambia la base. */
    val conteos: StateFlow<Map<EstadoTablet, Int>> = repo.observarConteos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EstadoTablet.entries.associateWith { 0 },
        )
}
