package cat.dam.mamadou.etable.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Gestor de notificaciones para platos listos
 */
class OrderReadyNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "order_ready_channel"
        private const val NOTIFICATION_GROUP = "order_ready_group"
        private const val TAG = "OrderReadyNotificationManager"
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Platos Listos"
            val descriptionText = "Notificaciones para platos listos para servir"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Canal de notificaciones creado")
        }
    }
    
    /**
     * Crea un PendingIntent para la notificación
     * 
     * @param order La orden con platos listos
     * @param stage La etapa que está lista
     * @param activityClass La clase de actividad que se abrirá al hacer clic en la notificación
     * @return El PendingIntent creado
     */
    private fun createPendingIntent(order: Order, stage: String, activityClass: Class<*>): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("order_id", order.id)
            putExtra("stage", stage)
            putExtra("table_id", order.tableId)
        }
        
        return PendingIntent.getActivity(
            context, 
            "${order.id}_$stage".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Muestra una notificación para un plato listo
     * 
     * @param order La orden con platos listos
     * @param stage La etapa que está lista (PRIMERS_BEGUDES, SEGONS, POSTRES)
     * @param activityClass La clase de actividad que se abrirá al hacer clic en la notificación
     */
    fun showOrderReadyNotification(order: Order, stage: String, activityClass: Class<*>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Texto para notificación más específico
                val stageName = when (stage) {
                    "PRIMERS_BEGUDES" -> "PRIMEROS y BEBIDAS"
                    "SEGONS" -> "SEGUNDOS"
                    "POSTRES" -> "POSTRES"
                    else -> stage
                }
                
                // Nombre de mesa a mostrar
                val tableName = order.tableName ?: "Mesa ${order.tableId}"
                
                // 1. NOTIFICACIÓN DEL SISTEMA ANDROID
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.raw.ic_notification_food)
                    .setContentTitle("¡$stageName LISTOS!")
                    .setContentText("$tableName: $stageName listos para servir")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("$tableName: Los $stageName están listos para servir.\nPulsa para ir a servir."))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibración más distintiva
                    .setLights(
                        when (stage) {
                            "PRIMERS_BEGUDES" -> 0xFFFFA726.toInt() // Naranja
                            "SEGONS" -> 0xFFEF5350.toInt() // Rojo
                            "POSTRES" -> 0xFF66BB6A.toInt() // Verde
                            else -> 0xFF2196F3.toInt() // Azul
                        }, 1000, 1000
                    )
                    .setColor(
                        when (stage) {
                            "PRIMERS_BEGUDES" -> 0xFFFFA726.toInt() // Naranja
                            "SEGONS" -> 0xFFEF5350.toInt() // Rojo
                            "POSTRES" -> 0xFF66BB6A.toInt() // Verde
                            else -> 0xFF2196F3.toInt() // Azul
                        }
                    )
                    .setContentIntent(createPendingIntent(order, stage, activityClass))
                    .setAutoCancel(true)
                
                // 2. NOTIFICACIÓN EN LA APLICACIÓN
                order.tableId?.let { tableId -> 
                    OrderReadyEventBus.notifyTableStageReady(tableId.toString(), stage)
                }

                
                // Mostrar notificación Android
                with(NotificationManagerCompat.from(context)) {
                    try {
                        notify("${order.id}_$stage".hashCode(), builder.build())
                        Log.d(TAG, "Notificación mostrada: $tableName, $stageName")
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Error de permisos: ${e.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error general: ${e.message}", e)
            }
        }
    }
}