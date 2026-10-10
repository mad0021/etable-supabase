package cat.dam.mamadou.etable.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.OrderStages
import cat.dam.mamadou.etable.models.NotificationInfo
import cat.dam.mamadou.etable.utils.DateTimeFormatter
import cat.dam.mamadou.etable.utils.TTSManager
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsPanel(
    notifications: List<NotificationInfo>,
    onDismiss: (String) -> Unit,
    onServe: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }
    
    // TTS para nuevas notificaciones - ACTUALIZADO para usar función específica
    LaunchedEffect(notifications.size) {
        if (notifications.isNotEmpty() && ttsManager.isEnabled()) {
            val latestNotification = notifications.first()
            val tableNumber = latestNotification.tableId.replace("Mesa ", "")
            
            // NUEVO: Usar la función específica para notificaciones
            ttsManager.speakOrderNotification(tableNumber, latestNotification.stage)
        }
    }

    // Filtrar notificaciones duplicadas antes de mostrarlas
    val uniqueNotifications = remember(notifications) {
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<NotificationInfo>()
        
        notifications.forEach { notification ->
            val key = "${notification.id}_${notification.stage}"
            if (!seen.contains(key)) {
                seen.add(key)
                unique.add(notification)
            }
        }
        
        // Ordenar por timestamp (más reciente primero)
        unique.sortedByDescending { it.timestamp }
    }

    // Añadir log para las notificaciones filtradas
    Log.d("NotificationsPanel", "🔍 Después de filtrar: ${uniqueNotifications.size} notificaciones únicas")
    
    // Obtener texto pluralizado para los pedidos
    val notificationsCountText = stringResource(
        id = R.string.notifications_count, 
        uniqueNotifications.size
    )
    
    val motion = MaterialTheme.motionScheme

    // Siempre mostrar el contenedor, con altura adaptativa
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (uniqueNotifications.isEmpty()) Color.Transparent 
                           else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.large
                )
                .padding(8.dp)
        ) {
            // Título del panel con icono de servicio
            if (uniqueNotifications.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RoomService,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = notificationsCountText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
            
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = uniqueNotifications,
                        key = { it.id + it.stage }
                    ) { notification ->
                        // Entrada/salida y recolocación con specs del motionScheme (solo alpha + translación)
                        Box(
                            modifier = Modifier.animateItem(
                                fadeInSpec = motion.defaultEffectsSpec(),
                                placementSpec = motion.defaultSpatialSpec(),
                                fadeOutSpec = motion.fastEffectsSpec()
                            )
                        ) {
                            MejoradoNotificationItem(
                                notification = notification,
                                onDismiss = { onDismiss(notification.id) },
                                onServe = { id, tableId, stage -> onServe(id, tableId, stage) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MejoradoNotificationItem(
    notification: NotificationInfo,
    onDismiss: () -> Unit,
    onServe: ((String, String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }
    
    // Añadir log para identificar la notificación que causa problemas
    Log.d("NotificationsPanel", "Procesando notificación: ID=${notification.id}, Etapa=${notification.stage}, Mesa=${notification.tableId}")
    
    // Obtener contexto para traducciones
    
    // Textos traducidos usando recursos de strings
    val readyToServeText = stringResource(id = R.string.notification_ready_to_serve)
    val preparedAtText = stringResource(id = R.string.notification_prepared_at)
    val serveButtonText = stringResource(id = R.string.notification_served_button)
    val dismissButtonText = stringResource(id = R.string.notification_dismiss_button)
    val tableContentDesc = stringResource(id = R.string.table_prefix, 0) // Pasar un número entero
    
    // Manejar adecuadamente etapas vacías o nulas
    val stageValue = notification.stage.takeIf { !it.isNullOrEmpty() } ?: "UNKNOWN"
    
    // Obtener los recursos de string para cada etapa
    val startersDrinksText = stringResource(id = R.string.stage_starters_drinks)
    val startersText = stringResource(id = R.string.stage_starters)
    val drinksText = stringResource(id = R.string.stage_drinks)
    val mainCoursesText = stringResource(id = R.string.stage_main_courses)
    val dessertsText = stringResource(id = R.string.stage_desserts)
    
    // Obtener la información de display de etapa de manera segura
    val (icon, stageColor, stageName) = getStageDisplayInfo(
        stageValue = stageValue,
        startersDrinksText = startersDrinksText,
        startersText = startersText,
        drinksText = drinksText,
        mainCoursesText = mainCoursesText,
        dessertsText = dessertsText,
        context = context
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Cabecera con número de mesa destacado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de mesa con la mesa destacada
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, stageColor, CircleShape)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableBar,
                            contentDescription = tableContentDesc,
                            tint = stageColor,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        // Extraer solo el número de la mesa
                        val mesaNumero = notification.tableId.replace(Regex("^Mesa\\s*"), "")
                        Text(
                            text = mesaNumero,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Información del pedido
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = readyToServeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = stageColor
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Etapa con icono específico
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = stageColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stageName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Mostrar tiempo con formato localizado
                    val timeText = try {
                        DateTimeFormatter.formatTime(context, notification.timestamp)
                    } catch (e: Exception) {
                        Log.e("NotificationsPanel", "Error formateando tiempo: ${e.message}")
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(notification.timestamp))
                    }
                    Text(
                        text = "$preparedAtText $timeText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botones de acción más grandes y claros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de servir - ACTUALIZADO
                if (onServe != null) {
                    Button(
                        onClick = { 
                            // TTS para botón servir - ACTUALIZADO para usar función específica
                            if (ttsManager.isEnabled()) {
                                ttsManager.speakButtonActionWithResources("served")
                            }
                            onServe(notification.id, notification.tableId, notification.stage)
                        },
                        modifier = Modifier.weight(1f),
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = serveButtonText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Botón de descartar - ACTUALIZADO
                OutlinedButton(
                    onClick = { 
                        // TTS para botón descartar - ACTUALIZADO para usar función específica
                        if (ttsManager.isEnabled()) {
                            ttsManager.speakButtonActionWithResources("dismiss")
                        }
                        onDismiss()
                    },
                    modifier = if (onServe != null) Modifier.weight(0.7f) else Modifier.weight(1f),
                    shapes = ButtonDefaults.shapes(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = dismissButtonText)
                }
            }
        }
    }
}

/**
 * Función auxiliar para obtener la información de visualización de etapa
 * Evitando el uso de try-catch con funciones composables
 */
private fun getStageDisplayInfo(
    stageValue: String,
    startersDrinksText: String,
    startersText: String,
    drinksText: String,
    mainCoursesText: String,
    dessertsText: String,
    context: Context
): Triple<ImageVector, Color, String> {
    return when(stageValue) {
        "PRIMERS_BEGUDES" -> Triple(
            Icons.Default.LocalBar,
            Color(0xFF4CAF50), // Verde
            startersDrinksText
        )
        "PRIMERS" -> Triple(
            Icons.Default.Restaurant,
            Color(0xFF2196F3), // Azul
            startersText
        )
        "BEGUDES" -> Triple(
            Icons.Default.LocalBar,
            Color(0xFF9C27B0), // Morado
            drinksText
        )
        "SEGONS" -> Triple(
            Icons.Default.Fastfood,
            Color(0xFFFF9800), // Naranja
            mainCoursesText
        )
        "POSTRES" -> Triple(
            Icons.Default.Cake,
            Color(0xFFF44336), // Rojo
            dessertsText
        )
        else -> {
            // Caso de fallback para etapas no reconocidas
            Log.w("NotificationsPanel", "⚠️ Etapa desconocida: $stageValue")
            
            Triple(
                Icons.Default.Restaurant,
                Color(0xFF757575), // Gris por defecto
                stageValue // Usar el valor original como fallback
            )
        }
    }
}

// Función auxiliar para obtener el nombre de la etapa traducido
private fun getStageNameForTTS(stageKey: String, context: Context): String {
    return when(stageKey) {
        "PRIMERS_BEGUDES" -> "Primeros y Bebidas"
        "PRIMERS" -> "Primeros"
        "BEGUDES" -> "Bebidas"
        "SEGONS" -> "Segundos"
        "POSTRES" -> "Postres"
        else -> stageKey
    }
}