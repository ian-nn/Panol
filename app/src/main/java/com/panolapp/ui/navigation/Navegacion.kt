package com.panolapp.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.panolapp.domain.EstadoTablet
import com.panolapp.ui.screens.AltaScreen
import com.panolapp.ui.screens.DespachoScreen
import com.panolapp.ui.screens.DevolucionScreen
import com.panolapp.ui.screens.EscanerScreen
import com.panolapp.ui.screens.FichaScreen
import com.panolapp.ui.screens.InicioScreen
import com.panolapp.ui.screens.InventarioScreen
import com.panolapp.ui.screens.VerQrScreen
import com.panolapp.ui.vm.ResultadoEscaneo

/** Qué se quiere hacer con la tablet que se va a escanear. */
enum class ModoEscaneo(val argumento: String) {
    CONSULTA("consulta"),
    DESPACHO("despacho"),
    DEVOLUCION("devolucion"),
    ;

    companion object {
        fun desdeArgumento(valor: String?): ModoEscaneo =
            entries.firstOrNull { it.argumento == valor } ?: CONSULTA
    }
}

object Rutas {
    const val INICIO = "inicio"
    const val ESCANER = "escaner/{modo}"
    const val FICHA = "ficha/{serie}"
    const val DESPACHO = "despacho/{serie}"
    const val DEVOLUCION = "devolucion/{serie}"
    const val ALTA = "alta?serie={serie}"
    const val QR = "qr/{serie}?nuevo={nuevo}"
    const val INVENTARIO = "inventario?estado={estado}"

    fun escaner(modo: ModoEscaneo) = "escaner/${modo.argumento}"
    fun ficha(serie: String) = "ficha/$serie"
    fun despacho(serie: String) = "despacho/$serie"
    fun devolucion(serie: String) = "devolucion/$serie"
    fun alta(serie: String = "") = if (serie.isEmpty()) "alta" else "alta?serie=$serie"
    fun qr(serie: String, nuevo: Boolean = false) = "qr/$serie?nuevo=$nuevo"
    fun inventario(estado: EstadoTablet? = null) =
        if (estado == null) "inventario" else "inventario?estado=${estado.name}"
}

private const val DURACION = 240

@Composable
fun PanolNavHost(navController: NavHostController = rememberNavController()) {

    fun volver() {
        navController.popBackStack()
    }

    /**
     * Tras operar sobre una tablet se muestra su ficha actualizada. Si la pantalla anterior ya es
     * esa ficha se vuelve a ella (evita fichas duplicadas en la pila); si no, se la reemplaza.
     */
    fun irAFicha(serie: String, desde: String) {
        val anterior = navController.previousBackStackEntry
        val vieneDeLaFicha = anterior?.destination?.route == Rutas.FICHA &&
            anterior.arguments?.getString("serie") == serie
        if (vieneDeLaFicha) {
            navController.popBackStack()
        } else {
            navController.navigate(Rutas.ficha(serie)) {
                popUpTo(desde) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.INICIO,
        enterTransition = { fadeIn(tween(DURACION)) + slideInHorizontally(tween(DURACION)) { it / 12 } },
        exitTransition = { fadeOut(tween(DURACION / 2)) },
        popEnterTransition = { fadeIn(tween(DURACION)) },
        popExitTransition = { fadeOut(tween(DURACION / 2)) + slideOutHorizontally(tween(DURACION)) { it / 12 } },
    ) {
        composable(Rutas.INICIO) {
            InicioScreen(
                onEscanear = { modo -> navController.navigate(Rutas.escaner(modo)) },
                onAlta = { navController.navigate(Rutas.alta()) },
                onInventario = { estado -> navController.navigate(Rutas.inventario(estado)) },
            )
        }

        composable(
            route = Rutas.ESCANER,
            arguments = listOf(navArgument("modo") { type = NavType.StringType }),
        ) { entrada ->
            val modo = ModoEscaneo.desdeArgumento(entrada.arguments?.getString("modo"))
            EscanerScreen(
                modo = modo,
                onVolver = ::volver,
                onResultado = { resultado ->
                    val destino = when (resultado) {
                        is ResultadoEscaneo.NoRegistrada -> Rutas.alta(resultado.serie)
                        is ResultadoEscaneo.Encontrada -> when (modo) {
                            ModoEscaneo.CONSULTA -> Rutas.ficha(resultado.serie)
                            ModoEscaneo.DESPACHO -> Rutas.despacho(resultado.serie)
                            ModoEscaneo.DEVOLUCION -> Rutas.devolucion(resultado.serie)
                        }
                    }
                    navController.navigate(destino) {
                        popUpTo(Rutas.ESCANER) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Rutas.FICHA,
            arguments = listOf(navArgument("serie") { type = NavType.StringType }),
        ) { entrada ->
            val serie = entrada.arguments?.getString("serie").orEmpty()
            FichaScreen(
                onVolver = ::volver,
                onDespachar = { navController.navigate(Rutas.despacho(serie)) },
                onDevolver = { navController.navigate(Rutas.devolucion(serie)) },
                onVerQr = { navController.navigate(Rutas.qr(serie)) },
            )
        }

        composable(
            route = Rutas.DESPACHO,
            arguments = listOf(navArgument("serie") { type = NavType.StringType }),
        ) { entrada ->
            val serie = entrada.arguments?.getString("serie").orEmpty()
            DespachoScreen(
                onVolver = ::volver,
                onListo = { irAFicha(serie, Rutas.DESPACHO) },
            )
        }

        composable(
            route = Rutas.DEVOLUCION,
            arguments = listOf(navArgument("serie") { type = NavType.StringType }),
        ) { entrada ->
            val serie = entrada.arguments?.getString("serie").orEmpty()
            DevolucionScreen(
                onVolver = ::volver,
                onListo = { irAFicha(serie, Rutas.DEVOLUCION) },
            )
        }

        composable(
            route = Rutas.ALTA,
            arguments = listOf(
                navArgument("serie") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            AltaScreen(
                onVolver = ::volver,
                onAltaCreada = { serie ->
                    navController.navigate(Rutas.qr(serie, nuevo = true)) {
                        popUpTo(Rutas.ALTA) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Rutas.QR,
            arguments = listOf(
                navArgument("serie") { type = NavType.StringType },
                navArgument("nuevo") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entrada ->
            val serie = entrada.arguments?.getString("serie").orEmpty()
            val nuevo = entrada.arguments?.getBoolean("nuevo") ?: false
            VerQrScreen(
                nueva = nuevo,
                onVolver = ::volver,
                onIrAFicha = { irAFicha(serie, Rutas.QR) },
            )
        }

        composable(
            route = Rutas.INVENTARIO,
            arguments = listOf(
                navArgument("estado") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            InventarioScreen(
                onVolver = ::volver,
                onAbrir = { serie -> navController.navigate(Rutas.ficha(serie)) },
            )
        }
    }
}
