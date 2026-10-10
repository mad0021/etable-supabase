package cat.dam.mamadou.etable.utils

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import cat.dam.mamadou.etable.models.Order
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bus de eventos para notificaciones de órdenes listas
 */
object OrderReadyEventBus {
    private const val TAG = "OrderReadyEventBus"
    private val scope = CoroutineScope(Dispatchers.Default)
    
    // Flow para emitir eventos de órdenes listas
    private val _orderReadyFlow = MutableSharedFlow<Pair<Order, String>>()
    val orderReadyFlow: SharedFlow<Pair<Order, String>> = _orderReadyFlow
    
    // Lista de notificaciones activas
    private val _activeNotifications = mutableStateListOf<Triple<String, String, String>>() // (orderId, tableName, stage)
    val activeNotifications: List<Triple<String, String, String>> = _activeNotifications
    
    // Lista de listeners
    private val listeners = mutableListOf<() -> Unit>()
    
    // Control de debounce
    private var lastNotificationTime = 0L
    private const val DEBOUNCE_TIME = 500 // ms
    
    /**
     * Registra un listener para cambios
     */
    fun registerListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            Log.d(TAG, "Listener registrado. Total: ${listeners.size}")
        }
    }
    
    /**
     * Elimina un listener
     */
    fun unregisterListener(listener: () -> Unit) {
        listeners.remove(listener)
        Log.d(TAG, "Listener eliminado. Total: ${listeners.size}")
    }
    
    /**
     * Notifica a todos los listeners
     */
    fun notifyOrderReady(orderId: String, tableName: String, stage: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < DEBOUNCE_TIME) {
            return
        }
        
        lastNotificationTime = currentTime
        _activeNotifications.add(Triple(orderId, tableName, stage))
        Log.d(TAG, "Notificando a ${listeners.size} listeners: Mesa $tableName, Etapa: $stage")
        listeners.forEach { it() }
    }
    
    /**
     * Notifica a todos los listeners usando una Order
     */
    fun notifyOrderReady(order: Order, stage: String) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < DEBOUNCE_TIME) {
            return
        }
        
        lastNotificationTime = currentTime
        
        // Añadir a las notificaciones activas
        _activeNotifications.add(
            Triple(
                order.id,
                order.tableName ?: "Mesa ${order.tableId}", 
                stage
            )
        )
        
        // Emitir al Flow para los listeners reactivos
        scope.launch {
            _orderReadyFlow.emit(Pair(order, stage))
        }
        
        // Notificar a los listeners tradicionales
        Log.d(TAG, "Notificando orden lista: ${order.id}, Etapa: $stage")
        listeners.forEach { it() }
    }
    
    /**
     * Notifica cuando una orden es actualizada
     */
    fun notifyOrderUpdated(orderId: String) {
        Log.d(TAG, "Notificación de actualización para orden: $orderId")
    }
    
    /**
     * Notifica que una mesa tiene una etapa lista
     */
    fun notifyTableStageReady(tableId: String, stage: String) {
        Log.d(TAG, "Notificación de etapa lista: Mesa $tableId, Etapa: $stage")
    }
    
    /**
     * Descarta una notificación específica
     */
    fun dismissNotification(orderId: String) {
        _activeNotifications.removeIf { it.first == orderId }
        Log.d(TAG, "Notificación descartada: $orderId")
    }
}