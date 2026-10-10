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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gestor de notificaciones para categorías de menú listas
 */
class MenuCategoryReadyNotificationManager(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "menu_category_ready_channel"
        private const val NOTIFICATION_GROUP = "menu_category_ready_group"
        private const val TAG = "MenuCategoryReadyNotificationManager"
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Crea el canal de notificaciones (requerido para Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Categorías de Menú Listas"
            val descriptionText = "Notificaciones para categorías de menú listas para servir"
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
     * @param category La categoría que está lista
     * @param activityClass La clase de actividad que se abrirá al hacer clic en la notificación
     * @return El PendingIntent creado
     */
    private fun createPendingIntent(category: String, activityClass: Class<*>): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("category", category)
        }
        
        return PendingIntent.getActivity(
            context, 
            category.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
    
    /**
     * Muestra una notificación para una categoría lista
     * 
     * @param category La categoría que está lista (PRIMERS_BEGUDES, SEGONS, POSTRES)
     * @param message Mensaje adicional (opcional)
     * @param activityClass La clase de actividad que se abrirá al hacer clic en la notificación
     */
    fun showCategoryReadyNotification(category: String, message: String = "", activityClass: Class<*>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Texto para notificación más específico
                val categoryName = when (category) {
                    "PRIMERS_BEGUDES" -> "PRIMEROS y BEBIDAS"
                    "SEGONS" -> "SEGUNDOS"
                    "POSTRES" -> "POSTRES"
                    else -> category
                }
                
                // 1. NOTIFICACIÓN DEL SISTEMA ANDROID
                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.raw.ic_notification_food)
                    .setContentTitle("¡$categoryName LISTOS!")
                    .setContentText("Los $categoryName están listos para servir")
                    .setStyle(NotificationCompat.BigTextStyle()
                        .bigText("Los $categoryName están listos para servir.\n${if (message.isNotEmpty()) "Mensaje: $message\n" else ""}Pulsa para ver detalles."))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibración más distintiva
                    .setLights(
                        when (category) {
                            "PRIMERS_BEGUDES" -> 0xFFFFA726.toInt() // Naranja
                            "SEGONS" -> 0xFFEF5350.toInt() // Rojo
                            "POSTRES" -> 0xFF66BB6A.toInt() // Verde
                            else -> 0xFF2196F3.toInt() // Azul
                        }, 1000, 1000
                    )
                    .setColor(
                        when (category) {
                            "PRIMERS_BEGUDES" -> 0xFFFFA726.toInt() // Naranja
                            "SEGONS" -> 0xFFEF5350.toInt() // Rojo
                            "POSTRES" -> 0xFF66BB6A.toInt() // Verde
                            else -> 0xFF2196F3.toInt() // Azul
                        }
                    )
                    .setContentIntent(createPendingIntent(category, activityClass))
                    .setAutoCancel(true)
                
                // 2. NOTIFICACIÓN DE AUDIO

                // Mostrar notificación Android
                with(NotificationManagerCompat.from(context)) {
                    try {
                        notify(category.hashCode(), builder.build())
                        Log.d(TAG, "Notificación mostrada: $categoryName")
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