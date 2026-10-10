package cat.dam.mamadou.etable.ui.components

import cat.dam.mamadou.etable.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.dam.mamadou.etable.utils.NotificationHelper

// Mejorar la visualización de las notificaciones
@Composable
fun OrderReadyNotification(
    notification: NotificationHelper.OrderNotification,
    onDismiss: (String) -> Unit,
    onServe: (() -> Unit)? = null
) {
    // Solo entrada animada: el descarte lo gestiona quien la muestra
    val enterState = remember { MutableTransitionState(false).apply { targetState = true } }
    ExpressiveNotificationVisibility(visibleState = enterState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de notificación con estilo mejorado
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.tts_new_notification),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = CircleShape
                    )
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contenido de la notificación
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.order_ready_title),
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.notif_table_label, notification.tableName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = stringResource(
                        R.string.admin_orders_stage,
                        when (notification.stage) {
                            "PRIMERS_BEGUDES" -> stringResource(R.string.stage_primers_begudes)
                            "PRIMERS" -> stringResource(R.string.stage_starters)
                            "BEGUDES" -> stringResource(R.string.stage_drinks)
                            "SEGONS" -> stringResource(R.string.stage_segons)
                            "POSTRES" -> stringResource(R.string.stage_postres)
                            else -> notification.stage
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Acciones
            Column {
                // Botón de servir
                if (onServe != null) {
                    IconButton(
                        onClick = { onServe() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.tts_button_served),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Botón de descarte
                IconButton(
                    onClick = { onDismiss(notification.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.tts_button_dismiss),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
    }
}

// Versión simplificada para usar desde WaiterDashboardActivity
@Composable
fun OrderReadyNotification(
    orderId: String,
    tableName: String,
    stage: String,
    onDismiss: () -> Unit
) {
    // Crear una notificación desde los parámetros individuales
    val notification = NotificationHelper.OrderNotification(
        id = orderId,
        orderId = orderId,
        tableName = tableName,
        stage = stage
    )
    
    // Usar la implementación principal
    OrderReadyNotification(
        notification = notification,
        onDismiss = { onDismiss() }
    )
}