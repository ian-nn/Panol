package com.panolapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.domain.ErrorNegocio
import com.panolapp.domain.Reglas
import com.panolapp.domain.TabletSpecs
import com.panolapp.ui.components.AlertBanner
import com.panolapp.ui.components.BannerTipo
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.components.Campo
import com.panolapp.ui.components.PanolTopBar
import com.panolapp.ui.components.SectionCard
import com.panolapp.ui.vm.AltaViewModel

private val PUERTOS_BASE = listOf("USB-C", "Micro-USB", "Jack 3.5 mm", "HDMI", "microSD", "Pogo pin")
private val CONECTIVIDAD_BASE = listOf("Wi-Fi", "Bluetooth", "4G", "5G", "NFC", "GPS")

/** CU-01: alta de una tablet en el pañol y generación del QR. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AltaScreen(
    onVolver: () -> Unit,
    onAltaCreada: (String) -> Unit,
    vm: AltaViewModel = viewModel(),
) {
    val inicial = vm.prefill

    var serie by rememberSaveable { mutableStateOf(vm.serieInicial) }
    var procesador by rememberSaveable { mutableStateOf(inicial?.procesador.orEmpty()) }
    var ram by rememberSaveable { mutableStateOf(inicial?.ram.orEmpty()) }
    var almacenamiento by rememberSaveable { mutableStateOf(inicial?.almacenamiento.orEmpty()) }
    var pantalla by rememberSaveable { mutableStateOf(inicial?.pantalla.orEmpty()) }
    var puertos by rememberSaveable { mutableStateOf(inicial?.puertos.orEmpty()) }
    var conectividad by rememberSaveable { mutableStateOf(inicial?.conectividad.orEmpty()) }
    var puertosExtra by rememberSaveable { mutableStateOf("") }
    var conectividadExtra by rememberSaveable { mutableStateOf("") }
    var intento by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.altas.collect { serieCreada -> onAltaCreada(serieCreada) }
    }

    val serieNormalizada = Reglas.normalizarSerie(serie)
    val serieOk = Reglas.serieValida(serieNormalizada)
    val opcionesPuertos = (PUERTOS_BASE + inicial?.puertos.orEmpty()).distinct()
    val opcionesConectividad = (CONECTIVIDAD_BASE + inicial?.conectividad.orEmpty()).distinct()

    fun armarSpecs() = TabletSpecs(
        serie = serieNormalizada,
        procesador = procesador,
        ram = ram,
        almacenamiento = almacenamiento,
        pantalla = pantalla,
        puertos = (puertos + separar(puertosExtra)).distinct(),
        conectividad = (conectividad + separar(conectividadExtra)).distinct(),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PanolTopBar(titulo = stringResource(R.string.alta_titulo), onVolver = onVolver) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (inicial != null) {
                AlertBanner(
                    titulo = stringResource(R.string.alta_no_registrada),
                    texto = stringResource(
                        if (inicial.completa) R.string.alta_desde_qr_completo else R.string.alta_desde_qr_parcial,
                    ),
                    tipo = BannerTipo.INFO,
                )
            }

            SectionCard(titulo = stringResource(R.string.alta_identificacion)) {
                Campo(
                    valor = serie,
                    onCambio = {
                        serie = it
                        vm.limpiarErrores()
                    },
                    etiqueta = stringResource(R.string.campo_serie),
                    error = when {
                        ErrorNegocio.SERIE_DUPLICADA in vm.errores -> stringResource(R.string.err_serie_duplicada)
                        (intento || serie.isNotEmpty()) && !serieOk -> stringResource(R.string.err_serie_invalida)
                        else -> null
                    },
                    ayuda = stringResource(R.string.alta_serie_ayuda),
                    capitalizacion = KeyboardCapitalization.Characters,
                    maxCaracteres = 40,
                )
            }

            SectionCard(titulo = stringResource(R.string.ficha_specs)) {
                Campo(
                    valor = procesador,
                    onCambio = { procesador = it },
                    etiqueta = stringResource(R.string.spec_procesador),
                    error = errorSiVacio(intento, procesador),
                    maxCaracteres = 60,
                )
                Campo(
                    valor = ram,
                    onCambio = { ram = it },
                    etiqueta = stringResource(R.string.spec_ram),
                    error = errorSiVacio(intento, ram),
                    ayuda = stringResource(R.string.alta_ejemplo_ram),
                    maxCaracteres = 30,
                )
                Campo(
                    valor = almacenamiento,
                    onCambio = { almacenamiento = it },
                    etiqueta = stringResource(R.string.spec_almacenamiento),
                    error = errorSiVacio(intento, almacenamiento),
                    ayuda = stringResource(R.string.alta_ejemplo_almacenamiento),
                    maxCaracteres = 30,
                )
                Campo(
                    valor = pantalla,
                    onCambio = { pantalla = it },
                    etiqueta = stringResource(R.string.spec_pantalla),
                    error = errorSiVacio(intento, pantalla),
                    ayuda = stringResource(R.string.alta_ejemplo_pantalla),
                    maxCaracteres = 30,
                )
            }

            SectionCard(titulo = stringResource(R.string.ficha_puertos)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    opcionesPuertos.forEach { opcion ->
                        OpcionChip(opcion, opcion in puertos) {
                            puertos = if (opcion in puertos) puertos - opcion else puertos + opcion
                        }
                    }
                }
                Campo(
                    valor = puertosExtra,
                    onCambio = { puertosExtra = it },
                    etiqueta = stringResource(R.string.alta_otros_puertos),
                    maxCaracteres = 80,
                )
            }

            SectionCard(titulo = stringResource(R.string.ficha_conectividad)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    opcionesConectividad.forEach { opcion ->
                        OpcionChip(opcion, opcion in conectividad) {
                            conectividad =
                                if (opcion in conectividad) conectividad - opcion else conectividad + opcion
                        }
                    }
                }
                Campo(
                    valor = conectividadExtra,
                    onCambio = { conectividadExtra = it },
                    etiqueta = stringResource(R.string.alta_otras_conexiones),
                    imeAction = ImeAction.Done,
                    maxCaracteres = 80,
                )
            }

            if (ErrorNegocio.ERROR_INTERNO in vm.errores) {
                AlertBanner(texto = stringResource(R.string.err_interno), tipo = BannerTipo.ERROR)
            }

            BotonPrincipal(
                texto = stringResource(R.string.alta_guardar),
                icono = Icons.Rounded.QrCodeScanner,
                cargando = vm.ocupado,
                onClick = {
                    intento = true
                    val specs = armarSpecs()
                    if (Reglas.validarAlta(specs).isEmpty()) vm.guardar(specs)
                },
            )
        }
    }
}

@Composable
private fun errorSiVacio(intento: Boolean, valor: String): String? =
    if (intento && valor.isBlank()) stringResource(R.string.err_campo_obligatorio) else null

@Composable
private fun OpcionChip(texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionada,
        onClick = onClick,
        label = { Text(texto) },
        leadingIcon = if (seleccionada) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.padding(0.dp)) }
        } else {
            null
        },
    )
}

private fun separar(texto: String): List<String> =
    texto.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
