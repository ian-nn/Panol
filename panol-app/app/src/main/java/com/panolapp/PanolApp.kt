package com.panolapp

import android.app.Application
import com.panolapp.data.PanolDatabase
import com.panolapp.data.PanolRepository
import com.panolapp.domain.TabletSpecs
import com.panolapp.ui.UiMensaje
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Contenedor de dependencias mínimo (sin framework de DI: alcanza para este TP y es fácil de explicar).
 */
class PanolApp : Application() {

    val database: PanolDatabase by lazy { PanolDatabase.crear(this) }

    val repository: PanolRepository by lazy { PanolRepository(database) }

    /** Mensajes globales (snackbar) que sobreviven a la navegación entre pantallas. */
    val mensajes = MutableSharedFlow<UiMensaje>(extraBufferCapacity = 8)

    /**
     * Datos leídos de un QR de una tablet aún no registrada, para precargar el formulario de alta.
     * Lo escribe el escáner y lo consume (y limpia) la pantalla de alta.
     */
    @Volatile
    var altaPrefill: TabletSpecs? = null
}
