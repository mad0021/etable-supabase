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
import java.util.Date

class ReservationsViewModel(private val context: Context) : ViewModel() {
    private val firebaseManager = FirebaseManager()
    private var reservationsListener: cat.dam.mamadou.etable.supabase.ListenerRegistration? = null
    
    private val _reservations = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val reservations: StateFlow<List<Map<String, Any>>> = _reservations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadReservations()
    }
    
    fun loadReservations() {
        try {
            _isLoading.value = true
            _error.value = null
            
            // Cancelar listener anterior si existe
            reservationsListener?.remove()
            
            // Configurar un nuevo listener
            reservationsListener = firebaseManager.reservationManager.listenToReservations { reservationsList ->
                _reservations.value = reservationsList
                _isLoading.value = false
            }
        } catch (e: Exception) {
            Log.e("ReservationsViewModel", "Error loading reservations: ${e.message}", e)
            _error.value = context.getString(R.string.reservation_error_loading_tables, e.message)
            _isLoading.value = false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Limpiar el listener cuando el ViewModel se destruye
        reservationsListener?.remove()
    }
}