package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UsersViewModel(private val context: Context) : ViewModel() {
    private val firebaseManager = FirebaseManager()
    
    private val _users = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val users: StateFlow<List<Map<String, Any>>> = _users.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _userConfig = MutableStateFlow<Map<String, Any>>(emptyMap())
    val userConfig: StateFlow<Map<String, Any>> = _userConfig.asStateFlow()
    
    fun getUserConfig(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("UsersViewModel", "Obteniendo configuración del usuario: $userId")
                
                val result = firebaseManager.userManager.getUserConfig(userId)
                if (result.isSuccess) {
                    _userConfig.value = result.getOrDefault(emptyMap())
                } else {
                    _error.value = "Error obteniendo configuración: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error obteniendo configuración: ${e.message}", e)
                _error.value = "Error obteniendo configuración: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateUserConfig(userId: String, configData: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("UsersViewModel", "Updating user configuration: $userId")
                
                val result = firebaseManager.userManager.updateUserConfig(userId, configData)
                if (result.isSuccess) {
                    // Actualizar la lista de usuarios localmente para reflejar los cambios inmediatamente
                    refreshUserConfig(userId, configData)
                    onComplete(true)
                } else {
                    _error.value = context.getString(R.string.user_config_update_error)
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error updating configuration: ${e.message}", e)
                _error.value = context.getString(R.string.user_config_update_error)
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método nuevo para actualizar la configuración local de un usuario
    private fun refreshUserConfig(userId: String, configData: Map<String, Any>) {
        val currentUsers = _users.value.toMutableList()
        val userIndex = currentUsers.indexOfFirst { (it["id"] as? String) == userId }
        
        if (userIndex != -1) {
            val updatedUser = currentUsers[userIndex].toMutableMap()
            
            // Actualizar los campos de configuración
            configData.forEach { (key, value) ->
                updatedUser[key] = value
            }
            
            currentUsers[userIndex] = updatedUser
            _users.value = currentUsers
            
            Log.d("UsersViewModel", "Configuración de usuario actualizada localmente: $userId")
        }
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d("UsersViewModel", "Iniciando carga de usuarios...")
                
                // Configurar un listener para cambios en tiempo real
                firebaseManager.userManager.listenToUsers { usersList ->
                    Log.d("UsersViewModel", "Usuarios recibidos: ${usersList.size}")
                    
                    // Depurar cada usuario recibido
                    usersList.forEachIndexed { index, user ->
                        Log.d("UsersViewModel", "Usuario $index: ${user["email"]} - ${user["fullName"]} - ${user["role"]}")
                    }
                    
                    _users.value = usersList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error cargando usuarios: ${e.message}", e)
                _error.value = "Error cargando usuarios: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun createUser(email: String, password: String, fullName: String, role: String, onComplete: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Usar la función createUser corregida sin el parámetro 'name' extra
                val result = firebaseManager.userManager.createUser(email, password, fullName, role)
                
                if (result.isSuccess) {
                    onComplete(Result.success(true))
                } else {
                    _error.value = "Error creando usuario: ${result.exceptionOrNull()?.message}"
                    onComplete(Result.failure(result.exceptionOrNull() ?: Exception("Error desconocido")))
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error creando usuario: ${e.message}", e)
                _error.value = "Error creando usuario: ${e.message}"
                onComplete(Result.failure(e))
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateUser(userId: String, fullName: String, role: String, isActive: Boolean): Boolean {
        var success = false
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "fullName" to fullName,
                    "role" to role,
                    "isActive" to isActive
                )
                
                val result = firebaseManager.userManager.updateUser(userId, updates)
                success = result.isSuccess
                if (!result.isSuccess) {
                    _error.value = "Error actualizando usuario: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error actualizando usuario: ${e.message}", e)
                _error.value = "Error actualizando usuario: ${e.message}"
            }
        }
        return success
    }
    
    fun deleteUser(userId: String): Boolean {
        var success = false
        viewModelScope.launch {
            try {
                val result = firebaseManager.userManager.deleteUser(userId)
                success = result.isSuccess
                if (!result.isSuccess) {
                    _error.value = "Error eliminando usuario: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error eliminando usuario: ${e.message}", e)
                _error.value = "Error eliminando usuario: ${e.message}"
            }
        }
        return success
    }
    
    fun updateUserRole(userId: String, newRole: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val result = firebaseManager.userManager.updateUser(userId, mapOf("role" to newRole))
                if (result.isSuccess) {
                    onComplete(true)
                } else {
                    _error.value = "Error actualizando rol: ${result.exceptionOrNull()?.message}"
                    onComplete(false)
                }
            } catch (e: Exception) {
                Log.e("UsersViewModel", "Error actualizando rol: ${e.message}", e)
                _error.value = "Error actualizando rol: ${e.message}"
                onComplete(false)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Limpiar el listener cuando el ViewModel se destruye
        firebaseManager.userManager.removeListeners()
    }
}