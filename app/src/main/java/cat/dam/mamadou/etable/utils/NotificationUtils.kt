package cat.dam.mamadou.etable.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cat.dam.mamadou.etable.utils.firebase.BreakRequestEventBus
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager

/**
 * Hook que escucha notificaciones de descanso pero no activa el modal central
 */
@Composable
fun useBreakNotificationListenerNoModal(firebaseManager: FirebaseManager): Boolean {
    var showBreakNotification by remember { mutableStateOf(false) }
    
    // Notificaciones modales desactivadas (una vez, no en cada recomposición)
    LaunchedEffect(Unit) {
        BreakNotificationManager.disableModalNotifications()
    }
    
    DisposableEffect(firebaseManager) {
        // Usar addListener en lugar de registerBreakRequestListener
        val listener = BreakRequestEventBus.addListener {
            // Solo establecer estado para la notificación de banner superior
            showBreakNotification = true
        }
        
        onDispose {
            // Usar removeListener en lugar de unregisterListener
            BreakRequestEventBus.removeListener(listener)
        }
    }
    
    return showBreakNotification // Retorna solo para el banner, no para modal
}