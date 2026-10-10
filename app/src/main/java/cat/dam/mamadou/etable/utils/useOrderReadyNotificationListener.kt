package cat.dam.mamadou.etable.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import kotlinx.coroutines.flow.collectLatest

/**
 * Hook personalizado para escuchar notificaciones de platos listos
 * 
 * @param onOrderReady Callback que se ejecuta cuando se recibe una notificación de plato listo
 * @return Triple con la última orden recibida, la etapa lista y un booleano que indica si hay una nueva notificación
 */
@Composable
fun useOrderReadyNotificationListener(
    onOrderReady: ((Order, String) -> Unit)? = null
): Triple<Order?, String?, Boolean> {
    var lastOrder by remember { mutableStateOf<Order?>(null) }
    var lastStage by remember { mutableStateOf<String?>(null) }
    var hasNewNotification by remember { mutableStateOf(false) }
    
    LaunchedEffect(key1 = true) {
        OrderReadyEventBus.orderReadyFlow.collectLatest { pair ->
            val order = pair.first
            val stage = pair.second
            lastOrder = order
            lastStage = stage
            hasNewNotification = true
            onOrderReady?.invoke(order, stage)
        }
    }
    
    return Triple(lastOrder, lastStage, hasNewNotification)
}