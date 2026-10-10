package cat.dam.mamadou.etable.utils

import android.content.Context
import android.util.Log
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton para gestionar la configuración de usuario en tiempo real
 */
object UserConfigManager {
    private const val TAG = "UserConfigManager"
    private var configListener: ListenerRegistration? = null
    private val firebaseManager = FirebaseManager()
    
    // StateFlow para notificar cambios en permisos de usuario
    private val _permissionsUpdated = MutableStateFlow(false)
    val permissionsUpdated: StateFlow<Boolean> = _permissionsUpdated.asStateFlow()
    
    // Últimos permisos recibidos
    private var _lastPermissions: Map<String, Any> = mapOf()
    val lastPermissions get() = _lastPermissions
    
    // Usado para evitar procesamiento redundante
    private var lastUpdateTimestamp = 0L
    private var processingUpdate = false
    
    /**
     * Inicia la escucha de cambios en la configuración del usuario
     */
    fun startListening(userId: String) {
        stopListening() // Asegurar que no hay listener previo
        
        Log.d(TAG, "🔄 Iniciando escucha de configuración para usuario $userId")
        
        configListener = firebaseManager.getFirestore()
            .collection("user")
            .document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando configuración: ${error.message}")
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    // Evitar procesamiento redundante si estamos en medio de una actualización
                    if (processingUpdate) {
                        Log.d(TAG, "⚠️ Ya hay una actualización en proceso, omitiendo")
                        return@addSnapshotListener
                    }
                    
                    // Control de actualización para evitar múltiples eventos en cascada
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTimestamp < 500) {
                        Log.d(TAG, "⏱️ Omitiendo actualización por tiempo mínimo no cumplido")
                        return@addSnapshotListener
                    }
                    
                    lastUpdateTimestamp = currentTime
                    processingUpdate = true
                    
                    try {
                        // Extraer permisos específicos
                        val permissions = mutableMapOf<String, Any>()
                        
                        // Imprimir datos para depuración
                        Log.d(TAG, "📄 Recibidos datos de usuario: ${snapshot.data}")
                        
                        // Intentar obtener del campo permissions primero
                        val permissionsField = snapshot.get("permissions") as? Map<*, *>
                        if (permissionsField != null) {
                            Log.d(TAG, "🔒 Campo permissions encontrado: $permissionsField")
                            permissionsField.forEach { (key, value) ->
                                if (key is String) {
                                    permissions[key] = value!!
                                    Log.d(TAG, "🔑 Permiso cargado de 'permissions': $key = $value")
                                }
                            }
                        }
                        
                        // También verificar campos directamente en la raíz del documento
                        val permissionKeys = listOf(
                            "canRequestBreak", "canChangeTheme", "canLogout",
                            "canChangeLanguage", "canUseTTS", "canChangeTextSize", 
                            "allowedThemes", "allowedLanguages" // NUEVO: añadir allowedLanguages
                        )
                        
                        permissionKeys.forEach { key ->
                            if (snapshot.contains(key)) {
                                val value = snapshot.get(key)!!
                                permissions[key] = value
                                Log.d(TAG, "🔑 Permiso cargado de raíz: $key = $value")
                            }
                        }
                        
                        if (permissions.isNotEmpty() && permissions != _lastPermissions) {
                            Log.d(TAG, "📱 Permisos actualizados recibidos: $permissions")
                            _lastPermissions = permissions
                            _permissionsUpdated.value = true
                        } else {
                            Log.d(TAG, "📱 Permisos sin cambios, omitiendo notificación")
                        }
                    } finally {
                        processingUpdate = false
                    }
                }
            }
    }
    
    /**
     * Detiene la escucha de cambios
     */
    fun stopListening() {
        configListener?.remove()
        configListener = null
        processingUpdate = false
    }
    
    /**
     * Reinicia el indicador de permisos actualizados
     */
    fun resetUpdatedFlag() {
        _permissionsUpdated.value = false
    }
}