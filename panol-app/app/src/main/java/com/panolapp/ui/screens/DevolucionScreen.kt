package com.panolapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.EstadoFisico
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.Reglas
import com.panolapp.ui.components.AlertBanner
import com.panolapp.ui.components.BannerTipo
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.components.Campo
import com.panolapp.ui.components.EstadoVacio
import com.panolapp.ui.components.PanolTopBar
import com.panolapp.ui.components.SectionCard
import com.panolapp.ui.components.SpecRow
import com.panolapp.ui.components.TarjetaOpcion
import com.panolapp.ui.formatearFecha
import com.panolapp.ui.mensajeRes
import com.panolapp.ui.vm.EventoOperacion
import com.panolapp.ui.vm.TabletUi
import com.panolapp.ui.vm.TabletViewModel

/** CU-03: recibir la devolución, verificar el estado físico y reingresar al pañol. */
@Composable
fun DevolucionScreen(
    onVolver: () -> Unit,
    onListo: () -> Unit,
    vm: TabletViewModel = viewModel(),
) {
    val estado by vm.tablet.collectAsStateWithLifecycle()

    var fisico by rememberSaveable { mutableStateOf(EstadoFisico.SIN_DANOS) }
    var observaciones by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf<ErrorNegocio?>(null) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { evento ->
            when (evento) {
                EventoOperacion.Exito -> onListo()
                is EventoOperacion.Fallo -> errorServidor = evento.errores.first()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PanolTopBar(titulo = stringResource(R.string.devolucion_titulo), onVolver = onVolver) },
    ) { padding ->
        Box(Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize()) {
            when (val ui = estado) {
                TabletUi.Cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                TabletUi.NoEncontrada -> EstadoVacio(
                    icono = Icons.Rounded.TabletAndroid,
                    titulo = stringResource(R.string.err_tablet_no_encontrada),
                    texto = stringResource(R.string.ficha_no_encontrada_texto),
                    modifier = Modifier.align(Alignment.Center),
                )
                is TabletUi.Lista -> {
                    val tablet = ui.tablet
                    val errores = Reglas.validarDevolucion(tablet.estado, fisico, observaciones)

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ResumenTablet(tablet)

                        if (!tablet.estado.enUso) {
                            AlertBanner(
                                titulo = stringResource(R.string.err_tablet_no_en_uso),
                                texto = stringResource(R.string.devolucion_no_en_uso_texto),
                                tipo = BannerTipo.ERROR,
                            )
                        } else {
                            SectionCard(titulo = stringResource(R.string.devolucion_quien)) {
                                SpecRow(stringResource(R.string.asig_legajo), tablet.legajoActual.orEmpty())
                                SpecRow(stringResource(R.string.asig_empleado), tablet.nombreActual.orEmpty())
                                SpecRow(
                                    stringResource(R.string.asig_uso),
                                    stringResource(
                                        if (tablet.estado == EstadoTablet.USO_EXTERNO) {
                                            R.string.uso_home_office
                                        } else {
                                            R.string.uso_oficina
                                        },
                                    ),
                                )
                                SpecRow(stringResource(R.string.asig_tarea), tablet.tareaActual.orEmpty())
                                SpecRow(
                                    stringResource(R.string.asig_desde),
                                    formatearFecha(tablet.estadoDesde),
                                )
                            }

                            SectionCard(titulo = stringResource(R.string.devolucion_estado_fisico)) {
                                TarjetaOpcion(
                                    titulo = stringResource(R.string.fisico_ok),
                                    descripcion = stringResource(R.string.fisico_ok_desc),
                                    icono = Icons.Rounded.CheckCircle,
                                    seleccionada = fisico == EstadoFisico.SIN_DANOS,
                                    onClick = { fisico = EstadoFisico.SIN_DANOS },
                                )
                                TarjetaOpcion(
                                    titulo = stringResource(R.string.fisico_danos),
                                    descripcion = stringResource(R.string.fisico_danos_desc),
                                    icono = Icons.Rounded.Build,
                                    seleccionada = fisico == EstadoFisico.CON_DANOS,
                                    onClick = { fisico = EstadoFisico.CON_DANOS },
                                )
                                Campo(
                                    valor = observaciones,
                                    onCambio = { observaciones = it },
                                    etiqueta = stringResource(
                                        if (fisico == EstadoFisico.CON_DANOS) {
                                            R.string.campo_obs_obligatoria
                                        } else {
                                            R.string.campo_obs_opcional
                                        },
                                    ),
                                    error = if (intento && ErrorNegocio.DANOS_SIN_OBSERVACION in errores) {
                                        stringResource(R.string.err_danos_sin_observacion)
                                    } else {
                                        null
                                    },
                                    lineas = 3,
                                    imeAction = ImeAction.Done,
                                    maxCaracteres = 200,
                                )
                            }

                            AlertBanner(
                                texto = stringResource(
                                    if (fisico == EstadoFisico.SIN_DANOS) {
                                        R.string.devolucion_resultado_ok
                                    } else {
                                        R.string.devolucion_resultado_servicio
                                    },
                                ),
                                tipo = if (fisico == EstadoFisico.SIN_DANOS) BannerTipo.INFO else BannerTipo.ADVERTENCIA,
                            )

                            errorServidor?.let {
                                AlertBanner(texto = stringResource(it.mensajeRes()), tipo = BannerTipo.ERROR)
                            }

                            BotonPrincipal(
                                texto = stringResource(
                                    if (fisico == EstadoFisico.SIN_DANOS) {
                                        R.string.devolucion_confirmar
                                    } else {
                                        R.string.devolucion_confirmar_servicio
                                    },
                                ),
                                icono = Icons.AutoMirrored.Rounded.ArrowBack,
                                cargando = vm.ocupado,
                                onClick = {
                                    intento = true
                                    errorServidor = null
                                    if (errores.isEmpty()) vm.devolver(fisico, observaciones)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
