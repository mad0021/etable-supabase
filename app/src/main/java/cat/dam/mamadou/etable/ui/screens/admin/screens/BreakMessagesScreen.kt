// c:\Users\AA\AndroidStudioProjects\etable\app\src\main\java\cat\dam\mamadou\etable\ui\screens\admin\screens\BreakMessagesScreen.kt

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.admin.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MarkEmailRead
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.components.BreakRequestNotification
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.BreakRequestViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.BreakRequestViewModelFactory
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.TextUtils
import cat.dam.mamadou.etable.utils.useBreakNotificationListener
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.firebase.BreakRequest
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Modelo de datos para los mensajes de descanso
data class BreakMessage(
    val id: String,
    val sender: String,
    val content: String,
    val timestamp: Date,
    val viewed: Boolean = false,
    val status: String = "pending"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakMessagesScreen(navController: NavController) {
    val context = LocalContext.current
    
    // Agregar referencia a los colores del tema actual
    val colorScheme = MaterialTheme.colorScheme

    // Optimización: memoizar objetos para evitar recreaciones
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val firebaseManager = remember { FirebaseManager() }
    val breakRequestViewModel: BreakRequestViewModel = viewModel(
        factory = BreakRequestViewModelFactory(context)
    )
    
    // Estado para la lista de mensajes de descanso - optimizado
    var breakMessages by remember { mutableStateOf<List<BreakMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isMarkingAsRead by remember { mutableStateOf(false) }
    
    // Estados para UI - minimizar recomposiciones
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showNewMessageNotification by remember { mutableStateOf(false) }
    
    // Optimización: usar estado derivado para reducir recomposiciones
    val sortedMessages = remember(breakMessages) {
        breakMessages.sortedByDescending { it.timestamp }
    }
    
    // Optimización: estado de lista para mejor rendimiento en scroll
    val lazyListState = rememberLazyListState()
    
    // Usar el hook para las notificaciones de descanso de manera eficiente
    val showBreakNotification = useBreakNotificationListener(firebaseManager)
    
    // Efectos secundarios optimizados - minimizar listeners
    DisposableEffect(Unit) {
        var listener: ListenerRegistration? = null
        
        // Cargamos los mensajes existentes una sola vez
        firebaseManager.breakManager.getBreakRequests { requests -> 
            breakMessages = requests.map { request -> 
                BreakMessage(
                    id = request.id,
                    // Asegurar que siempre mostramos el userName como remitente
                    sender = if (request.userName.isNotBlank()) request.userName else context.getString(R.string.user_unknown),
                    content = when {
                        // Usar string traducible cuando no hay motivo
                        request.reason.isNotBlank() -> request.reason
                        else -> context.getString(R.string.break_request_default)
                    },
                    timestamp = request.timestamp,
                    viewed = request.viewed,
                    status = request.status
                )
            }
            
            isLoading = false
            
            // Configurar listener para actualizaciones en tiempo real
            listener = firebaseManager.breakManager.listenForBreakRequests { requests -> 
                val newMessages = requests.map { request -> 
                    BreakMessage(
                        id = request.id,
                        // Asegurar que siempre mostramos el userName como remitente
                        sender = if (request.userName.isNotBlank()) request.userName else context.getString(R.string.user_unknown),
                        content = when {
                            // Usar string traducible cuando no hay motivo
                            request.reason.isNotBlank() -> request.reason
                            else -> context.getString(R.string.break_request_default)
                        },
                        timestamp = request.timestamp,
                        viewed = request.viewed,
                        status = request.status
                    )
                }
                
                // Verificar si hay mensajes nuevos NO VISTOS de manera eficiente
                val unviewedNewRequests = requests.filter { !it.viewed && it.status == "pending" }
                if (unviewedNewRequests.isNotEmpty() && unviewedNewRequests.size > breakMessages.count { !it.viewed }) {
                    showNewMessageNotification = true
                }
                
                breakMessages = newMessages
            }
        }
        
        // Limpiar listener al desmontar el componente
        onDispose {
            listener?.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.break_messages_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                actions = {
                    // Botón optimizado para tablets - más grande y accesible
                    Box(
                        modifier = Modifier
                            .size(56.dp) // Mayor área táctil
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                isMarkingAsRead = true
                                breakRequestViewModel.markAllBreakRequestsAsViewed { success -> 
                                    isMarkingAsRead = false
                                    snackbarMessage = if (success) {
                                        context.getString(R.string.break_request_mark_read_success)
                                    } else {
                                        context.getString(R.string.break_request_mark_read_error)
                                    }
                                    showSnackbar = true
                                }
                            },
                            enabled = !isMarkingAsRead,
                            modifier = Modifier.size(48.dp) // Botón más grande para tablets
                        ) {
                            if (isMarkingAsRead) {
                                LoadingIndicator(modifier = Modifier.size(28.dp), color = LocalContentColor.current)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = stringResource(R.string.break_request_mark_read),
                                    modifier = Modifier.size(28.dp) // Icono más grande para tablets
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    // Usar colores del tema en lugar de hardcoded
                    containerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues -> 
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp) // Padding horizontal adaptado para tablets
        ) {
            when {
                isLoading -> {
                    // Indicador de carga optimizado para tablets
                    AdminLoading()
                }
                breakMessages.isEmpty() -> {
                    // Estado vacío optimizado para tablets
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.break_request_empty),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.Center
                                ),
                                color = Color.Gray
                            )
                        }
                    }
                }
                else -> {
                    // Lista optimizada para tablets
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp), // Mayor espaciado para tablets
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(
                            items = sortedMessages,
                            key = { it.id } // Usar key para optimizar recomposiciones
                        ) { message ->
                            OptimizedBreakMessageItem(
                                modifier = adminAnimateItem(),
                                message = message,
                                dateFormat = dateFormat
                            )
                        }
                    }
                }
            }
            
            // Notificación de nuevo mensaje: specs del motionScheme; con reducción de movimiento solo fundido
            val motion = MaterialTheme.motionScheme
            val reduceMotion = rememberAdminReduceMotion()
            AnimatedVisibility(
                visible = showNewMessageNotification,
                enter = if (reduceMotion) fadeIn(motion.defaultEffectsSpec()) else slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = motion.defaultSpatialSpec()
                ) + fadeIn(motion.defaultEffectsSpec()),
                exit = if (reduceMotion) fadeOut(motion.fastEffectsSpec()) else slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = motion.fastSpatialSpec()
                ) + fadeOut(motion.fastEffectsSpec()),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f) // Limitado al 80% del ancho para tablets
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.primary // Usar el color primario del tema
                    ),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = colorScheme.onPrimary, // Color que contrasta con primary
                            modifier = Modifier.size(28.dp) // Más grande para tablets
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.break_notification_received),
                            color = colorScheme.onPrimary, // Color que contrasta con primary
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp // Más grande para tablets
                            )
                        )
                    }
                }
                
                // Optimizado: ocultar tras 3 segundos en vez de 5 para ahorrar recursos
                LaunchedEffect(showNewMessageNotification) {
                    if (showNewMessageNotification) {
                        delay(3000)
                        showNewMessageNotification = false
                    }
                }
            }
            
            // Snackbar optimizado para tablets
            if (showSnackbar) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                ) {
                    Snackbar(
                        modifier = Modifier.width(560.dp), // Ancho fijo para tablets
                        shape = MaterialTheme.shapes.medium,
                        action = {
                            TextButton(
                                shapes = ButtonDefaults.shapes(),
                                onClick = { showSnackbar = false },
                                modifier = Modifier.heightIn(min = 48.dp) // Más alto para tablets
                            ) {
                                Text(
                                    "OK",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                            }
                        }
                    ) {
                        Text(
                            text = snackbarMessage,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp // Más grande para tablets
                            )
                        )
                    }
                }
                
                // Optimizado: ocultar tras 2 segundos en vez de 3 para ahorrar recursos
                LaunchedEffect(showSnackbar) {
                    if (showSnackbar) {
                        delay(2000)
                        showSnackbar = false
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizedBreakMessageItem(
    message: BreakMessage,
    dateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Memoizar el formato de fecha para evitar recálculos
    val formattedDate = remember(message.timestamp) {
        dateFormat.format(message.timestamp)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Cabecera destacada con el nombre del remitente
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nombre del remitente con mayor énfasis
                Text(
                    text = stringResource(R.string.break_request_from, message.sender),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = colorScheme.primary
                )

                Surface(
                    color = colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Contenido del mensaje
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp
                ),
                color = colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Indicador de estado - usar recursos de strings
            val statusText = when (message.status) {
                "approved" -> stringResource(R.string.break_request_status_approved)
                "rejected" -> stringResource(R.string.break_request_status_rejected)
                else -> stringResource(R.string.break_request_status_pending)
            }

            val statusColor = when (message.status) {
                "approved" -> MaterialTheme.colorScheme.primary
                "rejected" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.secondary
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}