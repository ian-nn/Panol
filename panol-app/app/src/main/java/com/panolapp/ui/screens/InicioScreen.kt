package com.panolapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.domain.EstadoTablet
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.etiquetaRes
import com.panolapp.ui.navigation.ModoEscaneo
import com.panolapp.ui.theme.paleta
import com.panolapp.ui.vm.InicioViewModel

@Composable
fun InicioScreen(
    onEscanear: (ModoEscaneo) -> Unit,
    onAlta: () -> Unit,
    onInventario: (EstadoTablet?) -> Unit,
    vm: InicioViewModel = viewModel(),
) {
    val conteos by vm.conteos.collectAsStateWithLifecycle()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Encabezado(total = conteos.values.sum())

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.inicio_estado_flota),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Contador(EstadoTablet.EN_PANOL, conteos, onInventario, Modifier.weight(1f))
                    Contador(EstadoTablet.USO_LOCAL, conteos, onInventario, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Contador(EstadoTablet.USO_EXTERNO, conteos, onInventario, Modifier.weight(1f))
                    Contador(EstadoTablet.SERVICIO_TECNICO, conteos, onInventario, Modifier.weight(1f))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.inicio_acciones),
                    style = MaterialTheme.typography.titleMedium,
                )
                BotonPrincipal(
                    texto = stringResource(R.string.accion_escanear),
                    icono = Icons.Rounded.QrCodeScanner,
                    onClick = { onEscanear(ModoEscaneo.CONSULTA) },
                    modifier = Modifier.height(64.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = { onEscanear(ModoEscaneo.DESPACHO) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.accion_despachar))
                    }
                    FilledTonalButton(
                        onClick = { onEscanear(ModoEscaneo.DEVOLUCION) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.accion_devolver))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onAlta,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.accion_alta))
                    }
                    OutlinedButton(
                        onClick = { onInventario(null) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.accion_inventario))
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(total: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.TabletAndroid,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
        Column {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.inicio_subtitulo, total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Contador(
    estado: EstadoTablet,
    conteos: Map<EstadoTablet, Int>,
    onClick: (EstadoTablet?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paleta = estado.paleta()
    Surface(
        color = paleta.contenedor,
        contentColor = paleta.contenido,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, paleta.contenido.copy(alpha = 0.18f)),
        modifier = modifier.clickable { onClick(estado) },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = (conteos[estado] ?: 0).toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(estado.etiquetaRes()),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
