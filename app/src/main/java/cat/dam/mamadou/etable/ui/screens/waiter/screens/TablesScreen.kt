@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.DimensionUtils
import cat.dam.mamadou.etable.ui.screens.waiter.components.tables.TarjetaMesa
import cat.dam.mamadou.etable.ui.screens.waiter.components.tables.DetallesMesa
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Pantalla de gestión de mesas optimizada para tablets
 * Sin componentes de IA para mejor rendimiento
 */
@Composable
fun TablesScreen(navController: NavController) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    
    // Considerar tablet si el ancho mínimo es > 600dp
    val isTablet = minOf(screenWidth, screenHeight) > 600
    
    // Dos paneles en tablet horizontal o en móvil horizontal muy ancho
    val shouldUseLandscapeLayout = (isTablet && isLandscape) || (isLandscape && screenWidth > 1000)
    
    val firebaseManager = remember { FirebaseManager() }
    
    // Estado optimizado con listas inmutables para minimizar recomposiciones
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val mesas = remember { mutableStateListOf<Mesa>() }
    var mesaSeleccionada by remember { mutableStateOf<Mesa?>(null) }
    
    // Referencias a listeners para limpieza adecuada
    val tableListenerRef = remember { mutableStateOf<ListenerRegistration?>(null) }
    val reservationListenerRef = remember { mutableStateOf<ListenerRegistration?>(null) }
    
    // Scope para operaciones coroutine
    val scope = rememberCoroutineScope()

    // Extraer los strings necesarios fuera de los composables
    val stateOccupied = context.getString(R.string.state_occupied)
    val stateReserved = context.getString(R.string.state_reserved)
    val stateFree = context.getString(R.string.state_free)
    val updateErrorMsg = context.getString(R.string.error_update_table_firebase)

    // Calcular estado derivado para reducir recomposiciones
    val mesasGrouped by remember(mesas) {
        derivedStateOf {
            // Simplemente ordenar por ID para mantener posiciones fijas
            mesas.sortedBy { it.id }
        }
    }

    // Función optimizada para actualizar clientes
    fun actualizarClientes(mesaId: Int, nuevoNumeroClientes: Int) {
        val index = mesas.indexOfFirst { it.id == mesaId }
        if (index == -1) return

        val mesaActual = mesas[index]
        // Evitar actualización si no hay cambios
        if (mesaActual.clientes == nuevoNumeroClientes) return
        
        val nuevoEstado = when {
            nuevoNumeroClientes > 0 -> stateOccupied
            mesaActual.estado == stateReserved -> stateReserved
            else -> stateFree
        }

        // Actualizar localmente primero para UI responsiva
        val mesaActualizada = mesaActual.copy(
            estado = nuevoEstado,
            clientes = nuevoNumeroClientes
        )
        mesas[index] = mesaActualizada
        
        // Actualizar selección si necesario
        if (mesaSeleccionada?.id == mesaId) {
            mesaSeleccionada = mesaActualizada
        }

        // Actualizar en Firebase posteriormente
        scope.launch {
            try {
                firebaseManager.tableManager.updateTableStatus(
                    tableId = mesaId,
                    estado = nuevoEstado,
                    clientes = nuevoNumeroClientes
                ) { success ->
                    if (!success) {
                        errorMessage = updateErrorMsg
                    }
                }
            } catch (e: Exception) {
                Log.e("TablesScreen", "Error actualizando mesa: ${e.message}")
            }
        }
    }

    // Optimizado: usar LaunchedEffect para inicialización
    LaunchedEffect(key1 = Unit) {
        isLoading = true
        errorMessage = null

        try {
            // Configurar listener eficiente para tablas
            tableListenerRef.value = firebaseManager.tableManager.listenToTables { tablesList ->
                mesas.clear()
                mesas.addAll(tablesList)
                isLoading = false
                Log.d("TablesScreen", "📋 Tables loaded: ${tablesList.size} mesas")
            }
            
            // Listener eficiente para reservas
            val fechaActual = java.util.Date()
            reservationListenerRef.value = firebaseManager.reservationManager
                .listenToReservationsByDate(fechaActual) { reservations ->
                    Log.d("TablesScreen", "📅 Reservations updated: ${reservations.size}")
                    // Procesamiento por lotes
                    val tablesToUpdate = mutableMapOf<Int, Mesa>()
                    
                    reservations.forEach { reserva ->
                        val mesaId = (reserva["tableId"] as? Number)?.toInt() ?: return@forEach
                        val mesa = mesas.find { it.id == mesaId } ?: return@forEach
                        
                        if (mesa.clientes == 0 && mesa.estado != stateReserved) {
                            tablesToUpdate[mesaId] = mesa.copy(estado = stateReserved)
                        }
                    }
                    
                    // Actualizar localmente
                    tablesToUpdate.forEach { (mesaId, mesa) ->
                        val index = mesas.indexOfFirst { it.id == mesaId }
                        if (index != -1) {
                            mesas[index] = mesa
                            
                            // También actualizar en Firebase (optimizado en lote si es posible)
                            firebaseManager.tableManager.updateTableStatus(
                                tableId = mesaId,
                                estado = stateReserved,
                                clientes = mesa.clientes
                            ) { _ -> }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e("TablesScreen", "Error inicializando datos: ${e.message}", e)
            errorMessage = context.getString(R.string.error_loading_tables, e.message)
            isLoading = false
        }
    }

    // Limpieza de recursos cuando se destruye la pantalla
    DisposableEffect(Unit) {
        onDispose {
            Log.d("TablesScreen", "🧹 Cleaning up listeners")
            tableListenerRef.value?.remove()
            reservationListenerRef.value?.remove()
        }
    }

    // UI principal basada en estado de carga
    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
        errorMessage != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage!!, color = Color.Red)
            }
        }
        else -> {
            if (shouldUseLandscapeLayout) {
                HorizontalLayout(
                    mesasGrouped = mesasGrouped,
                    mesaSeleccionada = mesaSeleccionada,
                    onMesaSelected = { mesa -> 
                        Log.d("TablesScreen", "Mesa seleccionada: ${mesa.id}")
                        mesaSeleccionada = mesa 
                    },
                    onClientesChanged = { mesaId, clientes -> 
                        Log.d("TablesScreen", "Clientes changed: mesa $mesaId -> $clientes")
                        actualizarClientes(mesaId, clientes) 
                    },
                    onNuevaComanda = { etapaSiguiente ->
                        Log.d("TablesScreen", "Nueva comanda: etapa $etapaSiguiente")
                        etapaSiguiente?.let { etapa ->
                            val route = "orders/${mesaSeleccionada!!.id}?etapa=$etapa"
                            Log.d("TablesScreen", "Navigating to: $route")
                            navController.navigate(route)
                        }
                    }
                )
            } else {
                VerticalLayout(
                    mesasGrouped = mesasGrouped,
                    mesaSeleccionada = mesaSeleccionada,
                    onMesaSelected = { mesa -> 
                        Log.d("TablesScreen", "Mesa seleccionada: ${mesa.id}")
                        mesaSeleccionada = mesa 
                    },
                    onClientesChanged = { mesaId, clientes -> 
                        Log.d("TablesScreen", "Clientes changed: mesa $mesaId -> $clientes")
                        actualizarClientes(mesaId, clientes) 
                    },
                    onNuevaComanda = { etapaSiguiente ->
                        Log.d("TablesScreen", "Nueva comanda: etapa $etapaSiguiente")
                        etapaSiguiente?.let { etapa ->
                            val route = "orders/${mesaSeleccionada!!.id}?etapa=$etapa"
                            Log.d("TablesScreen", "Navigating to: $route")
                            navController.navigate(route)
                        }
                    }
                )
            }
        }
    }
}

// EXTRAER los layouts como funciones separadas para mejor organización
@Composable
private fun HorizontalLayout(
    mesasGrouped: List<Mesa>,
    mesaSeleccionada: Mesa?,
    onMesaSelected: (Mesa) -> Unit,
    onClientesChanged: (Int, Int) -> Unit,
    onNuevaComanda: (String?) -> Unit
) {
    Log.d("TablesScreen", "🎨 Rendering HorizontalLayout with ${mesasGrouped.size} mesas")
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // IZQUIERDA: Panel de mesas - MÁS ANCHO Y CENTRADO
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.3f)
                .padding(end = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                
                // GRID CENTRADO de mesas 2x2
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth(0.85f)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = mesasGrouped,
                                key = { it.id }
                            ) { mesa ->
                                TarjetaMesa(
                                    mesa = mesa,
                                    onClick = { 
                                        Log.d("TablesScreen", "🖱️ Mesa ${mesa.id} clicked in HorizontalLayout")
                                        onMesaSelected(mesa) 
                                    },
                                    isSelected = mesaSeleccionada?.id == mesa.id,
                                    modifier = Modifier.aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // DERECHA: Panel de detalles - MÁS COMPACTO
        Card(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (mesaSeleccionada != null) {
                    Log.d("TablesScreen", "📋 Showing details for mesa ${mesaSeleccionada.id}")

                    
                    // DETALLES con scroll si es necesario
                    Box(modifier = Modifier.weight(1f)) {
                        DetallesMesa(
                            mesa = mesaSeleccionada,
                            onClientesChanged = { clientes -> 
                                onClientesChanged(mesaSeleccionada.id, clientes) 
                            },
                            onNuevaComanda = onNuevaComanda
                        )
                    }
                } else {
                    Log.d("TablesScreen", "❓ No mesa selected - showing empty state")
                    
                    // ESTADO VACÍO mejorado
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableRestaurant,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = stringResource(R.string.select_table_for_details),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Text(
                                text = stringResource(R.string.tables_tap_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalLayout(
    mesasGrouped: List<Mesa>,
    mesaSeleccionada: Mesa?,
    onMesaSelected: (Mesa) -> Unit,
    onClientesChanged: (Int, Int) -> Unit,
    onNuevaComanda: (String?) -> Unit
) {
    Log.d("TablesScreen", "📱 Rendering VerticalLayout with ${mesasGrouped.size} mesas")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(
                    items = mesasGrouped,
                    key = { it.id }
                ) { mesa ->
                    TarjetaMesa(
                        mesa = mesa,
                        onClick = { 
                            Log.d("TablesScreen", "🖱️ Mesa ${mesa.id} clicked in VerticalLayout")
                            onMesaSelected(mesa) 
                        },
                        isSelected = mesaSeleccionada?.id == mesa.id
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            if (mesaSeleccionada != null) {
                Log.d("TablesScreen", "📋 Showing details for mesa ${mesaSeleccionada.id} in vertical")
                
                DetallesMesa(
                    mesa = mesaSeleccionada,
                    onClientesChanged = { clientes -> 
                        onClientesChanged(mesaSeleccionada.id, clientes) 
                    },
                    onNuevaComanda = onNuevaComanda
                )
            } else {
                Log.d("TablesScreen", "❓ No mesa selected in vertical - showing placeholder")
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.select_table_for_details),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}