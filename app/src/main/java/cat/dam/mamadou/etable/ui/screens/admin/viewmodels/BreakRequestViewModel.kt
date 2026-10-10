package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.BreakRequest
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BreakRequestViewModel(private val context: Context) : ViewModel() {
    private val firebaseManager = FirebaseManager()
    
    // Estado para las solicitudes de descanso
    private val _breakRequests = MutableStateFlow<List<BreakRequest>>(emptyList())
    val breakRequests: StateFlow<List<BreakRequest>> = _breakRequests.asStateFlow()
    
    // Estado para indicar carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Estado para errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Cargar solicitudes al inicializar
        loadBreakRequests()
    }
    
    fun loadBreakRequests() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            firebaseManager.breakManager.getBreakRequests { requests ->
                // Convertir de utils.firebase.BreakRequest a models.BreakRequest
                val modelRequests = requests.map { request ->
                    // Crear una instancia de models.BreakRequest con los datos de firebase.BreakRequest
                    BreakRequest(
                        id = request.id,
                        userId = request.userId,
                        userName = request.userName.ifBlank { 
                            request.userId.takeIf { it.isNotBlank() } ?: context.getString(R.string.user_unknown)
                        },
                        timestamp = request.timestamp,
                        reason = request.reason.ifBlank { 
                            context.getString(R.string.break_request_default)
                        },
                        status = request.status,
                        isViewed = request.viewed ?: false,
                        fullName = request.fullName ?: ""
                    )
                }
                _breakRequests.value = modelRequests
                _isLoading.value = false
            }
        }
    }

    fun updateBreakRequestStatus(requestId: String, status: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                firebaseManager.breakManager.updateBreakRequestStatus(requestId, status) { success ->
                    if (success) {
                        loadBreakRequests()
                    } else {
                        _error.value = context.getString(R.string.break_request_update_error)
                    }
                    onComplete(success)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = context.getString(R.string.break_request_update_error)
                onComplete(false)
                _isLoading.value = false
            }
        }
    }

    fun markBreakRequestAsViewed(requestId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                firebaseManager.breakManager.markBreakRequestAsViewed(requestId) { success ->
                    if (success) {
                        // NUEVO: También marcar como procesada en el sistema de notificaciones
                        BreakNotificationManager.markNotificationAsProcessed(context, requestId)
                        loadBreakRequests()
                    } else {
                        _error.value = context.getString(R.string.break_request_mark_read_error)
                    }
                    onComplete(success)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.break_request_mark_read_error)
                onComplete(false)
                _isLoading.value = false
            }
        }
    }
    
    fun markAllBreakRequestsAsViewed(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Obtener todas las solicitudes pendientes no vistas
                val pendingRequests = _breakRequests.value.filter { !it.isViewed && it.status == "pending" }
                
                if (pendingRequests.isEmpty()) {
                    onComplete(true)
                    _isLoading.value = false
                    return@launch
                }
                
                // NUEVO: Marcar todas como procesadas inmediatamente
                pendingRequests.forEach { request ->
                    BreakNotificationManager.markNotificationAsProcessed(context, request.id)
                }
                
                // Contador para seguir el progreso
                var completedCount = 0
                var allSuccess = true
                
                // Actualizar inmediatamente el estado local para evitar renotificaciones
                val updatedList = _breakRequests.value.map { request ->
                    if (!request.isViewed && request.status == "pending") {
                        request.copy(isViewed = true)
                    } else {
                        request
                    }
                }
                _breakRequests.value = updatedList
                
                // Marcar cada solicitud como vista en Firebase
                pendingRequests.forEach { request ->
                    firebaseManager.breakManager.markBreakRequestAsViewed(request.id) { success ->
                        completedCount++
                        if (!success) allSuccess = false
                        
                        // Cuando todas las solicitudes han sido procesadas
                        if (completedCount == pendingRequests.size) {
                            if (allSuccess) {
                                // Recargar para asegurar sincronización con Firebase
                                loadBreakRequests()
                            } else {
                                _error.value = context.getString(R.string.break_request_mark_read_error)
                            }
                            onComplete(allSuccess)
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.break_request_mark_read_error)
                onComplete(false)
                _isLoading.value = false
            }
        }
    }

    fun getLatestBreakRequest(callback: (BreakRequest?) -> Unit) {
        viewModelScope.launch {
            firebaseManager.breakManager.getLatestBreakRequest { firebaseBreakRequest ->
                // Convertir de firebase.BreakRequest a models.BreakRequest
                val modelBreakRequest = firebaseBreakRequest?.let {
                    BreakRequest(
                        id = it.id,
                        userId = it.userId,
                        userName = it.userName.ifBlank { 
                            it.userId.takeIf { id -> id.isNotBlank() } ?: context.getString(R.string.user_unknown)
                        },
                        timestamp = it.timestamp,
                        reason = it.reason.ifBlank { 
                            context.getString(R.string.break_request_default)
                        },
                        status = it.status,
                        isViewed = it.viewed ?: false,
                        fullName = it.fullName ?: ""
                    )
                }
                callback(modelBreakRequest)
            }
        }
    }
}
