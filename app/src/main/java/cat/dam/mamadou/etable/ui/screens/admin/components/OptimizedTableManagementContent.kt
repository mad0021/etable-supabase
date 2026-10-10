package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa

@Composable
fun OptimizedTableManagementContent(
    tables: List<Mesa>,
    isLoading: Boolean,
    selectedState: String,
    onStateSelected: (String) -> Unit,
    onEditTable: (Mesa) -> Unit,
    onDeleteTable: (Mesa) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Selector de estados optimizado para tablets con textos localizados
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TableStates.ALL.forEach { state ->
                OptimizedCategoryChip(
                    category = state,
                    categoryDisplay = TableStates.getLocalizedText(context, state),
                    selected = state == selectedState,
                    onClick = { onStateSelected(state) }
                )
            }
        }
        
        // Mostrar carga o contenido
        if (isLoading) {
            AdminLoading()
        } else {
            // Filtrar mesas de manera optimizada
            val filteredTables = remember(tables, selectedState) {
                if (selectedState.isEmpty()) tables else tables.filter { 
                    TableStates.fromLegacyValue(it.estado) == TableStates.fromLegacyValue(selectedState) 
                }
            }
            
            if (filteredTables.isEmpty()) {
                // Estado vacío mejorado
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.table_management_empty, selectedState),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Grid optimizado para tablets - columnas adaptativas según el tamaño
                val columns = when {
                    isLargeScreen && configuration.screenWidthDp >= 840 -> 4
                    isLargeScreen -> 3
                    else -> 2
                }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    // Espacio inferior extra para que el FAB no tape la última fila
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = filteredTables,
                        key = { it.id } // Usar key para optimizar recomposiciones
                    ) { table ->
                        OptimizedTableCard(
                            table = table,
                            onClick = { onEditTable(table) },
                            onDelete = { onDeleteTable(table) },
                            modifier = adminAnimateItem()
                        )
                    }
                }
            }
        }
    }
}

// Modificar el chip para aceptar texto localizado
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptimizedCategoryChip(
    category: String,
    categoryDisplay: String, // Texto localizado para mostrar
    selected: Boolean,
    onClick: () -> Unit
) {
    // Usar categoryDisplay para el texto visible; ToggleButton con morph de forma al marcarse
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonDefaults.shapes(),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Text(categoryDisplay)
    }
}