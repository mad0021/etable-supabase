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
import cat.dam.mamadou.etable.utils.firebase.BreakRequest // IMPORTANTE: Usar este tipo

object BreakNotificationManager {
    
    private const val TAG = "BreakNotificationManager"
    const val CHANNEL_ID = "break_requests_channel"
    
    // CAMBIAR: Usar el tipo correcto para currentBreakRequest
    private var currentBreakRequest: BreakRequest? = null
    
    // Control para notificaciones modales
    private var modalNotificationsEnabled = true
    
    // Constante para identificar las notificaciones de break request
    private const val BREAK_NOTIFICATION_TAG = "break_notification"
    
    // Sistema de persistencia para notificaciones procesadas
    private const val PROCESSED_NOTIFICATIONS_PREFS = "ProcessedBreakNotifications"
    private const val KEY_PROCESSED_IDS = "processed_ids"
    
    /**
     * PÚBLICO: Verifica si una notificación ya fue procesada
     */
    fun isNotificationProcessed(context: Context, requestId: String): Boolean {
        val prefs = context.getSharedPreferences(PROCESSED_NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
        val processedIds = prefs.getStringSet(KEY_PROCESSED_IDS, emptySet()) ?: emptySet()
        return processedIds.contains(requestId)
    }
    
    /**
     * PÚBLICO: Marca una notificación como procesada
     */
    fun markNotificationAsProcessed(context: Context, requestId: String) {
        val prefs = context.getSharedPreferences(PROCESSED_NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
        val processedIds = prefs.getStringSet(KEY_PROCESSED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        processedIds.add(requestId)
        prefs.edit().putStringSet(KEY_PROCESSED_IDS, processedIds).apply()
        Log.d(TAG, "✅ Notificación $requestId marcada como procesada")
    }

    /**
     * NUEVO: Método para limpiar una notificación específica de las procesadas
     */
    fun unmarkNotificationAsProcessed(context: Context, requestId: String) {
        val prefs = context.getSharedPreferences(PROCESSED_NOTIFICATIONS_PREFS, Context.MODE_PRIVATE)
        val processedIds = prefs.getStringSet(KEY_PROCESSED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        processedIds.remove(requestId)
        prefs.edit().putStringSet(KEY_PROCESSED_IDS, processedIds).apply()
        Log.d(TAG, "🔄 Notificación $requestId removida de procesadas")
    }

    /**
     * Habilita las notificaciones modales
     */
    fun enableModalNotifications() {
        modalNotificationsEnabled = true
        Log.d(TAG, "✅ Notificaciones modales habilitadas")
    }
    
    /**
     * Deshabilita las notificaciones modales
     */
    fun disableModalNotifications() {
        modalNotificationsEnabled = false
        Log.d(TAG, "🚫 Notificaciones modales deshabilitadas")
    }

    /**
     * NUEVO: Obtiene la solicitud de descanso actual
     */
    fun getCurrentBreakRequest(): BreakRequest? {
        return currentBreakRequest
    }

    /**
     * NUEVO: Descarta/limpia la notificación actual
     */
    fun dismissNotification() {
        currentBreakRequest = null
        Log.d(TAG, "🗑️ Notificación de descanso descartada")
    }

    /**
     * CORREGIDO: Muestra una notificación para una solicitud de descanso
     */
    fun showBreakNotification(context: Context, breakRequest: BreakRequest) {
        createNotificationChannel(context)
        
        // Verificar si ya fue procesada
        if (isNotificationProcessed(context, breakRequest.id)) {
            Log.d(TAG, "🚫 Notificación ${breakRequest.id} ya fue procesada, omitiendo")
            return
        }
        
        // CORREGIR: Verificar si las notificaciones modales están HABILITADAS
        if (!modalNotificationsEnabled) {
            Log.d(TAG, "🚫 Notificaciones modales desactivadas, omitiendo")
            return
        }
        
        // Si el breakRequest ya está siendo procesado, no mostrar notificación
        if (currentBreakRequest != null && currentBreakRequest?.id == breakRequest.id) {
            Log.d(TAG, "Ya existe una solicitud con el mismo ID: ${breakRequest.id}")
            return
        }
        
        // Almacenar la solicitud actual
        currentBreakRequest = breakRequest
        
        // Marcar inmediatamente como procesada para evitar renotificaciones
        markNotificationAsProcessed(context, breakRequest.id)
        
        // Crear intent para abrir la actividad correspondiente
        val intent = Intent(context, cat.dam.mamadou.etable.ui.screens.admin.AdminDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "break_requests")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // USAR strings traducibles para el contenido
        val notificationTitle = context.getString(R.string.break_notification_title)
        val notificationContent = when {
            breakRequest.userName.isNotBlank() -> context.getString(R.string.break_notification_from, breakRequest.userName)
            else -> context.getString(R.string.break_request_default)
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.notificacion)
            .setContentTitle(notificationTitle)
            .setContentText(notificationContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}"))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

    val notificationManager = NotificationManagerCompat.from(context)
    notificationManager.notify(breakRequest.id.hashCode(), notification)
    
    Log.d(TAG, "📱 Notificación de descanso mostrada para: ${breakRequest.userName}")
    }

    /**
     * Crea el canal de notificaciones necesario para Android 8.0+
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.break_requests_channel_name)
            val descriptionText = context.getString(R.string.break_requests_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            // CREAR URI PARA TU ARCHIVO .MP3
            val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}")
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                // USAR TU ARCHIVO .MP3 COMO SONIDO DEL CANAL
                setSound(soundUri, null)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "📢 Canal de notificaciones creado con notification_sound.mp3")
        }
    }
}