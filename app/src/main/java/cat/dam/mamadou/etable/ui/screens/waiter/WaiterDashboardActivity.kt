package cat.dam.mamadou.etable.ui.screens.waiter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Build
import android.util.Log
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.components.TextSizeAdjuster
import cat.dam.mamadou.etable.ui.screens.auth.LoginActivity
import cat.dam.mamadou.etable.ui.screens.waiter.components.BottomNavigationBar
import cat.dam.mamadou.etable.ui.screens.waiter.screens.ConfigScreen
import cat.dam.mamadou.etable.ui.screens.waiter.screens.OrdersScreen
import cat.dam.mamadou.etable.ui.screens.waiter.screens.TablesScreen
import cat.dam.mamadou.etable.ui.theme.AppTheme
import cat.dam.mamadou.etable.ui.theme.ETableThemeWrapper
import cat.dam.mamadou.etable.ui.theme.ThemeManager
import cat.dam.mamadou.etable.utils.DimensionUtils
import cat.dam.mamadou.etable.utils.TextUtils
import cat.dam.mamadou.etable.utils.OrderReadyNotificationManager
import cat.dam.mamadou.etable.utils.useOrderReadyNotificationListener
import cat.dam.mamadou.etable.utils.firebase.FirebaseTableManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.NotificationHelper
import cat.dam.mamadou.etable.models.NotificationInfo
import cat.dam.mamadou.etable.ui.components.NotificationsPanel
import cat.dam.mamadou.etable.ui.components.PermissionDialog
import cat.dam.mamadou.etable.utils.LocaleManager
import cat.dam.mamadou.etable.utils.PermissionsManager
import cat.dam.mamadou.etable.utils.TTSManager
import cat.dam.mamadou.etable.utils.UserConfigManager
import cat.dam.mamadou.etable.supabase.DocumentChange
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import java.util.Locale

class WaiterDashboardActivity : ComponentActivity() {
    companion object {
        private const val TAG = "WaiterDashboardActivity"
        
        // Cambiar a un objeto compartido estático para toda la aplicación
        private const val NOTIFICATION_PREFERENCES = "ReadNotifications"
    }

    private val activeNotifications = mutableStateListOf<NotificationInfo>()
    private val activeCategoryNotifications = mutableStateListOf<String>()
    private val dismissedNotificationIds = mutableSetOf<String>()

    // Definir esta variable como propiedad de la clase
    private var isDarkTheme = false

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                NotificationHelper.ACTION_NEW_ORDER_NOTIFICATION -> {
                    val orderId = intent.getStringExtra(NotificationHelper.EXTRA_ORDER_ID) ?: return
                    val tableName = intent.getStringExtra(NotificationHelper.EXTRA_TABLE_NAME) ?: return
                    val stage = intent.getStringExtra(NotificationHelper.EXTRA_STAGE) ?: return
                    val notificationId = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_ID) ?: return
                    
                    Log.d(TAG, "Notificación recibida: Mesa $tableName, Etapa: $stage")
                    
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(500)
                    }
                    
                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(applicationContext, notification)
                    ringtone.play()
                    
                    updateNotificationsState()
                }
                
                NotificationHelper.ACTION_DISMISS_NOTIFICATION -> {
                    updateNotificationsState()
                }
            }
        }
    }

    private fun updateNotificationsState() {
        val notifications = NotificationHelper.getActiveNotifications(this)
        
        activeNotifications.clear()
        notifications.forEach { notification ->
            activeNotifications.add(NotificationInfo(
                id = notification.id ?: "",
                tableId = notification.tableId ?: "0", 
                stage = notification.stage ?: "",
                timestamp = notification.timestamp ?: System.currentTimeMillis()
            ))
        }
    }

    private fun updateNotificationsUI() {
        Log.d(TAG, "🔄 Actualizando UI de notificaciones, total: ${activeNotifications.size}")
        
        // Este enfoque fuerza una actualización de la UI de manera más efectiva
        runOnUiThread {
            // Crear una copia temporal de las notificaciones actuales
            val tempList = activeNotifications.toList()
            
            // Limpiar la lista actual
            activeNotifications.clear()
            
            // Usar Handler para garantizar que el clear y addAll no se ejecuten en el mismo frame
            Handler(Looper.getMainLooper()).post {
                // Volver a añadir los elementos
                activeNotifications.addAll(tempList)
                
                Log.d(TAG, "✅ UI de notificaciones actualizada, ahora mostrando: ${activeNotifications.size}")
            }
        }
    }

    private val firebaseManager = FirebaseManager()
    private var notificationsListener: ListenerRegistration? = null

    private fun setupOrderNotificationsListener() {
        notificationsListener?.remove()
        
        // Verificar si debemos bloquear la restauración de notificaciones
        val blockRestore = getSharedPreferences("NotificationState", Context.MODE_PRIVATE)
                        .getBoolean("blockNotificationRestore", false)
    
        if (blockRestore) {
            // Si estamos bloqueando la restauración, limpiar todo antes de configurar el listener
            activeNotifications.clear()
            
            // Restablecer la bandera
            getSharedPreferences("NotificationState", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("blockNotificationRestore", false)
                .apply()
        }
    
        Log.d(TAG, "🎯 Configurando listener de notificaciones. Bloqueo activo: $blockRestore")
    
        // Verificar notificaciones con el servidor antes de configurar el listener
        verifyNotificationsWithServer()
        
        // Crear un conjunto para rastrear las notificaciones activas existentes
        val activeNotificationIds = HashSet<String>()
        
        notificationsListener = firebaseManager.getFirestore()
            .collection("orderNotifications")
            .whereEqualTo("read", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando notificaciones: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    // No hay notificaciones activas, limpiar todo
                    if (activeNotifications.isNotEmpty()) {
                        Log.d(TAG, "🧹 No hay notificaciones activas en el servidor, limpiando locales")
                        activeNotifications.clear()
                        updateNotificationsUI()
                    }
                    return@addSnapshotListener
                }

                // Limpiar notificaciones que ya no están en Firestore
                val currentIds = snapshot.documents.map { it.id }.toSet()
                val toRemove = activeNotifications.filter { !currentIds.contains(it.id) }
                
                if (toRemove.isNotEmpty()) {
                    Log.d(TAG, "🧹 Eliminando ${toRemove.size} notificaciones que ya no existen en el servidor")
                    activeNotifications.removeAll(toRemove)
                    updateNotificationsUI()
                }
                
                // Procesar cambios y adiciones
                snapshot.documentChanges.forEach { change ->
                    val document = change.document
                    val notificationId = document.id
                    
                    // Verificar si esta notificación ya fue procesada localmente
                    if (dismissedNotificationIds.contains(notificationId)) {
                        Log.d(TAG, "🚫 Ignorando notificación ya procesada: $notificationId")
                        // Marcar como leída en Firestore si aún está marcada como no leída
                        document.reference.update("read", true)
                        return@forEach
                    }
                    
                    when (change.type) {
                        DocumentChange.Type.ADDED -> {
                            val data = document.data ?: return@forEach
                            
                            // Verificar si ya está marcada como leída en Firestore
                            val isRead = data["read"] as? Boolean ?: false
                            if (isRead) {
                                dismissedNotificationIds.add(notificationId)
                                saveDismissedNotification(notificationId)
                                return@forEach
                            }
                            
                            // Verificar si ya tenemos esta notificación en memoria
                            val existingIndex = activeNotifications.indexOfFirst { it.id == notificationId }
                            if (existingIndex >= 0) {
                                // Ya existe, no añadir duplicado
                                return@forEach
                            }
                            
                            val tableId = when (val tableIdValue = document.get("tableId")) {
                                is String -> tableIdValue
                                is Number -> tableIdValue.toString()
                                else -> ""
                            }
                            val tableName = data["tableName"] as? String ?: "Mesa $tableId"
                            val stage = data["stage"] as? String ?: return@forEach
                            
                            val notification = NotificationInfo(
                                id = notificationId,
                                tableId = tableName,
                                stage = stage,
                                timestamp = when (val timestampValue = data["timestamp"]) {
                                    is cat.dam.mamadou.etable.supabase.Timestamp -> timestampValue.toDate().time
                                    is Long -> timestampValue
                                    is Number -> timestampValue.toLong()
                                    else -> System.currentTimeMillis()
                                }
                            )
                            
                            // Registrar que esta notificación está activa
                            activeNotificationIds.add(notificationId)
                            
                            runOnUiThread {
                                if (!isNotificationDuplicate(notification)) {
                                    Log.d(TAG, "🔔 Nueva notificación recibida: $tableName, etapa: $stage")
                                    activeNotifications.add(notification)
                                    updateNotificationsUI()
                                }
                            }
                        }
                        
                        DocumentChange.Type.MODIFIED -> {
                            val data = document.data ?: return@forEach
                            
                            // Si fue marcada como leída en otro dispositivo
                            val isRead = data["read"] as? Boolean ?: false
                            if (isRead) {
                                // Marcarla como procesada localmente
                                dismissedNotificationIds.add(notificationId)
                                saveDismissedNotification(notificationId)
                                
                                // Removerla de las notificaciones activas
                                activeNotifications.removeAll { it.id == notificationId }
                                updateNotificationsUI()
                            }
                        }
                        
                        DocumentChange.Type.REMOVED -> {
                            // Si fue eliminada del servidor, removerla localmente
                            activeNotifications.removeAll { it.id == notificationId }
                            // Marcarla como procesada para no volver a mostrarla
                            dismissedNotificationIds.add(notificationId)
                            saveDismissedNotification(notificationId)
                            updateNotificationsUI()
                        }
                    }
                }
            }
    }

    private fun isNotificationAlreadyRead(notificationId: String): Boolean {
        // Debug: Registrar el ID que estamos verificando
        Log.d(TAG, "🔍 Verificando si la notificación $notificationId ya fue leída")
        
        // Verificar primero en memoria para mayor rapidez
        if (dismissedNotificationIds.contains(notificationId)) {
            Log.d(TAG, "🔍 Notificación $notificationId descartada en memoria")
            return true
        }
        
        // Si no está en memoria, verificar en SharedPreferences
        val prefs = getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
        val isMarkedAsRead = prefs.getBoolean(notificationId, false)
        
        // Si está en SharedPreferences, añadir a memoria para futuras consultas
        if (isMarkedAsRead) {
            Log.d(TAG, "🔍 Notificación $notificationId descartada en SharedPreferences")
            dismissedNotificationIds.add(notificationId)
        } else {
            Log.d(TAG, "✅ Notificación $notificationId NO leída anteriormente")
        }
        
        return isMarkedAsRead
    }

    private fun dismissNotification(id: String) {
        Log.d(TAG, "🗑️ Marcando notificación como leída: $id")
        
        // Añadir al conjunto en memoria
        dismissedNotificationIds.add(id)
        
        // Guardar en SharedPreferences para persistencia
        saveDismissedNotification(id)
        
        // Eliminar de la lista activa
        activeNotifications.removeAll { it.id == id }
        updateNotificationsUI()
        
        // Marcar como leído en Firebase
        firebaseManager.getFirestore().collection("orderNotifications")
            .document(id)
            .update("read", true)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Notificación $id marcada como leída en Firebase")
                NotificationHelper.dismissNotification(this, id)
            }
            .addOnFailureListener { e ->
                // En caso de error, mantener la notificación como procesada localmente
                Log.e(TAG, "❌ Error al marcar notificación como leída: ${e.message}")
            }
    }

    private fun saveDismissedNotification(notificationId: String) {
        val prefs = getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(notificationId, true).apply()
    }

    private fun loadDismissedNotificationsFromPreferences() {
        val prefs = getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE)
        val allPrefs = prefs.all
        
        Log.d(TAG, "🔄 Cargando notificaciones leídas desde preferencias: ${allPrefs.size} notificaciones")
        
        for (key in allPrefs.keys) {
            if (prefs.getBoolean(key, false)) {
                dismissedNotificationIds.add(key)
            }
        }
        
        Log.d(TAG, "✅ Cargadas ${dismissedNotificationIds.size} notificaciones previamente leídas")
    }

    private fun isNotificationDuplicate(notification: NotificationInfo): Boolean {
        // Verificar si ya está en la lista de notificaciones descartadas
        if (dismissedNotificationIds.contains(notification.id)) {
            Log.d(TAG, "🚫 Notificación ${notification.id} encontrada en lista de descartadas")
            return true
        }
        
        // Verificar si ya existe en las notificaciones activas
        return activeNotifications.any { existingNotification ->
            (existingNotification.id == notification.id && existingNotification.stage == notification.stage) ||
            // También considerar como duplicados si tienen el mismo tableId y stage (múltiples pedidos de la misma mesa/fase)
            (existingNotification.tableId == notification.tableId && existingNotification.stage == notification.stage)
        }
    }

    private fun cleanupDuplicateNotifications() {
        // Crear un mapa para detectar duplicados de manera eficiente
        val uniqueNotifications = mutableMapOf<String, NotificationInfo>()
        
        activeNotifications.forEach { notification ->
            val key = "${notification.id}_${notification.stage}"
            // Solo guardar la más reciente
            if (!uniqueNotifications.containsKey(key) || 
                notification.timestamp > (uniqueNotifications[key]?.timestamp ?: 0)) {
                uniqueNotifications[key] = notification
            }
        }
        
        if (uniqueNotifications.size < activeNotifications.size) {
            Log.d(TAG, "🧹 Eliminando ${activeNotifications.size - uniqueNotifications.size} notificaciones duplicadas")
            activeNotifications.clear()
            activeNotifications.addAll(uniqueNotifications.values)
            updateNotificationsUI()
        }
        
        // También eliminar las notificaciones ya procesadas
        activeNotifications.removeAll { notification -> 
            dismissedNotificationIds.contains(notification.id)
        }
    }

    private fun depurarNotificaciones() {
        Log.d(TAG, "📱 DEPURACIÓN DE NOTIFICACIONES 📱")
        Log.d(TAG, "Total notificaciones activas: ${activeNotifications.size}")
        Log.d(TAG, "Total notificaciones descartadas: ${dismissedNotificationIds.size}")
        
        // Listar todas las notificaciones activas
        activeNotifications.forEachIndexed { index, notification ->
            Log.d(TAG, "$index: ID=${notification.id}, Mesa=${notification.tableId}, Etapa=${notification.stage}")
        }
        
        // Verificar si alguna notificación activa está marcada como descartada
        val problematicas = activeNotifications.filter { 
            dismissedNotificationIds.contains(it.id) || isNotificationAlreadyRead(it.id) 
        }
        
        Log.d(TAG, "Notificaciones activas marcadas como leídas: ${problematicas.size}")
        
        // Limpieza condicional
        if (problematicas.isNotEmpty() && activeNotifications.size == problematicas.size) {
            Log.d(TAG, "⚠️ TODAS las notificaciones están marcadas como leídas. Realizando limpieza...")
            
            // Opción para una situación extrema - reset de las notificaciones descartadas
            // dismissedNotificationIds.clear()
            // getSharedPreferences(NOTIFICATION_PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply()
            // Log.d(TAG, "✅ Limpieza de notificaciones descartadas completada")
        }
    }

    private fun verifyNotificationsWithServer() {
        // Mostrar un indicador de carga si es necesario
        
        firebaseManager.getFirestore()
            .collection("orderNotifications")
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "🔍 Verificación con servidor completada")
                
                // Limpiar notificaciones actuales, no queremos nada que provenga de caché
                activeNotifications.clear()
                
                if (snapshot == null || snapshot.isEmpty) {
                    Log.d(TAG, "✅ No hay notificaciones activas en el servidor")
                    updateNotificationsUI()
                    return@addOnSuccessListener
                }
                
                // Procesar solo las notificaciones que existen en el servidor y no han sido descartadas
                val validNotifications = mutableListOf<NotificationInfo>()
                
                snapshot.documents.forEach { doc ->
                    val notificationId = doc.id
                    val isRead = doc.getBoolean("read") ?: false
                    
                    // Solo procesar notificaciones no leídas y que no estén en la lista de descartadas
                    if (!isRead && !dismissedNotificationIds.contains(notificationId)) {
                        // Extraer datos
                        val tableId = when (val tableIdValue = doc.get("tableId")) {
                            is String -> tableIdValue
                            is Number -> tableIdValue.toString()
                            else -> "0"
                        }
                        val stage = doc.getString("stage") ?: ""
                        val timestamp = when (val timestampValue = doc.get("timestamp")) {
                            is Long -> timestampValue
                            is Number -> timestampValue.toLong()
                            is cat.dam.mamadou.etable.supabase.Timestamp -> timestampValue.toDate().time
                            else -> System.currentTimeMillis()
                        }
                        
                        // Crear objeto de notificación
                        val notification = NotificationInfo(
                            id = notificationId,
                            tableId = tableId,
                            stage = stage,
                            timestamp = timestamp
                        )
                        
                        validNotifications.add(notification)
                    } else if (isRead) {
                        // Si está marcada como leída en el servidor pero no localmente, 
                        // agregarla a la lista de descartadas
                        dismissedNotificationIds.add(notificationId)
                        saveDismissedNotification(notificationId)
                    }
                }
                
                // Actualizar la lista de notificaciones activas solo con las validadas por el servidor
                activeNotifications.addAll(validNotifications)
                
                Log.d(TAG, "✅ Notificaciones actualizadas desde servidor: ${validNotifications.size} notificaciones válidas")
                
                updateNotificationsUI()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error verificando notificaciones con el servidor: ${e.message}")
            }
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleManager.getPersistedLocale(newBase)
        val context = LocaleManager.updateResources(newBase, locale)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar el tema al crear la actividad
        isDarkTheme = ThemeManager.isDarkMode(this)
        
        val intentFilter = IntentFilter().apply {
            addAction(NotificationHelper.ACTION_NEW_ORDER_NOTIFICATION)
            addAction(NotificationHelper.ACTION_DISMISS_NOTIFICATION)
        }
        
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(notificationReceiver, intentFilter)
        
        Log.d("WaiterDashboard", "Broadcast receiver registrado para notificaciones")
        
        updateNotificationsState()
        cleanupDuplicateNotifications()
        
        // Limpiar notificaciones huérfanas al inicio
        cleanOrphanNotifications()
        
        // IMPORTANTE: No cargar notificaciones de ninguna caché local al inicio
        // Simplemente inicializar la lista vacía
        activeNotifications.clear()
        
        // Cargar solo notificaciones descartadas, no las activas
        loadDismissedNotificationsFromPreferences()
        
        // Primero verificar con el servidor antes de mostrar cualquier notificación
        verifyNotificationsWithServer()
        
        Log.d("WaiterDashboard", "🌈 Iniciando WaiterDashboardActivity")

        val firestore = FirebaseFirestore.getInstance()
        val firebaseTableManager = FirebaseTableManager(firestore)

        setContent {
            // Añadir estas variables dentro del @Composable
            val context = LocalContext.current
            
            // Estado para controlar diálogo de permisos
            var showNotificationPermissionDialog by remember { mutableStateOf(false) }
            
            // Recordar si el permiso ha sido rechazado permanentemente
            var isPermissionPermanentlyDeclined by remember { 
                mutableStateOf(false)
            }
            
            // Launcher para solicitar permiso de notificaciones
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    // Permiso concedido, inicializar notificaciones
                    setupOrderNotificationsListener()
                } else {
                    // Verificar si ha sido denegado permanentemente
                    isPermissionPermanentlyDeclined = !PermissionsManager.shouldShowRequestPermissionRationale(
                        this@WaiterDashboardActivity,
                        PermissionsManager.PermissionType.NOTIFICATION
                    )
                    if (!isPermissionPermanentlyDeclined) {
                        // Si no es permanente, podemos mostrar el diálogo de nuevo más tarde
                        showNotificationPermissionDialog = false
                    } else {
                        // Si es permanente, mantener diálogo con opción para ir a configuración
                        showNotificationPermissionDialog = true
                    }
                }
            }
            
            // Verificar y solicitar permiso de notificaciones cuando se inicia la actividad
            LaunchedEffect(Unit) {
                // PEDIR PERMISO INMEDIATAMENTE al entrar
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!PermissionsManager.isPermissionGranted(context, PermissionsManager.PermissionType.NOTIFICATION)) {
                        Log.d(TAG, "🔔 Solicitando permiso de notificaciones al iniciar WaiterDashboard")
                        
                        if (PermissionsManager.shouldShowRequestPermissionRationale(
                            this@WaiterDashboardActivity,
                            PermissionsManager.PermissionType.NOTIFICATION
                        )) {
                            showNotificationPermissionDialog = true
                        } else {
                            PermissionsManager.requestPermission(
                                PermissionsManager.PermissionType.NOTIFICATION,
                                notificationPermissionLauncher
                            )
                        }
                    } else {
                        // Si ya tiene permiso, configurar listener inmediatamente
                        setupOrderNotificationsListener()
                    }
                } else {
                    // En versiones anteriores a Android 13, configurar listener directamente
                    setupOrderNotificationsListener()
                }
            }
            
            // Diálogo para solicitar permiso de notificaciones si es necesario
            if (showNotificationPermissionDialog) {
                PermissionDialog(
                    permissionTextProvider = PermissionsManager.getRationaleMessage(
                        context,
                        PermissionsManager.PermissionType.NOTIFICATION
                    ),
                    isPermanentlyDeclined = isPermissionPermanentlyDeclined,
                    onDismiss = { showNotificationPermissionDialog = false },
                    onOkClick = {
                        showNotificationPermissionDialog = false
                        PermissionsManager.requestPermission(
                            PermissionsManager.PermissionType.NOTIFICATION,
                            notificationPermissionLauncher
                        )
                    },
                    onGoToAppSettingsClick = {
                        showNotificationPermissionDialog = false
                        PermissionsManager.openAppSettings(context)
                    }
                )
            }
            
            ETableThemeWrapper {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val notifications = remember { activeNotifications }
                    val categoryNotifications = remember { activeCategoryNotifications }
                    val coroutineScope = rememberCoroutineScope()

                    val orderReadyNotificationManager = remember { OrderReadyNotificationManager(this) }
                    Log.d("WaiterDashboard", "Flujo de notificación inicializado")

                    val (lastOrder, lastStage, hasNewOrderReady) = useOrderReadyNotificationListener { order, stage ->
                        Log.d("WaiterDashboard", "Notificación recibida para orden: ${order.id}, etapa: $stage")

                        orderReadyNotificationManager.showOrderReadyNotification(
                            order = order,
                            stage = stage,
                            activityClass = WaiterDashboardActivity::class.java
                        )
                    }
                    
                    DisposableEffect(Unit) {
                        onDispose {
                            notificationsListener?.remove()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        WaiterDashboardScreen(
                            onThemeChanged = { isDark -> 
                                // Ahora sí podemos asignar a la variable de clase
                                isDarkTheme = isDark
                                val theme = if (isDark) AppTheme.DARK else AppTheme.LIGHT
                                ThemeManager.applyTheme(context, theme, isDark)
                                
                                // IMPORTANTE: Agregar una bandera para bloquear la restauración de notificaciones
                                val editor = getSharedPreferences("NotificationState", Context.MODE_PRIVATE).edit()
                                editor.putBoolean("blockNotificationRestore", true)
                                editor.apply()
                                
                                // Limpiar notificaciones inmediatamente
                                activeNotifications.clear()
                                updateNotificationsUI()
                            }
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                                .fillMaxWidth()
                        ) {
                            // Primer cambio: Verificar si hay notificaciones que deberían mostrarse
                            val notificationsToShow = activeNotifications.filter { notification ->
                                val shouldShow = !dismissedNotificationIds.contains(notification.id) && 
                                                !isNotificationAlreadyRead(notification.id)
                                Log.d(TAG, "🔎 Evaluando notificación ${notification.id} (${notification.tableId}, ${notification.stage}): mostrar=$shouldShow")
                                shouldShow
                            }
                            
                            // Mostrar conteo de notificaciones filtradas vs originales
                            Log.d(TAG, "📊 Notificaciones: ${notificationsToShow.size}/${activeNotifications.size} pasaron el filtro")

                            // Segundo cambio: No usar remember para las notificaciones filtradas
                            // ya que puede estar causando que el estado no se actualice correctamente
                            val currentTime = System.currentTimeMillis()
                            
                            // Tercer cambio: Usar key con más factores para forzar recomposición
                            key(
                                activeNotifications.size,
                                notificationsToShow.size, 
                                currentTime / 3000, // Forzar actualización cada 3 segundos como máximo
                                // Incluir IDs de notificaciones para garantizar actualización cuando cambian
                                notificationsToShow.map { it.id }.joinToString()
                            ) {
                                Log.d(TAG, "💡 Mostrando ${notificationsToShow.size} notificaciones en el panel")
                                
                                // Si hay notificaciones, mostrar el panel
                                if (notificationsToShow.isNotEmpty()) {
                                    NotificationsPanel(
                                        notifications = notificationsToShow,
                                        onDismiss = { id -> dismissNotification(id) },
                                        onServe = { id, tableId, stage ->
                                            saveActiveOrder(tableId, stage)
                                            dismissNotification(id)
                                            
                                            Toast.makeText(
                                                this@WaiterDashboardActivity, 
                                                getString(R.string.order_in_process, tableId), 
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                } else {
                                    // Si no hay notificaciones, mostrar mensaje claro en los logs
                                    Log.d(TAG, "❌ No hay notificaciones para mostrar en el panel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        
        Log.d(TAG, "🔄 onResume: total notificaciones descartadas: ${dismissedNotificationIds.size}")
        
        // No cargar notificaciones locales - primero limpiar y consultar al servidor
        activeNotifications.clear()
        updateNotificationsUI() // Actualizar UI inmediatamente para mostrar 0 notificaciones
        
        // Verificar con el servidor qué notificaciones realmente existen
        verifyNotificationsWithServer()
        
        depurarNotificaciones()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        UserConfigManager.stopListening()

        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(notificationReceiver)

        val firestore = FirebaseFirestore.getInstance()
        val tableManager = FirebaseTableManager(firestore)

        tableManager.getTables { result -> 
            result.onSuccess { tables -> 
                tables.forEach { table -> 
                    val tableId = (table["id"] as? Number)?.toInt() ?: return@forEach

                    val stages = listOf("PRIMERS_BEGUDES", "SEGONS", "POSTRES")
                    stages.forEach { stage -> 
                    }
                }
            }.onFailure { e -> 
                e.printStackTrace()
            }
        }

        notificationsListener?.remove()

        // Limpiar TTS
        TTSManager.getInstance(this).shutdown()
    }

    private fun saveActiveOrder(tableId: String, stage: String) {
        val sharedPrefs = getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
        
        val mesaNumero = extractTableNumber(tableId)
        
        val nextStage = when (stage) {
            "PRIMERS_BEGUDES" -> "SEGONS"
            "SEGONS" -> "POSTRES"
            "POSTRES" -> "POSTRES" // No avanzar automáticamente a FINALIZADA
            else -> "PRIMERS_BEGUDES"
        }
        
        Log.d(TAG, "📝 Guardando comanda activa: Mesa $tableId, etapa actual: $nextStage")
        
        sharedPrefs.edit().apply {
            putString("table_$mesaNumero", nextStage)
            apply()
        }
        
        // Solo actualizar la mesa cuando se finaliza la comanda (desde OrdersScreen)
    }

    private fun extractTableNumber(tableId: String): String {
        return try {
            val regex = "Mesa (\\d+)".toRegex()
            val matchResult = regex.find(tableId)
            matchResult?.groupValues?.get(1) ?: tableId
        } catch (e: Exception) {
            tableId
        }
    }

    private fun updateTableAfterCompletion(tableId: String) {
        val mesaNumero = extractTableNumber(tableId)
        
        firebaseManager.tableManager.getTableById(mesaNumero.toIntOrNull() ?: return) { tableData ->
            if (tableData != null) {
                val isReserved = tableData["estado"] == "RESERVADA" || tableData["status"] == "RESERVADA"
                val currentClientes = when (val clientesValue = tableData["clientes"]) {
                    is Long -> clientesValue.toInt()
                    is Int -> clientesValue
                    is Number -> clientesValue.toInt()
                    else -> 0
                }
                
                Log.d(TAG, "🍽️ Actualizando mesa después de FINALIZADA: Mesa $mesaNumero, Reservada: $isReserved")
                
                // Si está reservada, mantener como RESERVADA, si no, cambiar a LLIURE
                if (isReserved) {
                    firebaseManager.tableManager.updateTableStatus(
                        tableId = mesaNumero.toIntOrNull() ?: return@getTableById,
                        estado = "RESERVADA",
                        clientes = 0  // Resetear clientes pero mantener como reservada
                    ) { success -> 
                        Log.d(TAG, "✅ Mesa $mesaNumero actualizada a RESERVADA: $success")
                    }
                } else {
                    firebaseManager.tableManager.updateTableStatus(
                        tableId = mesaNumero.toIntOrNull() ?: return@getTableById,
                        estado = "LLIURE",
                        clientes = 0
                    ) { success -> 
                        Log.d(TAG, "✅ Mesa $mesaNumero liberada: $success")
                    }
                }
            }
        }
    }

    private fun liberarMesaSiNoEstaReservada(tableId: Int) {
        firebaseManager.tableManager.getTableById(tableId) { tableData ->
            if (tableData != null) {
                val isReserved = tableData["estado"] == "RESERVADA" || tableData["status"] == "RESERVADA"
                
                if (isReserved) {
                    // Si está reservada, mantenerla como RESERVADA pero sin clientes
                    firebaseManager.tableManager.updateTableStatus(
                        tableId = tableId,
                        estado = "RESERVADA",
                        clientes = 0
                    ) { success -> 
                        Log.d(TAG, "✅ Mesa $tableId mantenida como RESERVADA: $success")
                    }
                } else {
                    // Si no está reservada, liberar la mesa
                    firebaseManager.tableManager.updateTableStatus(
                        tableId = tableId,
                        estado = "LLIURE",
                        clientes = 0
                    ) { success -> 
                        Log.d(TAG, "✅ Mesa $tableId liberada: $success")
                    }
                }
            }
        }
    }

    private fun updateTableAfterPostres(tableId: String) {
        val mesaNumero = extractTableNumber(tableId)
        
        // Verificar si hay etapa FINALIZADA en las preferencias
        val sharedPrefs = getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
        val currentStage = sharedPrefs.getString("table_$mesaNumero", null)
        
        // Solo liberar la mesa si está en etapa FINALIZADA
        if (currentStage == "FINALIZADA") {
            firebaseManager.tableManager.getTableById(mesaNumero.toIntOrNull() ?: return) { tableData ->
                if (tableData != null) {
                    val isReserved = tableData["estado"] == "RESERVADA" || tableData["status"] == "RESERVADA"
                    
                    // Si está reservada, mantener ese estado pero sin clientes
                    if (isReserved) {
                        firebaseManager.tableManager.updateTableStatus(
                            tableId = mesaNumero.toIntOrNull() ?: return@getTableById,
                            estado = "RESERVADA",
                            clientes = 0
                        ) { success -> 
                            Log.d(TAG, "✅ Mesa $mesaNumero actualizada como RESERVADA: $success")
                        }
                    } else {
                        // Si no está reservada, liberarla
                        firebaseManager.tableManager.updateTableStatus(
                            tableId = mesaNumero.toIntOrNull() ?: return@getTableById,
                            estado = "LLIURE",
                            clientes = 0
                        ) { success -> 
                            Log.d(TAG, "✅ Mesa $mesaNumero liberada: $success")
                        }
                    }
                    
                    // Limpiar la etapa guardada
                    sharedPrefs.edit().remove("table_$mesaNumero").apply()
                }
            }
        }
    }

    private fun handleOrderReadyNotification(id: String, tableId: String, stage: String) {
        // Si es una notificación de postres, actualizar la mesa directamente
        if (stage == "POSTRES") {
            val mesaNumero = extractTableNumber(tableId)
            
            firebaseManager.tableManager.getTableById(mesaNumero.toIntOrNull() ?: return) { tableData ->
                if (tableData != null) {
                    val isReserved = tableData["estado"] as? String == "RESERVADA" || 
                                    tableData["status"] as? String == "RESERVADA"
                    
                    if (isReserved) {
                        firebaseManager.tableManager.updateTableStatus(
                            tableId = mesaNumero.toIntOrNull() ?: return@getTableById,
                            estado = "RESERVADA",
                            clientes = 0
                        ) { success -> 
                            Log.d(TAG, "✅ Mesa $mesaNumero actualizada como RESERVADA: $success")
                        }
                    } else {
                        firebaseManager.tableManager.updateTableStatus(
                            tableId = mesaNumero.toIntOrNull() ?: return@getTableById,
                            estado = "LLIURE",
                            clientes = 0
                        ) { success -> 
                            Log.d(TAG, "✅ Mesa $mesaNumero liberada: $success")
                        }
                    }
                    
                    // Marcar la comanda como FINALIZADA en las preferencias
                    val sharedPrefs = getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("table_$mesaNumero", "FINALIZADA").apply()
                }
            }
        }
        
        // Descartar la notificación
        dismissNotification(id)
    }

    private fun cleanOrphanNotifications() {
        Log.d(TAG, applicationContext.getString(R.string.notifications_clean_up))
        
        // Primera verificación: notificaciones que ya no existen en el servidor
        verifyNotificationsWithServer()
        
        // Verificación adicional: notificaciones que ya no están asociadas a mesas válidas
        firebaseManager.tableManager.getTables { result ->
            result.onSuccess { tables ->
                // Crear un mapa de mesas válidas
                val validTables = mutableMapOf<Int, Boolean>()
                tables.forEach { table ->
                    val id = (table["id"] as? Number)?.toInt() ?: return@forEach
                    val estado = table["estado"] as? String ?: ""
                    val clientes = (table["clientes"] as? Number)?.toInt() ?: 0
                    
                    // Una mesa es válida si está ocupada o reservada con clientes
                    validTables[id] = (estado == "OCUPADA" || estado == "RESERVADA") && clientes > 0
                }
                
                // Verificar todas las notificaciones activas
                val notificationsToRemove = mutableListOf<NotificationInfo>()
                
                activeNotifications.forEach { notification ->
                    // Extraer ID de mesa del nombre (por ejemplo, "Mesa 3" -> 3)
                    val mesaId = extractTableNumber(notification.tableId).toIntOrNull() ?: return@forEach
                    
                    // Si la mesa no es válida o no existe, marcar para eliminar
                    if (!validTables.containsKey(mesaId) || validTables[mesaId] == false) {
                        notificationsToRemove.add(notification)
                        
                        // Marcar como leída en Firebase
                        firebaseManager.getFirestore().collection("orderNotifications")
                            .document(notification.id)
                            .update("read", true)
                            .addOnSuccessListener {
                                Log.d(TAG, "✅ Notificación huérfana marcada como leída: ${notification.id}")
                            }
                    }
                }
                
                // Eliminar las notificaciones marcadas
                if (notificationsToRemove.isNotEmpty()) {
                    Log.d(TAG, "🗑️ Eliminando ${notificationsToRemove.size} notificaciones huérfanas")
                    activeNotifications.removeAll(notificationsToRemove)
                    updateNotificationsUI()
                }
            }.onFailure { e ->
                Log.e(TAG, "❌ Error obteniendo mesas: ${e.message}")
            }
        }
    }

    @Composable
    fun WaiterDashboardScreen(
        onThemeChanged: (Boolean) -> Unit = {}
    ) {
        val context = LocalContext.current
        var textSize by remember { mutableStateOf(TextUtils.getSavedTextSize(context)) }

        var appLanguage by remember { mutableStateOf(Locale("ca", "ES")) }

        val onLogout = {
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        }

        val navController = rememberNavController()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "tables"

        val currentScreen = when {
            currentRoute.startsWith("orders") -> "orders"
            else -> currentRoute
        }

        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> 
                        if (currentScreen != screen) {
                            navController.navigate(screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "tables",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("tables") {
                    TablesScreen(navController = navController)
                }
                composable(
                    "orders/{mesaId}?etapa={etapa}",
                    arguments = listOf(
                        navArgument("mesaId") { type = NavType.IntType },
                        navArgument("etapa") { 
                            type = NavType.StringType 
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val mesaId = backStackEntry.arguments?.getInt("mesaId")
                    val etapa = backStackEntry.arguments?.getString("etapa")
                    OrdersScreen(
                        mesaId = mesaId,
                        etapaInicial = etapa,
                        navController = navController
                    )
                }
                composable("orders") {
                    OrdersScreen(mesaId = null, navController = navController)
                }
                composable("config") {
                    ConfigScreen(
                        navController = navController,
                        onTextSizeChanged = { newSize -> 
                            textSize = newSize
                            TextUtils.saveTextSize(context, newSize)
                        },
                        onThemeChanged = { isDark -> 
                            // MISMO COMPORTAMIENTO QUE AdminDashboardActivity
                            isDarkTheme = isDark
                            
                            // También actualizar en el ThemeManager para persistencia
                            val currentTheme = ThemeManager.getTheme(context)
                            ThemeManager.applyTheme(context, currentTheme, isDark)
                            
                            // IMPORTANTE: Agregar una bandera para bloquear la restauración de notificaciones
                            val editor = getSharedPreferences("NotificationState", Context.MODE_PRIVATE).edit()
                            editor.putBoolean("blockNotificationRestore", true)
                            editor.apply()
                        },
                        onLanguageChanged = { locale -> 
                            appLanguage = locale 
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }

    @Composable
    fun HorizontalLayout(context: Context, textSizeValue: Float) {
        var textSize = textSizeValue

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    DimensionUtils.adaptiveDimen(
                        phone = 16.dp,
                        smallTablet = 24.dp,
                        largeTablet = 32.dp
                    )
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.waiter_dashboard_title),
                    style = TextUtils.getAdaptiveTextStyle(
                        context,
                        androidx.compose.ui.text.font.FontWeight.Bold,
                        1.2f
                    ),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Text(
                    text = stringResource(R.string.waiter_dashboard_welcome),
                    style = TextUtils.getAdaptiveTextStyle(context),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                TextSizeAdjuster(
                    onTextSizeChanged = { newSize -> 
                        textSize = newSize
                        TextUtils.saveTextSize(context, newSize)
                    }
                )
            }
        }
    }

    @Composable
    fun VerticalLayout(context: Context, textSizeValue: Float) {
        var textSize = textSizeValue

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    DimensionUtils.adaptiveDimen(
                        phone = 16.dp,
                        smallTablet = 24.dp,
                        largeTablet = 32.dp
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.waiter_dashboard_title),
                style = TextUtils.getAdaptiveTextStyle(
                    context,
                    androidx.compose.ui.text.font.FontWeight.Bold,
                    1.2f
                ),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Text(
                text = stringResource(R.string.waiter_dashboard_welcome),
                style = TextUtils.getAdaptiveTextStyle(context),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TextSizeAdjuster(
                onTextSizeChanged = { newSize -> 
                    textSize = newSize
                    TextUtils.saveTextSize(context, newSize)
                }
            )
        }
    }
}