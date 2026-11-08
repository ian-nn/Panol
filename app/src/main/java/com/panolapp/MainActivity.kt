package com.panolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.panolapp.ui.navigation.PanolNavHost
import com.panolapp.ui.theme.PanolTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PanolTheme {
                PanolRoot()
            }
        }
    }
}

@Composable
private fun PanolRoot() {
    val contexto = LocalContext.current
    val app = contexto.applicationContext as PanolApp
    val snackbar = remember { SnackbarHostState() }

    // Los mensajes (p. ej. "Tablet despachada") sobreviven a la navegación entre pantallas.
    LaunchedEffect(Unit) {
        app.mensajes.collect { mensaje ->
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(contexto.getString(mensaje.res, *mensaje.args.toTypedArray()))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar, modifier = Modifier.navigationBarsPadding()) },
    ) { _ ->
        PanolNavHost()
    }
}
