package com.panolapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.panolapp.R
import com.panolapp.domain.EstadoTablet
import com.panolapp.ui.etiquetaRes
import com.panolapp.ui.theme.paleta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanolTopBar(
    titulo: String,
    onVolver: (() -> Unit)?,
    modifier: Modifier = Modifier,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Text(
                text = titulo,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.volver),
                    )
                }
            }
        },
        actions = acciones,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = modifier,
    )
}

@Composable
fun EstadoChip(estado: EstadoTablet, modifier: Modifier = Modifier) {
    val paleta = estado.paleta()
    Surface(color = paleta.contenedor, contentColor = paleta.contenido, shape = CircleShape, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(paleta.contenido, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(estado.etiquetaRes()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun SectionCard(
    titulo: String?,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (titulo != null) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            contenido()
        }
    }
}

@Composable
fun SpecRow(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = valor.ifBlank { stringResource(R.string.no_informado) },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(items: List<String>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) {
        Text(
            text = stringResource(R.string.no_informado),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                Text(
                    text = item,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

enum class BannerTipo { INFO, ADVERTENCIA, ERROR }

@Composable
fun AlertBanner(
    texto: String,
    tipo: BannerTipo,
    modifier: Modifier = Modifier,
    titulo: String? = null,
) {
    val colores = MaterialTheme.colorScheme
    val (fondo, tinta, icono) = when (tipo) {
        BannerTipo.INFO ->
            Triple(colores.primaryContainer, colores.onPrimaryContainer, Icons.Rounded.Info)
        BannerTipo.ADVERTENCIA ->
            Triple(colores.tertiaryContainer, colores.onTertiaryContainer, Icons.Rounded.Warning)
        BannerTipo.ERROR ->
            Triple(colores.errorContainer, colores.onErrorContainer, Icons.Rounded.Warning)
    }
    Surface(
        color = fondo,
        contentColor = tinta,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (titulo != null) {
                    Text(titulo, style = MaterialTheme.typography.titleSmall)
                }
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    cargando: Boolean = false,
    colores: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        colors = colores,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Spacer(Modifier.width(10.dp))
        } else if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texto, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BotonSecundario(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texto, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
fun Campo(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    ayuda: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    capitalizacion: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    lineas: Int = 1,
    habilitado: Boolean = true,
    maxCaracteres: Int = 120,
) {
    val textoApoyo = error ?: ayuda
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> if (nuevo.length <= maxCaracteres) onCambio(nuevo) },
        label = { Text(etiqueta) },
        isError = error != null,
        supportingText = if (textoApoyo != null) {
            { Text(textoApoyo) }
        } else {
            null
        },
        singleLine = lineas == 1,
        minLines = lineas,
        maxLines = if (lineas == 1) 1 else lineas + 2,
        keyboardOptions = KeyboardOptions(
            keyboardType = teclado,
            capitalization = capitalizacion,
            imeAction = imeAction,
        ),
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun EstadoVacio(
    icono: ImageVector,
    titulo: String,
    texto: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
