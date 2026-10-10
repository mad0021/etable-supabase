package cat.dam.mamadou.etable.ui.components

import cat.dam.mamadou.etable.ui.theme.TableStatusColors
import cat.dam.mamadou.etable.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OrderReadyPopupNotification(
    tableId: Int,
    tableName: String?,
    stage: String,
    onDismiss: () -> Unit,
    onServe: () -> Unit
) {
    // onDismiss se llama al terminar la animación de salida
    val visibleState = rememberNotificationState(onDismiss)

    // Texto más descriptivo según la etapa
    val stageInfo = when (stage) {
        "PRIMERS_BEGUDES" -> Triple(stringResource(R.string.stage_primers_begudes).uppercase(), TableStatusColors.Reserved, Icons.Default.RestaurantMenu)
        "SEGONS" -> Triple(stringResource(R.string.stage_segons).uppercase(), TableStatusColors.Occupied, Icons.Default.LocalDining)
        "POSTRES" -> Triple(stringResource(R.string.stage_postres).uppercase(), TableStatusColors.Free, Icons.Default.Notifications)
        else -> Triple(stage, MaterialTheme.colorScheme.primary, Icons.Default.Notifications)
    }

    // Nombre de mesa a mostrar
    val displayName = tableName ?: stringResource(R.string.table_prefix, tableId)

    // Auto-ocultar después de 10 segundos en pantalla
    LaunchedEffect(Unit) {
        delay(10000)
        visibleState.targetState = false
    }

    Popup(
        alignment = Alignment.TopCenter,
        properties = PopupProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ),
        onDismissRequest = { visibleState.targetState = false }
    ) {
        ExpressiveNotificationVisibility(
            visibleState = visibleState,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(top = 24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Insignia con el color de la etapa (TableStatusColors: contraste >= 4.5:1 con blanco)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .background(stageInfo.second, CircleShape)
                    ) {
                        Icon(
                            imageVector = stageInfo.third,
                            contentDescription = stringResource(R.string.notif_dishes_icon_description),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.notif_dishes_ready_title, stageInfo.first),
                            style = MaterialTheme.typography.headlineSmall,
                            color = stageInfo.second
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.notif_dishes_ready_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Solo cierra la notificación, sin navegar (no se llama a onServe)
                    Button(
                        onClick = { visibleState.targetState = false },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = stageInfo.second,
                            contentColor = Color.White
                        ),
                        contentPadding = ButtonDefaults.MediumContentPadding,
                        modifier = Modifier.heightIn(min = ButtonDefaults.MediumContainerHeight)
                    ) {
                        Text(text = stringResource(R.string.admin_orders_accept))
                    }
                }
            }
        }
    }
}
