package com.panolapp.ui.camera

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Vista de cámara que apunta, enfoca y lee códigos QR.
 *
 *  - Apertura: CameraX (cámara trasera) atada al ciclo de vida de la pantalla.
 *  - Enfoque: autoenfoque continuo (por defecto en CameraX) + enfoque manual al tocar la imagen.
 *  - Lectura: ML Kit Barcode Scanning (solo formato QR), 100 % en el dispositivo, sin internet.
 *
 * [habilitado] pausa la lectura (p. ej. mientras se muestra un error) sin cerrar la cámara.
 */
@Composable
fun CameraQrScanner(
    habilitado: Boolean,
    linterna: Boolean,
    onQr: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCamaraLista: (tieneFlash: Boolean) -> Unit = {},
    onFalloCamara: () -> Unit = {},
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val habilitadoFlag = remember { AtomicBoolean(habilitado) }
    SideEffect { habilitadoFlag.set(habilitado) }
    val onQrActual by rememberUpdatedState(onQr)
    val onListaActual by rememberUpdatedState(onCamaraLista)
    val onFalloActual by rememberUpdatedState(onFalloCamara)

    var camara by remember { mutableStateOf<Camera?>(null) }
    var puntoFoco by remember { mutableStateOf<Offset?>(null) }

    val previewView = remember {
        PreviewView(contexto).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner) {
        val ejecutor = Executors.newSingleThreadExecutor()
        val lector = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
        val futuro = ProcessCameraProvider.getInstance(contexto)
        futuro.addListener(
            {
                try {
                    val proveedor = futuro.get()
                    val vistaPrevia = Preview.Builder().build()
                    vistaPrevia.setSurfaceProvider(previewView.surfaceProvider)
                    // 1280x720 (en vez de los 640x480 por defecto): un QR con ficha técnica
                    // completa es denso y necesita más píxeles para decodificarse con una etiqueta chica.
                    val resolucion = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build()
                    val analisis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolucion)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analisis.setAnalyzer(ejecutor) { imagen ->
                        analizar(imagen, lector, habilitadoFlag) { valor -> onQrActual(valor) }
                    }
                    proveedor.unbindAll()
                    val enlazada = proveedor.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        vistaPrevia,
                        analisis,
                    )
                    camara = enlazada
                    onListaActual(enlazada.cameraInfo.hasFlashUnit())
                } catch (e: Exception) {
                    onFalloActual()
                }
            },
            ContextCompat.getMainExecutor(contexto),
        )
        onDispose {
            if (futuro.isDone) {
                runCatching { futuro.get().unbindAll() }
            }
            camara = null
            ejecutor.shutdown()
            lector.close()
        }
    }

    LaunchedEffect(camara, linterna) {
        val actual = camara ?: return@LaunchedEffect
        if (actual.cameraInfo.hasFlashUnit()) {
            actual.cameraControl.enableTorch(linterna)
        }
    }

    LaunchedEffect(puntoFoco) {
        if (puntoFoco != null) {
            delay(900)
            puntoFoco = null
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(camara) {
                    detectTapGestures { toque ->
                        val actual = camara ?: return@detectTapGestures
                        val punto = previewView.meteringPointFactory.createPoint(toque.x, toque.y)
                        actual.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(punto).build())
                        puntoFoco = toque
                    }
                },
        )
        val foco = puntoFoco
        if (foco != null) {
            val radio = with(LocalDensity.current) { 34.dp.roundToPx() }
            Box(
                Modifier
                    .offset { IntOffset(foco.x.toInt() - radio, foco.y.toInt() - radio) }
                    .size(68.dp)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun analizar(
    imagen: ImageProxy,
    lector: BarcodeScanner,
    habilitado: AtomicBoolean,
    alLeer: (String) -> Unit,
) {
    val media = imagen.image
    if (media == null || !habilitado.get()) {
        imagen.close()
        return
    }
    val entrada = InputImage.fromMediaImage(media, imagen.imageInfo.rotationDegrees)
    lector.process(entrada)
        .addOnSuccessListener { codigos ->
            val valor = codigos.firstNotNullOfOrNull { codigo ->
                codigo.rawValue?.takeIf { it.isNotBlank() }
            }
            // compareAndSet: si varios cuadros en vuelo detectan el mismo QR, solo se informa uno.
            if (valor != null && habilitado.compareAndSet(true, false)) {
                alLeer(valor)
            }
        }
        .addOnCompleteListener { imagen.close() }
}
