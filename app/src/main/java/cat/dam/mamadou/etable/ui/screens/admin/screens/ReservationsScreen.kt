@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.admin.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminLoading
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminSwitch
import cat.dam.mamadou.etable.ui.screens.admin.components.adminAnimateItem
import cat.dam.mamadou.etable.ui.screens.admin.components.rememberAdminReduceMotion
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.ReservationConstants
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.ReservationsViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.ReservationsViewModelFactory
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.TableStateUtils
import cat.dam.mamadou.etable.utils.TextUtils
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import java.text.SimpleDateFormat
import java.util.*

// Modelo de datos para reservas (usando el modelo de FirebaseManager)
typealias Reservation = cat.dam.mamadou.etable.utils.Reservation

// Efecto para escuchar notificaciones de descanso pero solo con sonido
@Composable
private fun UseBreakSoundOnlyListener(firebaseManager: FirebaseManager) {
    DisposableEffect(Unit) {
        // Asegurarse de que las notificaciones visuales están desactivadas
        BreakNotificationManager.disableModalNotifications()
        
        // El listener para solicitudes de descanso ya está configurado en AdminDashboardActivity
        // y se encarga del sonido y la vibración
        
        onDispose { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Optimización: solo crear una instancia del firebaseManager
    val firebaseManager = remember { FirebaseManager() }
    val viewModel: ReservationsViewModel = viewModel(factory = ReservationsViewModelFactory(context))
    
    // Usar nuestro efecto personalizado que no devuelve un estado visual
    UseBreakSoundOnlyListener(firebaseManager)
    
    // Estados para la gestión de reservas
    val reservations by viewModel.reservations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Optimización: usar derivedStateOf para operaciones costosas
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReservation by remember { mutableStateOf<Reservation?>(null) }
    
    // Estados para las mesas disponibles
    var availableTables by remember { mutableStateOf<List<Mesa>>(emptyList()) }
    var isLoadingTables by remember { mutableStateOf(true) }
    
    // Cargar mesas disponibles al iniciar (el listener se quita al salir de la pantalla)
    DisposableEffect(Unit) {
        val tablesListener = try {
            firebaseManager.tableManager.listenToTables { tables ->
                // Usar la constante TableStates.FREE en lugar del literal "LLIURE"
                availableTables = tables.filter { 
                    TableStates.fromLegacyValue(it.estado) == TableStates.FREE 
                }
                isLoadingTables = false
                
                // AÑADIR: Log para debug
                Log.d("ReservationsScreen", "Mesas disponibles actualizadas: ${availableTables.size}")
            }
        } catch (e: Exception) {
            Log.e("ReservationsScreen", "Error loading tables: ${e.message}", e)
            isLoadingTables = false
            null
        }
        onDispose { tablesListener?.remove() }
    }
    
    // Función para añadir o editar una reserva
    val saveReservation = remember<(Reservation, Boolean) -> Unit> {
        { reservation, isNew ->
            val reservationData = mapOf(
                "id" to reservation.id,
                "tableId" to reservation.tableId,
                "fecha" to reservation.fecha,
                "hora" to reservation.hora,
                "nombreCliente" to reservation.nombreCliente,
                "numPersonas" to reservation.numPersonas,
                "telefono" to reservation.telefono,
                "estado" to reservation.estado
            )
            
            try {
                // Primera parte: guardar la reserva
                if (isNew) {
                    firebaseManager.reservationManager.createReservation(reservationData)
                        .addOnSuccessListener {
                            // AÑADIR ESTA PARTE: Actualizar estado de la mesa al crear reserva
                            firebaseManager.tableManager.updateTableStatus(
                                tableId = reservation.tableId,
                                estado = TableStates.RESERVED,  // Usar constante, no literal "RESERVADA"
                                clientes = 0
                            ) { success ->
                                if (!success) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error_update_table_firebase),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                // Opcional: recargar datos
                                viewModel.loadReservations()
                            }
                        }
                } else {
                    firebaseManager.reservationManager.updateReservation(reservation.id, reservationData)
                        .addOnSuccessListener {
                            // También actualizar mesa en caso de cambio
                            firebaseManager.tableManager.updateTableStatus(
                                tableId = reservation.tableId,
                                estado = TableStates.RESERVED,
                                clientes = 0
                            ) { _ -> }
                        }
                }
            } catch (e: Exception) {
                Log.e("ReservationsScreen", "Error saving reservation: ${e.message}", e)
                Toast.makeText(
                    context, 
                    context.getString(R.string.reservation_error_saving, e.message), 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    // Función para eliminar una reserva
    val deleteReservation = remember<(Reservation) -> Unit> {
        { reservation ->
            try {
                firebaseManager.reservationManager.deleteReservation(reservation.id)
                    .addOnSuccessListener {
                        // Actualizar el estado de la mesa para LLIURE
                        firebaseManager.tableManager.updateTableStatus(
                            reservation.tableId, 
                            "LLIURE", 
                            0
                        ) { _ ->
                            // Recargar las reservas
                            viewModel.loadReservations()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReservationsScreen", "Error deleting reservation: ${e.message}", e)
                        Toast.makeText(
                            context, 
                            context.getString(R.string.reservation_error_deleting, e.message), 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } catch (e: Exception) {
                Log.e("ReservationsScreen", "Error deleting reservation: ${e.message}", e)
                Toast.makeText(
                    context, 
                    context.getString(R.string.reservation_error_deleting, e.message), 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            MediumFloatingActionButton(
                onClick = {
                    editingReservation = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.admin_reservations_add),
                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mostrar indicador de carga o lista de reservas
                if (isLoading) {
                    AdminLoading()
                } else if (reservations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.admin_reservations_empty),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Lista de reservas optimizada
                    ReservationsList(
                        reservations = reservations,
                        onEditReservation = { 
                            editingReservation = it 
                            showAddDialog = true
                        },
                        onDeleteReservation = deleteReservation
                    )
                }
            }
            
            // Diálogo para añadir/editar reserva
            if (showAddDialog) {
                OptimizedReservationDialog(
                    reservation = editingReservation,
                    availableTables = availableTables,
                    isLoadingTables = isLoadingTables,
                    onDismiss = { showAddDialog = false },
                    onSave = { reservation, isNew ->
                        saveReservation(reservation, isNew)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ReservationsList(
    reservations: List<Map<String, Any>>,
    onEditReservation: (Reservation) -> Unit,
    onDeleteReservation: (Reservation) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val reservationItems = remember(reservations) {
        reservations.mapNotNull { data ->
            try {
                val id = data["id"] as? String ?: ""
                val tableId = (data["tableId"] as? Number)?.toInt() ?: 0
                val fecha = data["fecha"] as? String ?: ""
                val hora = data["hora"] as? String ?: ""
                val nombreCliente = data["nombreCliente"] as? String ?: ""
                val numPersonas = (data["numPersonas"] as? Number)?.toInt() ?: 0
                val telefono = data["telefono"] as? String ?: ""
                val estado = data["estado"] as? String ?: "CONFIRMADA"
                
                Reservation(
                    id = id,
                    tableId = tableId,
                    fecha = fecha,
                    hora = hora,
                    nombreCliente = nombreCliente,
                    numPersonas = numPersonas,
                    telefono = telefono,
                    estado = estado
                )
            } catch (e: Exception) {
                Log.e("ReservationsScreen", "Error mapping reservation: ${e.message}", e)
                null
            }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        // Espacio inferior extra para que el FAB no tape la última tarjeta
        contentPadding = PaddingValues(bottom = 112.dp)
    ) {
        items(
            items = reservationItems,
            key = { it.id }
        ) { reservation ->
            OptimizedReservationCard(
                modifier = adminAnimateItem(),
                reservation = reservation,
                onEditReservation = onEditReservation,
                onDeleteReservation = onDeleteReservation
            )
        }
    }
}

@Composable
fun OptimizedReservationCard(
    reservation: Reservation,
    onEditReservation: (Reservation) -> Unit,
    onDeleteReservation: (Reservation) -> Unit,
    modifier: Modifier = Modifier
) {
    // Obtener el texto traducido basado en el estado interno
    val statusResourceId = ReservationConstants.getStatusResourceId(reservation.estado)
    val statusText = stringResource(statusResourceId)
    
    // Optimizar estado en la selección de colores
    val (statusColor, statusBgColor) = remember(reservation.estado) {
        when (ReservationConstants.fromLegacyStatus(reservation.estado)) {
            ReservationConstants.STATUS_CONFIRMED -> Pair(Color(0xFF4CAF50), Color(0xFFE8F5E9))
            ReservationConstants.STATUS_CANCELLED -> Pair(Color(0xFFF44336), Color(0xFFFFEBEE))
            ReservationConstants.STATUS_PENDING -> Pair(Color(0xFFFF9800), Color(0xFFFFF8E1))
            else -> Pair(Color(0xFF757575), Color(0xFFF5F5F5))
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Encabezado con información destacada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reservation.nombreCliente,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
                
                // Chip para el estado
                Surface(
                    color = statusBgColor,
                    shape = CircleShape,
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = statusColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Información principal en formato de cuadrícula
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Columna izquierda: Fecha, Hora, Personas
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoItem(
                        label = stringResource(R.string.reservation_label_date),
                        value = reservation.fecha
                    )
                    InfoItem(
                        label = stringResource(R.string.reservation_label_time),
                        value = reservation.hora
                    )
                    InfoItem(
                        label = stringResource(R.string.reservation_label_people),
                        value = "${reservation.numPersonas}"
                    )
                }
                
                // Columna derecha: Mesa, Teléfono
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoItem(
                        label = stringResource(R.string.reservation_label_table),
                        value = stringResource(R.string.reservation_table_id, reservation.tableId)
                    )
                    InfoItem(
                        label = stringResource(R.string.reservation_label_phone),
                        value = reservation.telefono
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botones de acción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Botón Editar
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onEditReservation(reservation) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.admin_reservations_edit),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_reservations_edit),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                }

                // Botón Eliminar
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = { onDeleteReservation(reservation) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.admin_reservations_delete),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.admin_reservations_delete),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedReservationDialog(
    reservation: Reservation?,
    availableTables: List<Mesa>,
    isLoadingTables: Boolean,
    onDismiss: () -> Unit,
    onSave: (Reservation, Boolean) -> Unit
) {
    val context = LocalContext.current
    val isNew = reservation == null
    
    // Strings de recursos para los títulos
    val dialogTitle = stringResource(
        if (isNew) R.string.admin_reservations_new 
        else R.string.admin_reservations_edit_title
    )
    
    // Obtener strings para estados desde recursos
    val confirmedStatusDisplay = stringResource(R.string.reservation_status_confirmed)
    val pendingStatusDisplay = stringResource(R.string.reservation_status_pending)
    val cancelledStatusDisplay = stringResource(R.string.reservation_status_cancelled)
    
    // Mapa de traducciones para mantener compatibilidad con backend (mostrar -> interno)
    val statusToInternal = mapOf(
        confirmedStatusDisplay to ReservationConstants.STATUS_CONFIRMED,
        pendingStatusDisplay to ReservationConstants.STATUS_PENDING,
        cancelledStatusDisplay to ReservationConstants.STATUS_CANCELLED
    )
    
    // Mapa inverso para mostrar correctamente el estado actual (interno -> mostrar)
    val internalToStatus = mapOf(
        ReservationConstants.STATUS_CONFIRMED to confirmedStatusDisplay,
        ReservationConstants.STATUS_PENDING to pendingStatusDisplay,
        ReservationConstants.STATUS_CANCELLED to cancelledStatusDisplay,
        // Para compatibilidad con valores antiguos
        "CONFIRMADA" to confirmedStatusDisplay,
        "PENDIENTE" to pendingStatusDisplay,
        "CANCELADA" to cancelledStatusDisplay
    )
    
    // Estados para los campos del formulario
    var nombreCliente by remember(reservation) { mutableStateOf(reservation?.nombreCliente ?: "") }
    var fecha by remember(reservation) { mutableStateOf(reservation?.fecha ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var hora by remember(reservation) { mutableStateOf(reservation?.hora ?: "13:00") }
    var numPersonas by remember(reservation) { mutableStateOf(reservation?.numPersonas?.toString() ?: "2") }
    var telefono by remember(reservation) { mutableStateOf(reservation?.telefono ?: "") }
    
    // Para el estado mostrado al usuario (traducido)
    var estadoMostrado by remember(reservation) { 
        mutableStateOf(internalToStatus[reservation?.estado] ?: confirmedStatusDisplay) 
    }
    
    // Para la selección de mesa
    var selectedTableId by remember(reservation) { mutableStateOf(reservation?.tableId ?: 0) }
    var showTableDropdown by remember { mutableStateOf(false) }
    
    // Lista combinada de mesas
    val allTables = remember(availableTables, reservation) {
        val tables = availableTables.toMutableList()
        if (reservation != null && !tables.any { it.id == reservation.tableId }) {
            tables.add(0, Mesa(id = reservation.tableId, estado = "RESERVADA", clientes = reservation.numPersonas, comandas = emptyList()))
        }
        tables
    }
    
    // Validación de campos
    val isValid = remember(nombreCliente, fecha, hora, numPersonas, telefono, selectedTableId) {
        nombreCliente.isNotBlank() && 
        fecha.isNotBlank() && 
        hora.isNotBlank() && 
        numPersonas.toIntOrNull() != null && 
        telefono.isNotBlank() && 
        selectedTableId > 0
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Título del diálogo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.admin_reservations_close),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Formulario con campos traducibles
                OutlinedTextField(
                    value = nombreCliente,
                    onValueChange = { nombreCliente = it },
                    label = { Text(stringResource(R.string.reservation_client_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Fecha y hora en la misma fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = fecha,
                        onValueChange = { fecha = it },
                        label = { Text(stringResource(R.string.reservation_date)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                    )
                    
                    OutlinedTextField(
                        value = hora,
                        onValueChange = { hora = it },
                        label = { Text(stringResource(R.string.reservation_time)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Número de personas y teléfono en la misma fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = numPersonas,
                        onValueChange = { numPersonas = it },
                        label = { Text(stringResource(R.string.reservation_people)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                    )
                    
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text(stringResource(R.string.reservation_phone)) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de mesa
                Box(modifier = Modifier.fillMaxWidth()) {
                    val tableText = if (selectedTableId > 0) 
                        stringResource(R.string.reservation_table_id, selectedTableId)
                    else 
                        stringResource(R.string.reservation_select_table)
                        
                    OutlinedTextField(
                        value = tableText,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.reservation_table)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        trailingIcon = {
                            IconButton(
                                onClick = { showTableDropdown = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.reservation_select_table),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showTableDropdown,
                        onDismissRequest = { showTableDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        if (isLoadingTables) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.reservation_loading_tables),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                                    ) 
                                },
                                onClick = { }
                            )
                        } else if (allTables.isEmpty()) {
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        stringResource(R.string.reservation_no_tables),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                                    ) 
                                },
                                onClick = { }
                            )
                        } else {
                            allTables.forEach { mesa ->
                                val displayState = TableStateUtils.getLocalizedTableState(context, mesa.estado)
                                
                                DropdownMenuItem(
                                    text = { 
                                        Text(stringResource(R.string.reservation_table_status, mesa.id, displayState))
                                    },
                                    onClick = { 
                                        selectedTableId = mesa.id
                                        showTableDropdown = false 
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selector de estado
                Box(modifier = Modifier.fillMaxWidth()) {
                    var showStatusDropdown by remember { mutableStateOf(false) }
                    
                    // Estado con colores
                    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                    val statusColor = remember(estadoMostrado, onSurfaceColor) {
                        when (estadoMostrado) {
                            confirmedStatusDisplay -> Color(0xFF4CAF50)
                            pendingStatusDisplay -> Color(0xFFFF9800)
                            cancelledStatusDisplay -> Color(0xFFF44336)
                            else -> onSurfaceColor
                        }
                    }
                    
                    OutlinedTextField(
                        value = estadoMostrado,
                        onValueChange = { },
                        label = { Text(stringResource(R.string.reservation_status)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp),
                        readOnly = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Medium
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { showStatusDropdown = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.reservation_status),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        listOf(confirmedStatusDisplay, pendingStatusDisplay, cancelledStatusDisplay).forEach { statusOption ->
                            val optionColor = when (statusOption) {
                                confirmedStatusDisplay -> Color(0xFF4CAF50)
                                pendingStatusDisplay -> Color(0xFFFF9800)
                                cancelledStatusDisplay -> Color(0xFFF44336)
                                else -> onSurfaceColor
                            }
                            
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        statusOption,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            color = optionColor,
                                            fontWeight = FontWeight.Medium
                                        )
                                    ) 
                                },
                                onClick = {
                                    estadoMostrado = statusOption
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        shapes = ButtonDefaults.shapes(),
                        onClick = onDismiss,
                        modifier = Modifier.heightIn(min = 54.dp)
                    ) {
                        Text(
                            stringResource(R.string.reservation_cancel),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        shapes = ButtonDefaults.shapes(),
                        onClick = {
                            // Convertir estado mostrado al valor interno
                            val estadoInterno = statusToInternal[estadoMostrado] ?: ReservationConstants.STATUS_CONFIRMED
                            
                            val newReservation = Reservation(
                                id = reservation?.id ?: UUID.randomUUID().toString(),
                                tableId = selectedTableId,
                                fecha = fecha,
                                hora = hora,
                                nombreCliente = nombreCliente,
                                numPersonas = numPersonas.toIntOrNull() ?: 0,
                                telefono = telefono,
                                estado = estadoInterno  // Usar el valor interno, no traducido
                            )
                            onSave(newReservation, isNew)
                        },
                        enabled = isValid,
                        modifier = Modifier.heightIn(min = 54.dp)
                    ) {
                        Text(
                            stringResource(R.string.reservation_save),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)
                        )
                    }
                }
            }
        }
    }
}