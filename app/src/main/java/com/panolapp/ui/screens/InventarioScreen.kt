package com.panolapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.panolapp.R
import com.panolapp.domain.EstadoTablet
import com.panolapp.domain.Tablet
import com.panolapp.ui.components.EstadoChip
import com.panolapp.ui.components.EstadoVacio
import com.panolapp.ui.components.PanolTopBar
import com.panolapp.ui.etiquetaRes
import com.panolapp.ui.vm.InventarioViewModel

@Composable
fun InventarioScreen(
    onVolver: () -> Unit,
    onAbrir: (String) -> Unit,
    vm: InventarioViewModel = viewModel(),
) {
    val filtro by vm.filtro.collectAsStateWithLifecycle()
    val tablets by vm.tablets.collectAsStateWithLifecycle()
    var texto by rememberSaveable { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { PanolTopBar(titulo = stringResource(R.string.inventario_titulo), onVolver = onVolver) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = texto,
                onValueChange = {
                    texto = it
                    vm.buscar(it)
                },
                placeholder = { Text(stringResource(R.string.inventario_buscar)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (texto.isNotEmpty()) {
                        IconButton(onClick = {
                            texto = ""
                            vm.buscar("")
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.limpiar))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = filtro == null,
                        onClick = { vm.elegirFiltro(null) },
                        label = { Text(stringResource(R.string.inventario_todas)) },
                    )
                }
                items(EstadoTablet.entries) { estado ->
                    FilterChip(
                        selected = filtro == estado,
                        onClick = { vm.elegirFiltro(estado) },
                        label = { Text(stringResource(estado.etiquetaRes())) },
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                val lista = tablets
                when {
                    lista == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    lista.isEmpty() -> EstadoVacio(
                        icono = Icons.Rounded.Inventory2,
                        titulo = stringResource(R.string.inventario_vacio_titulo),
                        texto = stringResource(R.string.inventario_vacio_texto),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp, end = 20.dp, bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(lista, key = { it.serie }) { tablet ->
                            FilaTablet(tablet, onClick = { onAbrir(tablet.serie) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaTablet(tablet: Tablet, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tablet.serie, style = MaterialTheme.typography.titleMedium)
                val resumen = listOf(tablet.specs.procesador, tablet.specs.ram, tablet.specs.almacenamiento)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (resumen.isNotEmpty()) {
                    Text(
                        resumen,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val legajo = tablet.legajoActual
                if (legajo != null) {
                    Text(
                        stringResource(R.string.mov_empleado, legajo, tablet.nombreActual ?: "—"),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            EstadoChip(tablet.estado)
        }
    }
}
