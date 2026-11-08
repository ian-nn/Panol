package com.panolapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.Reglas
import com.panolapp.domain.Tablet
import com.panolapp.domain.TipoUso
import com.panolapp.ui.components.AlertBanner
import com.panolapp.ui.components.BannerTipo
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.components.Campo
import com.panolapp.ui.components.EstadoChip
import com.panolapp.ui.components.EstadoVacio
import com.panolapp.ui.components.PanolTopBar
import com.panolapp.ui.components.SectionCard
import com.panolapp.ui.components.TarjetaOpcion
import com.panolapp.ui.mensajeRes
import com.panolapp.ui.vm.EventoOperacion
import com.panolapp.ui.vm.TabletUi
import com.panolapp.ui.vm.TabletViewModel
import kotlinx.coroutines.delay

/** CU-02: despachar una tablet a un empleado (oficina u Home Office con tarea asignada). */
@Composable
fun DespachoScreen(
    onVolver: () -> Unit,
    onListo: () -> Unit,
    vm: TabletViewModel = viewModel(),
) {
    val estado by vm.tablet.collectAsStateWithLifecycle()

    var uso by rememberSaveable { mutableStateOf(TipoUso.OFICINA) }
    var legajo by rememberSaveable { mutableStateOf("") }
    var nombre by rememberSaveable { mutableStateOf("") }
    var nombreAutocompletado by rememberSaveable { mutableStateOf(false) }
    var tarea by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }
    var alertaHomeOffice by rememberSaveable { mutableStateOf(false) }
    var errorServidor by remember { mutableStateOf<ErrorNegocio?>(null) }

    LaunchedEffect(Unit) {
        vm.eventos.collect { evento ->
            when (evento) {
                EventoOperacion.Exito -> onListo()
                is EventoOperacion.Fallo -> {
                    val primero = evento.errores.first()
                    if (primero == ErrorNegocio.HOME_OFFICE_SIN_TAREA) {
                        alertaHomeOffice = true
                    } else {
                        errorServidor = primero
                    }
                }
            }
        }
    }

    // Si el legajo ya existe en la base, se sugiere el nombre (editable).
    LaunchedEffect(legajo) {
        delay(250)
        val conocido = vm.nombreDeEmpleado(legajo)
        if (conocido != null && (nombre.isBlank() || nombreAutocompletado)) {
            nombre = conocido
            nombreAutocompletado = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PanolTopBar(titulo = stringResource(R.string.despacho_titulo), onVolver = onVolver) },
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
                    val errores = Reglas.validarDespacho(tablet.estado, uso, legajo, nombre, tarea)
                    val sinTarea = ErrorNegocio.HOME_OFFICE_SIN_TAREA in errores

                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ResumenTablet(tablet)

                        if (tablet.estado != EstadoTablet.EN_PANOL) {
                            AlertBanner(
                                titulo = stringResource(R.string.err_tablet_no_disponible),
                                texto = stringResource(R.string.despacho_no_disponible_texto),
                                tipo = BannerTipo.ERROR,
                            )
                        } else {
                            SectionCard(titulo = stringResource(R.string.despacho_tipo_uso)) {
                                TarjetaOpcion(
                                    titulo = stringResource(R.string.uso_oficina),
                                    descripcion = stringResource(R.string.uso_oficina_desc),
                                    icono = Icons.Rounded.Business,
                                    seleccionada = uso == TipoUso.OFICINA,
                                    onClick = { uso = TipoUso.OFICINA },
                                )
                                TarjetaOpcion(
                                    titulo = stringResource(R.string.uso_home_office),
                                    descripcion = stringResource(R.string.uso_home_office_desc),
                                    icono = Icons.Rounded.HomeWork,
                                    seleccionada = uso == TipoUso.HOME_OFFICE,
                                    onClick = { uso = TipoUso.HOME_OFFICE },
                                )
                            }

                            SectionCard(titulo = stringResource(R.string.despacho_empleado)) {
                                Campo(
                                    valor = legajo,
                                    onCambio = { legajo = it },
                                    etiqueta = stringResource(R.string.campo_legajo),
                                    error = if (intento && ErrorNegocio.LEGAJO_INVALIDO in errores) {
                                        stringResource(R.string.err_legajo_invalido)
                                    } else {
                                        null
                                    },
                                    capitalizacion = KeyboardCapitalization.Characters,
                                    maxCaracteres = 10,
                                )
                                Campo(
                                    valor = nombre,
                                    onCambio = {
                                        nombre = it
                                        nombreAutocompletado = false
                                    },
                                    etiqueta = stringResource(R.string.campo_nombre),
                                    error = if (intento && ErrorNegocio.NOMBRE_INVALIDO in errores) {
                                        stringResource(R.string.err_nombre_invalido)
                                    } else {
                                        null
                                    },
                                    capitalizacion = KeyboardCapitalization.Words,
                                    maxCaracteres = 60,
                                )
                            }

                            SectionCard(titulo = stringResource(R.string.despacho_tarea_seccion)) {
                                if (uso == TipoUso.HOME_OFFICE && sinTarea) {
                                    // Alerta visual: ámbar mientras escribe, roja tras intentar confirmar.
                                    AlertBanner(
                                        titulo = stringResource(R.string.alerta_home_titulo),
                                        texto = stringResource(R.string.alerta_home_texto, Reglas.MIN_TAREA),
                                        tipo = if (intento) BannerTipo.ERROR else BannerTipo.ADVERTENCIA,
                                    )
                                }
                                Campo(
                                    valor = tarea,
                                    onCambio = { tarea = it },
                                    etiqueta = stringResource(
                                        if (uso == TipoUso.HOME_OFFICE) {
                                            R.string.campo_tarea_obligatoria
                                        } else {
                                            R.string.campo_tarea_opcional
                                        },
                                    ),
                                    error = if (intento && uso == TipoUso.HOME_OFFICE && sinTarea) {
                                        stringResource(R.string.err_home_office_sin_tarea)
                                    } else {
                                        null
                                    },
                                    lineas = 3,
                                    imeAction = ImeAction.Done,
                                    maxCaracteres = 200,
                                )
                            }

                            errorServidor?.let {
                                AlertBanner(texto = stringResource(it.mensajeRes()), tipo = BannerTipo.ERROR)
                            }

                            BotonPrincipal(
                                texto = stringResource(R.string.despacho_confirmar),
                                icono = Icons.AutoMirrored.Rounded.ArrowForward,
                                cargando = vm.ocupado,
                                onClick = {
                                    intento = true
                                    errorServidor = null
                                    when {
                                        errores.isEmpty() -> vm.despachar(uso, legajo, nombre, tarea)
                                        sinTarea && uso == TipoUso.HOME_OFFICE -> alertaHomeOffice = true
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (alertaHomeOffice) {
        AlertDialog(
            onDismissRequest = { alertaHomeOffice = false },
            icon = {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.alerta_home_titulo)) },
            text = { Text(stringResource(R.string.alerta_home_dialogo, Reglas.MIN_TAREA)) },
            confirmButton = {
                TextButton(onClick = { alertaHomeOffice = false }) {
                    Text(stringResource(R.string.alerta_home_asignar))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        uso = TipoUso.OFICINA
                        alertaHomeOffice = false
                    },
                ) { Text(stringResource(R.string.alerta_home_cambiar)) }
            },
        )
    }
}

/** Cabecera compacta con la tablet que se está operando. */
@Composable
internal fun ResumenTablet(tablet: Tablet) {
    SectionCard(titulo = null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tablet.serie, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = listOf(tablet.specs.procesador, tablet.specs.ram, tablet.specs.almacenamiento)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            EstadoChip(tablet.estado)
        }
    }
}
