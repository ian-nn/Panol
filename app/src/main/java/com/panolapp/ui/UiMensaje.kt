package com.panolapp.ui

import androidx.annotation.StringRes

/** Mensaje breve (snackbar) que puede emitirse desde cualquier ViewModel. */
data class UiMensaje(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
)
