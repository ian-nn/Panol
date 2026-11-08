package com.panolapp.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.ui.camera.CameraQrScanner
import com.panolapp.ui.components.AlertBanner
import com.panolapp.ui.components.BannerTipo
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.components.BotonSecundario
import com.panolapp.ui.components.Campo
import com.panolapp.ui.mensajeRes
import com.panolapp.ui.navigation.ModoEscaneo
import com.panolapp.ui.vm.EscanerViewModel
import com.panolapp.ui.vm.ResultadoEscaneo

@Composable
fun EscanerScreen(
    modo: ModoEscaneo,
    onVolver: () -> Unit,
    onResultado: (ResultadoEscaneo) -> Unit,
    vm: EscanerViewModel = viewModel(),
) {
    val contexto = LocalContext.current
    val haptico = LocalHapticFeedback.current

    val tieneCamara = remember {
        contexto.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    var permiso by remember { mutableStateOf(permisoConcedido(contexto)) }
    var pidioPermiso by rememberSaveable { mutableStateOf(false) }
    var linterna by rememberSaveable { mutableStateOf(false) }
    var tieneFlash by remember { mutableStateOf(false) }
    var mostrarManual by rememberSaveable { mutableStateOf(false) }

    val lanzador = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        permiso = concedido
        pidioPermiso = true
    }

    // Si el usuario vuelve de Ajustes con el permiso otorgado, se activa la cámara.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permiso = permisoConcedido(contexto) }

    LaunchedEffect(Unit) {
        if (tieneCamara && !permiso) lanzador.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(Unit) {
        vm.resultados.collect { resultado -> onResultado(resultado) }
    }

    val camaraActiva = tieneCamara && permiso
    val lecturaActiva = vm.problema == null && !vm.procesando

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (camaraActiva) {
            CameraQrScanner(
                habilitado = lecturaActiva,
                linterna = linterna,
                onQr = { valor ->
                    haptico.performHapticFeedback(HapticFeedbackType.LongPress)
                    vm.alLeerQr(valor)
                },
                onCamaraLista = { tieneFlash = it },
                onFalloCamara = vm::fallaCamara,
                modifier = Modifier.fillMaxSize(),
            )
            MarcoDeLectura(error = vm.problema != null, modifier = Modifier.align(Alignment.Center))
        } else {
            PanelSinCamara(
                tieneCamara = tieneCamara,
                denegado = pidioPermiso && !permiso,
                onPedir = { lanzador.launch(Manifest.permission.CAMERA) },
                onAjustes = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", contexto.packageName, null)
                        },
                    )
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Barra superior flotante
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BotonFlotante(onClick = onVolver) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.volver),
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            if (camaraActiva && tieneFlash) {
                BotonFlotante(onClick = { linterna = !linterna }) {
                    Icon(
                        if (linterna) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                        contentDescription = stringResource(R.string.escaner_linterna),
                        tint = Color.White,
                    )
                }
            }
        }

        // Panel inferior con instrucciones / error / flujo alternativo
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val problema = vm.problema
                if (problema != null) {
                    AlertBanner(
                        titulo = stringResource(R.string.escaner_no_leido),
                        texto = stringResource(problema.mensajeRes()),
                        tipo = BannerTipo.ERROR,
                    )
                    BotonPrincipal(
                        texto = stringResource(R.string.escaner_reintentar),
                        icono = Icons.Rounded.QrCodeScanner,
                        onClick = vm::reintentar,
                    )
                } else {
                    Text(
                        text = stringResource(textoModo(modo)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.escaner_ayuda),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BotonSecundario(
                    texto = stringResource(R.string.escaner_ingreso_manual),
                    icono = Icons.Rounded.Keyboard,
                    onClick = { mostrarManual = true },
                )
            }
        }
    }

    if (mostrarManual) {
        DialogoSerieManual(
            error = vm.errorManual,
            onCambio = vm::limpiarErrorManual,
            onConfirmar = { serie -> vm.buscarManual(serie) },
            onCerrar = {
                mostrarManual = false
                vm.limpiarErrorManual()
            },
        )
    }
}

private fun permisoConcedido(contexto: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun textoModo(modo: ModoEscaneo): Int = when (modo) {
    ModoEscaneo.CONSULTA -> R.string.escaner_titulo_consulta
    ModoEscaneo.DESPACHO -> R.string.escaner_titulo_despacho
    ModoEscaneo.DEVOLUCION -> R.string.escaner_titulo_devolucion
}

@Composable
private fun BotonFlotante(onClick: () -> Unit, contenido: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.45f)),
    ) { contenido() }
}

@Composable
private fun MarcoDeLectura(error: Boolean, modifier: Modifier = Modifier) {
    val color = if (error) MaterialTheme.colorScheme.error else Color.White
    Box(
        modifier = modifier
            .fillMaxWidth(0.68f)
            .aspectRatio(1f)
            .border(3.dp, color.copy(alpha = 0.95f), RoundedCornerShape(28.dp)),
    )
}

@Composable
private fun PanelSinCamara(
    tieneCamara: Boolean,
    denegado: Boolean,
    onPedir: () -> Unit,
    onAjustes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(72.dp).background(Color.White.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
        }
        Text(
            text = stringResource(if (tieneCamara) R.string.permiso_titulo else R.string.sin_camara_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(if (tieneCamara) R.string.permiso_texto else R.string.sin_camara_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        if (tieneCamara) {
            if (denegado) {
                BotonPrincipal(stringResource(R.string.permiso_ajustes), onClick = onAjustes)
            } else {
                BotonPrincipal(stringResource(R.string.permiso_conceder), onClick = onPedir)
            }
        }
    }
}

@Composable
private fun DialogoSerieManual(
    error: Boolean,
    onCambio: () -> Unit,
    onConfirmar: (String) -> Unit,
    onCerrar: () -> Unit,
) {
    var serie by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(stringResource(R.string.manual_titulo)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.manual_texto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Campo(
                    valor = serie,
                    onCambio = {
                        serie = it
                        onCambio()
                    },
                    etiqueta = stringResource(R.string.campo_serie),
                    error = if (error) stringResource(R.string.err_serie_invalida) else null,
                    capitalizacion = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done,
                    maxCaracteres = 40,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(serie) }, enabled = serie.isNotBlank()) {
                Text(stringResource(R.string.manual_buscar))
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(stringResource(R.string.cancelar)) }
        },
    )
}
