package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.utils.OrderUtils
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PendingOrdersViewModel(
    private val orderManager: FirebaseOrderManager,
    private val context: Context
) : ViewModel() {
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Nuevo mapa para almacenar los puntos de cocción de cada item
    private val _itemCookingPoints = MutableStateFlow<Map<String, CookingPoint?>>(emptyMap())
    val itemCookingPoints: StateFlow<Map<String, CookingPoint?>> = _itemCookingPoints.asStateFlow()
    
    // Nuevo mapa para almacenar las observaciones de cada item
    private val _itemObservations = MutableStateFlow<Map<String, String?>>(emptyMap())
    val itemObservations: StateFlow<Map<String, String?>> = _itemObservations.asStateFlow()
    
    // AÑADIR un StateFlow simple
    private val _menuItems = MutableStateFlow<Map<String, String>>(emptyMap())
    val menuItems: StateFlow<Map<String, String>> = _menuItems.asStateFlow()
    
    private var ordersListener: ListenerRegistration? = null

    init {
        loadOrders()
    }
    
    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            
            ordersListener?.remove()
            ordersListener = orderManager.listenOrdersByStatus(OrderStatus.PENDING) { ordersList ->
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
    
    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            orderManager.updateOrderStatus(orderId, OrderStatus.IN_PROGRESS) { success ->
                if (success) {
                    _message.value = context.getString(R.string.admin_orders_accepted_success)
                } else {
                    _message.value = context.getString(R.string.admin_orders_accept_error)
                }
                _isLoading.value = false
            }
        }
    }
    
    fun rejectOrder(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            orderManager.updateOrderStatus(orderId, OrderStatus.REJECTED) { success ->
                if (success) {
                    _message.value = context.getString(R.string.admin_orders_rejected_success)
                } else {
                    _message.value = context.getString(R.string.admin_orders_reject_error)
                }
                _isLoading.value = false
            }
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