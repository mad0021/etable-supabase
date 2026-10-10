package cat.dam.mamadou.etable.ui.components

import cat.dam.mamadou.etable.ui.theme.TableStatusColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.TTSManager
import kotlinx.coroutines.delay

/**
 * Componente para mostrar una notificación emergente cuando una categoría del menú está lista
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuCategoryReadyPopupNotification(
    category: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }

    // TTS para notificación de categoría
    LaunchedEffect(category) {
        if (ttsManager.isEnabled()) {
            val categoryName = when (category) {
                "PRIMERS_BEGUDES" -> context.getString(R.string.menu_notification_starters_drinks)
                "SEGONS" -> context.getString(R.string.menu_notification_main_courses)
                "POSTRES" -> context.getString(R.string.menu_notification_desserts)
                else -> category
            }
            ttsManager.speakCategoryNotification(categoryName)
        }
    }

    val categoryInfo = when (category) {
        "PRIMERS_BEGUDES" -> Triple(
            stringResource(R.string.menu_notification_starters_drinks),
            TableStatusColors.Reserved, // Naranja
            Icons.Default.LocalDining
        )
        "SEGONS" -> Triple(
            stringResource(R.string.menu_notification_main_courses),
            TableStatusColors.Occupied, // Rojo
            Icons.Default.RestaurantMenu
        )
        "POSTRES" -> Triple(
            stringResource(R.string.menu_notification_desserts),
            TableStatusColors.Free, // Verde
            Icons.Default.LocalDining
        )
        else -> Triple(
            category,
            TableStatusColors.Selected, // Azul por defecto
            Icons.Default.Notifications
        )
    }

    // onDismiss se llama al terminar la animación de salida
    val visibleState = rememberNotificationState(onDismiss)

    // Auto-dismiss después de 5 segundos
    LaunchedEffect(Unit) {
        delay(5000)
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
                onClick = { visibleState.targetState = false },
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .background(categoryInfo.second, CircleShape)
                    ) {
                        Icon(
                            imageVector = categoryInfo.third,
                            contentDescription = stringResource(R.string.notif_category_ready_description),
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
                            text = stringResource(R.string.notif_dishes_ready_title, categoryInfo.first.uppercase()),
                            style = MaterialTheme.typography.headlineSmall,
                            color = categoryInfo.second
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.notif_dishes_ready_message),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { visibleState.targetState = false },
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = categoryInfo.second,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}
