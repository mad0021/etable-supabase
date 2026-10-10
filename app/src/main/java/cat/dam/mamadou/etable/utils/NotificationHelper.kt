package cat.dam.mamadou.etable.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

/**
 * Helper para gestionar notificaciones directas entre componentes de la app
 */
object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val PREFS_NAME = "OrderNotifications"
    private const val KEY_NOTIFICATIONS = "active_notifications"
    
    // Acciones para BroadcastReceiver
    const val ACTION_NEW_ORDER_NOTIFICATION = "cat.dam.mamadou.etable.NEW_ORDER_NOTIFICATION"
    const val ACTION_DISMISS_NOTIFICATION = "cat.dam.mamadou.etable.DISMISS_NOTIFICATION"
    
    // Extras para los intents
    const val EXTRA_ORDER_ID = "order_id"
    const val EXTRA_TABLE_NAME = "table_name"
    const val EXTRA_STAGE = "stage"
    const val EXTRA_NOTIFICATION_ID = "notification_id"
    
    // Estructura para almacenar notificaciones
    data class OrderNotification(
        val id: String = UUID.randomUUID().toString(),
        val orderId: String,
        val tableId: String? = null,
        val tableName: String,
        val stage: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Envía una notificación de orden lista
     */
    fun sendOrderReadyNotification(
        context: Context, 
        orderId: String, 
        tableName: String, 
        stage: String
    ) {
        Log.d(TAG, "Enviando notificación: Mesa $tableName, Etapa: $stage")
        
        // Extraer tableId del nombre de la mesa si es posible
        val tableId = tableName.replace("Mesa ", "").trim()
        
        // Crear notificación
        val notification = OrderNotification(
            orderId = orderId,
            tableId = tableId,
            tableName = tableName,
            stage = stage
        )
        
        // Guardar en SharedPreferences
        saveNotification(context, notification)
        
        // Enviar broadcast
        val intent = Intent(ACTION_NEW_ORDER_NOTIFICATION).apply {
            putExtra(EXTRA_ORDER_ID, orderId)
            putExtra(EXTRA_TABLE_NAME, tableName)
            putExtra(EXTRA_STAGE, stage)
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
        }
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
    
    /**
     * Descarta una notificación específica
     */
    fun dismissNotification(context: Context, notificationId: String) {
        Log.d(TAG, "Descartando notificación: $notificationId")
        
        // Eliminar de SharedPreferences
        removeNotification(context, notificationId)
        
        // Enviar broadcast de actualización
        val intent = Intent(ACTION_DISMISS_NOTIFICATION).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
    
    /**
     * Obtiene todas las notificaciones activas
     */
    fun getActiveNotifications(context: Context): List<OrderNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsJson = prefs.getString(KEY_NOTIFICATIONS, "[]")
        
        val type = object : TypeToken<List<OrderNotification>>() {}.type
        return try {
            Gson().fromJson(notificationsJson, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener notificaciones: ${e.message}")
            emptyList()
        }
    }
    
    // Métodos privados de ayuda
    
    private fun saveNotification(context: Context, notification: OrderNotification) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifications = getActiveNotifications(context).toMutableList()
        
        // Añadir la nueva notificación
        notifications.add(notification)
        
        // Guardar la lista actualizada
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putString(KEY_NOTIFICATIONS, Gson().toJson(notifications))
        editor.apply()
    }
    
    private fun removeNotification(context: Context, notificationId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifications = getActiveNotifications(context).toMutableList()
        
        // Eliminar la notificación
        notifications.removeIf { it.id == notificationId }
        
        // Guardar la lista actualizada
        val editor: SharedPreferences.Editor = prefs.edit()
        editor.putString(KEY_NOTIFICATIONS, Gson().toJson(notifications))
        editor.apply()
    }
}