package com.panolapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.Movimiento
import com.panolapp.domain.Tablet
import com.panolapp.ui.components.AlertBanner
import com.panolapp.ui.components.BannerTipo
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.components.BotonSecundario
import com.panolapp.ui.components.Campo
import com.panolapp.ui.components.EstadoChip
import com.panolapp.ui.components.EstadoVacio
import com.panolapp.ui.components.PanolTopBar
import com.panolapp.ui.components.SectionCard
import com.panolapp.ui.components.SpecRow
import com.panolapp.ui.components.TagRow
import com.panolapp.ui.etiquetaRes
import com.panolapp.ui.formatearFecha
import com.panolapp.ui.mensajeRes
import com.panolapp.ui.vm.EventoOperacion
import com.panolapp.ui.vm.TabletUi
import com.panolapp.ui.vm.TabletViewModel

private enum class DialogoFicha { NINGUNO, ENVIAR_SERVICIO, REINGRESAR }

@Composable
fun FichaScreen(
    onVolver: () -> Unit,
    onDespachar: () -> Unit,
    onDevolver: () -> Unit,
    onVerQr: () -> Unit,
    vm: TabletViewModel = viewModel(),
) {
    val estado by vm.tablet.collectAsStateWithLifecycle()
    val historial by vm.historial.collectAsStateWithLifecycle()
    var dialogo by rememberSaveable { mutableStateOf(DialogoFicha.NINGUNO) }
    var errorDialogo by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { evento ->
            when (evento) {
                EventoOperacion.Exito -> {
                    dialogo = DialogoFicha.NINGUNO
                    errorDialogo = null
                }
                is EventoOperacion.Fallo -> errorDialogo = evento.errores.first().mensajeRes()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PanolTopBar(titulo = stringResource(R.string.ficha_titulo), onVolver = onVolver) },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (val ui = estado) {
                TabletUi.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                TabletUi.NoEncontrada -> EstadoVacio(
                    icono = Icons.Rounded.TabletAndroid,
                    titulo = stringResource(R.string.err_tablet_no_encontrada),
                    texto = stringResource(R.string.ficha_no_encontrada_texto),
                    modifier = Modifier.align(Alignment.Center),
                )
                is TabletUi.Lista -> FichaContenido(
                    tablet = ui.tablet,
                    historial = historial,
                    onDespachar = onDespachar,
                    onDevolver = onDevolver,
                    onVerQr = onVerQr,
                    onEnviarServicio = {
                        errorDialogo = null
                        dialogo = DialogoFicha.ENVIAR_SERVICIO
                    },
                    onReingresar = {
                        errorDialogo = null
                        dialogo = DialogoFicha.REINGRESAR
                    },
                )
            }
        }
    }

    when (dialogo) {
        DialogoFicha.ENVIAR_SERVICIO -> DialogoMotivo(
            titulo = stringResource(R.string.dlg_servicio_titulo),
            etiqueta = stringResource(R.string.dlg_servicio_motivo),
            confirmar = stringResource(R.string.dlg_servicio_confirmar),
            obligatorio = true,
            ocupado = vm.ocupado,
            error = errorDialogo?.let { stringResource(it) },
            onConfirmar = vm::enviarAServicio,
            onCerrar = { dialogo = DialogoFicha.NINGUNO },
        )
        DialogoFicha.REINGRESAR -> DialogoMotivo(
            titulo = stringResource(R.string.dlg_reingreso_titulo),
            etiqueta = stringResource(R.string.dlg_reingreso_obs),
            confirmar = stringResource(R.string.dlg_reingreso_confirmar),
            obligatorio = false,
            ocupado = vm.ocupado,
            error = errorDialogo?.let { stringResource(it) },
            onConfirmar = vm::reingresarDeServicio,
            onCerrar = { dialogo = DialogoFicha.NINGUNO },
        )
        DialogoFicha.NINGUNO -> Unit
    }
}

@Composable
private fun FichaContenido(
    tablet: Tablet,
    historial: List<Movimiento>,
    onDespachar: () -> Unit,
    onDevolver: () -> Unit,
    onVerQr: () -> Unit,
    onEnviarServicio: () -> Unit,
    onReingresar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabecera: serie + estado
        SectionCard(titulo = null) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tablet.serie,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EstadoChip(tablet.estado)
                    Text(
                        text = stringResource(R.string.ficha_desde, formatearFecha(tablet.estadoDesde)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (tablet.estado.enUso) {
            SectionCard(titulo = stringResource(R.string.ficha_asignacion)) {
                SpecRow(stringResource(R.string.asig_legajo), tablet.legajoActual.orEmpty())
                SpecRow(stringResource(R.string.asig_empleado), tablet.nombreActual.orEmpty())
                SpecRow(
                    stringResource(R.string.asig_uso),
                    stringResource(
                        if (tablet.estado == EstadoTablet.USO_EXTERNO) R.string.uso_home_office else R.string.uso_oficina,
                    ),
                )
                SpecRow(stringResource(R.string.asig_tarea), tablet.tareaActual.orEmpty())
                if (tablet.estado == EstadoTablet.USO_EXTERNO) {
                    AlertBanner(
                        texto = stringResource(R.string.ficha_aviso_externa),
                        tipo = BannerTipo.INFO,
                    )
                }
            }
        }

        SectionCard(titulo = stringResource(R.string.ficha_specs)) {
            SpecRow(stringResource(R.string.spec_procesador), tablet.specs.procesador)
            SpecRow(stringResource(R.string.spec_ram), tablet.specs.ram)
            SpecRow(stringResource(R.string.spec_almacenamiento), tablet.specs.almacenamiento)
            SpecRow(stringResource(R.string.spec_pantalla), tablet.specs.pantalla)
        }

        SectionCard(titulo = stringResource(R.string.ficha_conectividad_puertos)) {
            Text(
                stringResource(R.string.ficha_puertos),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TagRow(tablet.specs.puertos)
            Text(
                stringResource(R.string.ficha_conectividad),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TagRow(tablet.specs.conectividad)
        }

        // Acciones según el estado vigente (solo se ofrecen transiciones válidas)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (tablet.estado) {
                EstadoTablet.EN_PANOL -> {
                    BotonPrincipal(
                        texto = stringResource(R.string.accion_despachar_tablet),
                        icono = Icons.AutoMirrored.Rounded.ArrowForward,
                        onClick = onDespachar,
                    )
                    BotonSecundario(
                        texto = stringResource(R.string.accion_enviar_servicio),
                        icono = Icons.Rounded.Build,
                        onClick = onEnviarServicio,
                    )
                }
                EstadoTablet.USO_LOCAL, EstadoTablet.USO_EXTERNO -> BotonPrincipal(
                    texto = stringResource(R.string.accion_registrar_devolucion),
                    icono = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onDevolver,
                )
                EstadoTablet.SERVICIO_TECNICO -> BotonPrincipal(
                    texto = stringResource(R.string.accion_reingresar),
                    icono = Icons.Rounded.CheckCircle,
                    onClick = onReingresar,
                )
            }
            BotonSecundario(
                texto = stringResource(R.string.accion_ver_qr),
                icono = Icons.Rounded.QrCodeScanner,
                onClick = onVerQr,
            )
        }

        SectionCard(titulo = stringResource(R.string.ficha_historial)) {
            if (historial.isEmpty()) {
                Text(
                    stringResource(R.string.ficha_historial_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                historial.forEachIndexed { indice, movimiento ->
                    if (indice > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    FilaMovimiento(movimiento)
                }
            }
        }
    }
}

@Composable
private fun FilaMovimiento(m: Movimiento) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(m.tipo.etiquetaRes()), style = MaterialTheme.typography.titleSmall)
            Text(
                formatearFecha(m.fecha),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val legajo = m.legajo
        if (legajo != null) {
            Text(
                stringResource(R.string.mov_empleado, legajo, m.nombre ?: "—"),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val tarea = m.tarea
        if (!tarea.isNullOrBlank()) {
            Text(
                stringResource(R.string.mov_tarea, tarea),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val obs = m.observaciones
        if (!obs.isNullOrBlank()) {
            Text(
                stringResource(R.string.mov_obs, obs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DialogoMotivo(
    titulo: String,
    etiqueta: String,
    confirmar: String,
    obligatorio: Boolean,
    ocupado: Boolean,
    error: String?,
    onConfirmar: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    var texto by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!ocupado) onCerrar() },
        title = { Text(titulo) },
        text = {
            Campo(
                valor = texto,
                onCambio = { texto = it },
                etiqueta = etiqueta,
                error = error,
                lineas = 3,
                imeAction = ImeAction.Done,
                maxCaracteres = 200,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(texto) },
                enabled = !ocupado && (!obligatorio || texto.isNotBlank()),
            ) { Text(confirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !ocupado) { Text(stringResource(R.string.cancelar)) }
        },
    )
}
