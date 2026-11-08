package com.panolapp.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.domain.Tablet
import com.panolapp.ui.components.AlertBanner
import com.panolapp.ui.components.BannerTipo
import com.panolapp.ui.components.BotonPrincipal
import com.panolapp.ui.components.BotonSecundario
import com.panolapp.ui.components.EstadoVacio
import com.panolapp.ui.components.PanolTopBar
import com.panolapp.ui.components.SectionCard
import com.panolapp.ui.qr.QrSticker
import com.panolapp.ui.vm.TabletUi
import com.panolapp.ui.vm.TabletViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Muestra la etiqueta QR de una tablet (recién creada o consultada) y permite compartirla. */
@Composable
fun VerQrScreen(
    nueva: Boolean,
    onVolver: () -> Unit,
    onIrAFicha: () -> Unit,
    vm: TabletViewModel = viewModel(),
) {
    val estado by vm.tablet.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PanolTopBar(titulo = stringResource(R.string.qr_titulo), onVolver = onVolver) },
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
                is TabletUi.Lista -> ContenidoQr(ui.tablet, nueva, onIrAFicha)
            }
        }
    }
}

@Composable
private fun ContenidoQr(tablet: Tablet, nueva: Boolean, onIrAFicha: () -> Unit) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var fallo by remember { mutableStateOf(false) }

    // Se genera fuera del hilo principal; si falla (p. ej. texto demasiado largo) se informa.
    val bitmap by produceState<Bitmap?>(initialValue = null, tablet.specs) {
        val generado = withContext(Dispatchers.Default) {
            runCatching { QrSticker.render(tablet.specs) }.getOrNull()
        }
        if (generado == null) fallo = true
        value = generado
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (nueva) {
            AlertBanner(
                titulo = stringResource(R.string.qr_registrada_titulo),
                texto = stringResource(R.string.qr_registrada_texto),
                tipo = BannerTipo.INFO,
            )
        }

        // El QR siempre sobre fondo blanco (también en modo oscuro) para que se lea bien.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, MaterialTheme.shapes.large)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val imagen = bitmap
            when {
                imagen != null -> Image(
                    bitmap = imagen.asImageBitmap(),
                    contentDescription = stringResource(R.string.qr_descripcion, tablet.serie),
                    modifier = Modifier.fillMaxWidth().aspectRatio(imagen.width.toFloat() / imagen.height),
                )
                fallo -> Text(stringResource(R.string.qr_error_generar), color = Color.Black)
                else -> CircularProgressIndicator(Modifier.padding(48.dp))
            }
        }

        if (fallo) {
            AlertBanner(texto = stringResource(R.string.qr_error_generar), tipo = BannerTipo.ERROR)
        }

        BotonPrincipal(
            texto = stringResource(R.string.qr_compartir),
            icono = Icons.Rounded.Share,
            habilitado = bitmap != null,
            onClick = {
                val imagen = bitmap ?: return@BotonPrincipal
                alcance.launch {
                    try {
                        val uri = withContext(Dispatchers.IO) { QrSticker.guardarPng(contexto, imagen, tablet.serie) }
                        QrSticker.compartir(contexto, uri, contexto.getString(R.string.qr_compartir))
                    } catch (e: Exception) {
                        fallo = true
                    }
                }
            },
        )
        BotonSecundario(texto = stringResource(R.string.qr_ir_ficha), onClick = onIrAFicha)

        SectionCard(titulo = stringResource(R.string.qr_contenido)) {
            Text(
                text = QrSticker.contenido(tablet.specs),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
