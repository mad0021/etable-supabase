package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class MenuViewModel(private val context: Context) : ViewModel() {
    private val TAG = "MenuViewModel"
    
    // Estado para los elementos del menú
    private val _menuItems = MutableStateFlow<List<MenuItem>>(emptyList())
    val menuItems: StateFlow<List<MenuItem>> = _menuItems.asStateFlow()
    
    // Estado para indicar carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Estado para errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Listener para elementos del menú
    private var menuItemsListener: ListenerRegistration? = null
    
    // Cargar elementos del menú
    fun loadMenuItems(firebaseManager: FirebaseManager) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            // Cancelar listener anterior si existe
            menuItemsListener?.remove()
            
            // Configurar nuevo listener
            menuItemsListener = firebaseManager.menuManager.listenToMenuItems { items ->
                _menuItems.value = items
                _isLoading.value = false
            }
        }
    }
    
    fun toggleItemAvailability(firebaseManager: FirebaseManager, itemId: String, isAvailable: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Actualizar disponibilidad usando el MenuManager
                firebaseManager.menuManager.updateMenuItemAvailability(itemId, isAvailable) { success ->
                    if (success) {
                        onComplete(true)
                    } else {
                        _error.value = "Error al actualizar disponibilidad"
                        onComplete(false)
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar disponibilidad: ${e.message}", e)
                _error.value = "Error: ${e.message}"
                onComplete(false)
                _isLoading.value = false
            }
        }
    }
    
    fun addMenuItem(firebaseManager: FirebaseManager, menuItem: MenuItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                firebaseManager.menuManager.addMenuItem(menuItem) { success ->
                    if (success) {
                        onComplete(true)
                    } else {
                        _error.value = "Error al añadir el plato"
                        onComplete(false)
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al añadir plato: ${e.message}", e)
                _error.value = "Error: ${e.message}"
                onComplete(false)
                _isLoading.value = false
            }
        }
    }
    
    fun updateMenuItem(firebaseManager: FirebaseManager, menuItem: MenuItem, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                firebaseManager.menuManager.updateMenuItem(menuItem) { success ->
                    if (success) {
                        onComplete(true)
                    } else {
                        _error.value = "Error al actualizar el plato"
                        onComplete(false)
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar plato: ${e.message}", e)
                _error.value = "Error: ${e.message}"
                onComplete(false)
                _isLoading.value = false
            }
        }
    }
    
    fun deleteMenuItem(firebaseManager: FirebaseManager, menuItem: MenuItem, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                firebaseManager.menuManager.deleteMenuItem(menuItem.id) { success ->
                    if (success) {
                        loadMenuItems(firebaseManager)
                    } else {
                        _error.value = context.getString(R.string.menu_management_delete_error)
                    }
                    onComplete(success)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = context.getString(R.string.menu_management_delete_error)
                onComplete(false)
                _isLoading.value = false
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Limpiar listener al destruir ViewModel
        menuItemsListener?.remove()
    }
}