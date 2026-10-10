// Si este archivo no existe, crea el archivo con esta función

package cat.dam.mamadou.etable.utils

import androidx.compose.runtime.*
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager

@Composable
fun useBreakNotificationListener(firebaseManager: FirebaseManager): Boolean {
    var showNotification by remember { mutableStateOf(false) }
    
    DisposableEffect(Unit) {
        val listener = firebaseManager.breakManager.listenForBreakRequests { requests ->
            // Filtrar solo las solicitudes pendientes y no vistas
            val pendingUnviewedRequests = requests.filter { 
                !it.viewed && it.status == "pending" 
            }
            
            // Solo mostrar notificación si hay solicitudes pendientes no vistas
            showNotification = pendingUnviewedRequests.isNotEmpty()
        }
        
        onDispose {
            listener.remove()
        }
    }
    
    return showNotification
}