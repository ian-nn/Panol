package com.panolapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.panolapp.domain.EstadoTablet

// Paleta alineada con el ícono de la app (azul #1B4DB3 + verde azulado #00695C).
private val LightColors = lightColorScheme(
    primary = Color(0xFF1B4DB3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE6FF),
    onPrimaryContainer = Color(0xFF00164A),
    secondary = Color(0xFF00695C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC4F0E8),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFB26A00),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE2B8),
    onTertiaryContainer = Color(0xFF2B1700),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF15191F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15191F),
    surfaceVariant = Color(0xFFE3E8F2),
    onSurfaceVariant = Color(0xFF434A58),
    outline = Color(0xFF7A8294),
    outlineVariant = Color(0xFFCBD2E0),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FAFE),
    surfaceContainer = Color(0xFFF1F4FA),
    surfaceContainerHigh = Color(0xFFEBEFF7),
    surfaceContainerHighest = Color(0xFFE5E9F2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002B75),
    primaryContainer = Color(0xFF123C8E),
    onPrimaryContainer = Color(0xFFDCE6FF),
    secondary = Color(0xFF6FD8C5),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF004F44),
    onSecondaryContainer = Color(0xFFC4F0E8),
    tertiary = Color(0xFFFFB95C),
    onTertiary = Color(0xFF472A00),
    tertiaryContainer = Color(0xFF5B3600),
    onTertiaryContainer = Color(0xFFFFE2B8),
    background = Color(0xFF10131A),
    onBackground = Color(0xFFE3E6EE),
    surface = Color(0xFF171B24),
    onSurface = Color(0xFFE3E6EE),
    surfaceVariant = Color(0xFF2B303C),
    onSurfaceVariant = Color(0xFFC2C8D6),
    outline = Color(0xFF8C93A3),
    outlineVariant = Color(0xFF3B4150),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surfaceContainerLowest = Color(0xFF0C0F15),
    surfaceContainerLow = Color(0xFF141820),
    surfaceContainer = Color(0xFF1A1F29),
    surfaceContainerHigh = Color(0xFF222732),
    surfaceContainerHighest = Color(0xFF2B303C),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun PanolTheme(
    oscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (oscuro) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = contenido,
    )
}

/** Color de texto + fondo para representar un estado (chips, contadores). */
@Immutable
data class EstadoPaleta(val contenido: Color, val contenedor: Color)

@Composable
fun EstadoTablet.paleta(): EstadoPaleta {
    val oscuro = isSystemInDarkTheme()
    return when (this) {
        EstadoTablet.EN_PANOL ->
            if (oscuro) EstadoPaleta(Color(0xFF7FD9A5), Color(0xFF0F3D26))
            else EstadoPaleta(Color(0xFF1B7F4B), Color(0xFFDDF3E6))
        EstadoTablet.USO_LOCAL ->
            if (oscuro) EstadoPaleta(Color(0xFFAEC6FF), Color(0xFF123C8E))
            else EstadoPaleta(Color(0xFF1B4DB3), Color(0xFFDCE6FF))
        EstadoTablet.USO_EXTERNO ->
            if (oscuro) EstadoPaleta(Color(0xFFD2BBFF), Color(0xFF3F2273))
            else EstadoPaleta(Color(0xFF6B3FB8), Color(0xFFEBDDFF))
        EstadoTablet.SERVICIO_TECNICO ->
            if (oscuro) EstadoPaleta(Color(0xFFFFB4AB), Color(0xFF6E1410))
            else EstadoPaleta(Color(0xFFB3261E), Color(0xFFFFDAD6))
    }
}
