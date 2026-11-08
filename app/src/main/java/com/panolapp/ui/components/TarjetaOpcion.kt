package com.panolapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Opción excluyente con ícono, título y descripción (tipo de uso, estado físico). */
@Composable
fun TarjetaOpcion(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colores = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (seleccionada) colores.primaryContainer else colores.surface,
        border = BorderStroke(
            width = if (seleccionada) 2.dp else 1.dp,
            color = if (seleccionada) colores.primary else colores.outlineVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icono,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = if (seleccionada) colores.primary else colores.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall)
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = colores.onSurfaceVariant,
                )
            }
            RadioButton(selected = seleccionada, onClick = null)
        }
    }
}
