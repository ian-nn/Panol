package com.panolapp.ui

import androidx.annotation.StringRes
import com.panolapp.R
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.QrError
import com.panolapp.domain.TipoMovimiento
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@StringRes
fun EstadoTablet.etiquetaRes(): Int = when (this) {
    EstadoTablet.EN_PANOL -> R.string.estado_en_panol
    EstadoTablet.USO_LOCAL -> R.string.estado_uso_local
    EstadoTablet.USO_EXTERNO -> R.string.estado_uso_externo
    EstadoTablet.SERVICIO_TECNICO -> R.string.estado_servicio_tecnico
}

@StringRes
fun TipoMovimiento.etiquetaRes(): Int = when (this) {
    TipoMovimiento.ALTA -> R.string.mov_alta
    TipoMovimiento.DESPACHO_LOCAL -> R.string.mov_despacho_local
    TipoMovimiento.DESPACHO_EXTERNO -> R.string.mov_despacho_externo
    TipoMovimiento.DEVOLUCION -> R.string.mov_devolucion
    TipoMovimiento.ENVIO_SERVICIO -> R.string.mov_envio_servicio
    TipoMovimiento.REINGRESO_SERVICIO -> R.string.mov_reingreso_servicio
}

@StringRes
fun ErrorNegocio.mensajeRes(): Int = when (this) {
    ErrorNegocio.SERIE_INVALIDA -> R.string.err_serie_invalida
    ErrorNegocio.SERIE_DUPLICADA -> R.string.err_serie_duplicada
    ErrorNegocio.ESPECIFICACIONES_INCOMPLETAS -> R.string.err_specs_incompletas
    ErrorNegocio.TABLET_NO_ENCONTRADA -> R.string.err_tablet_no_encontrada
    ErrorNegocio.TABLET_NO_DISPONIBLE -> R.string.err_tablet_no_disponible
    ErrorNegocio.TABLET_NO_EN_USO -> R.string.err_tablet_no_en_uso
    ErrorNegocio.LEGAJO_INVALIDO -> R.string.err_legajo_invalido
    ErrorNegocio.NOMBRE_INVALIDO -> R.string.err_nombre_invalido
    ErrorNegocio.HOME_OFFICE_SIN_TAREA -> R.string.err_home_office_sin_tarea
    ErrorNegocio.DANOS_SIN_OBSERVACION -> R.string.err_danos_sin_observacion
    ErrorNegocio.MOTIVO_REQUERIDO -> R.string.err_motivo_requerido
    ErrorNegocio.TRANSICION_INVALIDA -> R.string.err_transicion_invalida
    ErrorNegocio.ERROR_INTERNO -> R.string.err_interno
}

@StringRes
fun QrError.mensajeRes(): Int = when (this) {
    QrError.VACIO -> R.string.qr_err_vacio
    QrError.FORMATO_DESCONOCIDO -> R.string.qr_err_formato
    QrError.SIN_SERIE -> R.string.qr_err_sin_serie
    QrError.SERIE_INVALIDA -> R.string.qr_err_serie_invalida
    QrError.LECTURA_FALLIDA -> R.string.qr_err_lectura
}

/** dd/MM/yyyy HH:mm en horario local del dispositivo. */
fun formatearFecha(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-AR")).format(Date(epochMs))
