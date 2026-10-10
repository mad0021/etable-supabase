package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.models.OrderItem
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.utils.OrderUtils
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseMenuManager
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReadyOrdersViewModel(
    private val orderManager: FirebaseOrderManager,
    private val context: Context
) : ViewModel() {
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // Nuevo mapa para almacenar los puntos de cocción de cada item
    private val _itemCookingPoints = MutableStateFlow<Map<String, CookingPoint?>>(emptyMap())
    val itemCookingPoints: StateFlow<Map<String, CookingPoint?>> = _itemCookingPoints.asStateFlow()
    
    // Nuevo mapa para almacenar las observaciones de cada item
    private val _itemObservations = MutableStateFlow<Map<String, String?>>(emptyMap())
    val itemObservations: StateFlow<Map<String, String?>> = _itemObservations.asStateFlow()
    
    // AÑADIR un StateFlow simple para los nombres de los platos
    private val _menuItems = MutableStateFlow<Map<String, String>>(emptyMap())
    val menuItems: StateFlow<Map<String, String>> = _menuItems.asStateFlow()
    
    private var ordersListener: ListenerRegistration? = null

    init {
        loadOrders()
    }
    
    // CAMBIAR este método para usar un approach más simple
    private fun loadMenuItems() {
        viewModelScope.launch {
            try {
                // Crear un mapa básico de nombres de platos
                // En lugar de cargar desde Firebase, usar los nombres que ya están en las órdenes
                Log.d("ReadyOrdersViewModel", "Carga de menú simplificada")
            } catch (e: Exception) {
                Log.e("ReadyOrdersViewModel", "Error: ${e.message}", e)
            }
        }
    }
    
    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            
            ordersListener?.remove()
            ordersListener = orderManager.listenOrdersByStatus(OrderStatus.READY) { ordersList ->
                // Filtrar los items de cada orden para mostrar solo los de la etapa actual
                val filteredOrders = ordersList.map { order ->
                    val currentStage = order.currentStage ?: "PRIMERS_BEGUDES"
                    val filteredItems = order.items.filter { item ->
                        OrderUtils.extractBaseStage(item) == currentStage
                    }
                    order.copy(items = filteredItems)
                }
                
                // Procesar puntos de cocción y observaciones
                val cookingPointsMap = mutableMapOf<String, CookingPoint?>()
                val observationsMap = mutableMapOf<String, String?>()
                
                ordersList.forEach { order ->
                    order.items.forEach { item ->
                        val itemKey = "${order.id}_${item.id}"
                        cookingPointsMap[itemKey] = OrderUtils.extractCookingPoint(item)
                        observationsMap[itemKey] = OrderUtils.extractObservations(item)
                    }
                }
                
                _orders.value = filteredOrders
                _itemCookingPoints.value = cookingPointsMap
                _itemObservations.value = observationsMap
                _isLoading.value = false
            }
        }
    }
    
    fun markOrderAsCompleted(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            orderManager.markOrderAsCompleted(orderId) { success ->
                if (success) {
                    _message.value = context.getString(R.string.admin_orders_completed_success)
                } else {
                    _message.value = context.getString(R.string.admin_orders_complete_error)
                }
                _isLoading.value = false
            }
        }
    }
    
    fun sendNextStage(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Obtener la orden actual
            val currentOrder = _orders.value.find { it.id == orderId }
            
            if (currentOrder != null) {
                // Usar constantes de recursos para las etapas
                val nextStage = when (currentOrder.currentStage) {
                    context.getString(R.string.stage_primers_begudes) -> context.getString(R.string.stage_segons)
                    context.getString(R.string.stage_segons) -> context.getString(R.string.stage_postres)
                    context.getString(R.string.stage_postres) -> {
                        // Si es la última etapa, marcar como completada
                        markOrderAsCompleted(orderId)
                        return@launch
                    }
                    else -> context.getString(R.string.stage_primers_begudes) // Por defecto
                }
                
                // Actualizar la etapa de la orden y cambiar su estado a PENDING para que vuelva al flujo
                orderManager.updateOrderStatus(orderId, OrderStatus.PENDING) { success ->
                    if (success) {
                        orderManager.updateOrderStage(orderId, nextStage) { stageSuccess ->
                            if (stageSuccess) {
                                _message.value = context.getString(R.string.admin_orders_stage_sent, nextStage)
                            } else {
                                _message.value = context.getString(R.string.admin_orders_stage_update_error)
                            }
                            _isLoading.value = false
                        }
                    } else {
                        _message.value = context.getString(R.string.admin_orders_status_update_error)
                        _isLoading.value = false
                    }
                }
            } else {
                _message.value = context.getString(R.string.admin_orders_not_found_for_stage)
                _isLoading.value = false
            }
        }
    }
    
    // Método auxiliar para obtener el nombre localizado de una etapa
    private fun getLocalizedStageName(stageKey: String): String {
        return when (stageKey) {
            context.getString(R.string.stage_primers_begudes) -> 
                context.getString(R.string.stage_primers_begudes_name)
            context.getString(R.string.stage_segons) -> 
                context.getString(R.string.stage_segons_name)
            context.getString(R.string.stage_postres) -> 
                context.getString(R.string.stage_postres_name)
            else -> stageKey
        }
    }
    
    // Método para obtener el punto de cocción de un item específico
    fun getCookingPointForItem(orderId: String, itemId: String): CookingPoint? {
        return _itemCookingPoints.value["${orderId}_${itemId}"]
    }
    
    // Método para obtener las observaciones de un item específico
    fun getObservationsForItem(orderId: String, itemId: String): String? {
        return _itemObservations.value["${orderId}_${itemId}"]
    }

    fun clearMessage() {
        _message.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
    }
}