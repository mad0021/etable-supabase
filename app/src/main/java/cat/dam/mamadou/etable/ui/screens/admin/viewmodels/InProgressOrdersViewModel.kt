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
import cat.dam.mamadou.etable.utils.firebase.FirebaseModelConverter
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InProgressOrdersViewModel(
    private val firebaseOrderManager: FirebaseOrderManager, 
    private val context: Context
) : ViewModel() {
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Nuevo mapa para almacenar los puntos de cocción de cada item
    private val _itemCookingPoints = MutableStateFlow<Map<String, CookingPoint?>>(emptyMap())
    val itemCookingPoints: StateFlow<Map<String, CookingPoint?>> = _itemCookingPoints.asStateFlow()
    
    // Nuevo mapa para almacenar las observaciones de cada item
    private val _itemObservations = MutableStateFlow<Map<String, String?>>(emptyMap())
    val itemObservations: StateFlow<Map<String, String?>> = _itemObservations.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    private var ordersListener: ListenerRegistration? = null
    
    // AÑADIR un StateFlow simple
    private val _menuItems = MutableStateFlow<Map<String, String>>(emptyMap())
    val menuItems: StateFlow<Map<String, String>> = _menuItems.asStateFlow()
    
    init {
        loadInProgressOrders()
    }
    
    private fun loadOrders() {
        _isLoading.value = true
        ordersListener = firebaseOrderManager.listenOrdersByStatus(OrderStatus.IN_PROGRESS) { ordersList ->
            _orders.value = ordersList
            
            // Extraer puntos de cocción y observaciones de todas las órdenes
            val allCookingPoints = mutableMapOf<String, CookingPoint?>()
            val allObservations = mutableMapOf<String, String?>()
            
            ordersList.forEach { order ->
                // Reemplaza la línea con ambigüedad por esta versión menos ambigua
                val result = FirebaseModelConverter.extractCookingPointAndObservations(order)
                val extractedCookingPoints = result.first
                val extractedObservations = result.second

                // O alternativamente
                // val (extractedCookingPoints, extractedObservations) = extractCookingPointAndObservations(order) as Pair<Map<String, CookingPoint?>, Map<String, String?>>
                
                allCookingPoints.putAll(extractedCookingPoints)
                allObservations.putAll(extractedObservations)
            }
            
            _itemCookingPoints.value = allCookingPoints
            _itemObservations.value = allObservations
            _isLoading.value = false
        }
    }
    
    fun loadInProgressOrders() {
        _isLoading.value = true
        
        ordersListener?.remove()
        ordersListener = firebaseOrderManager.listenOrdersByStatus(OrderStatus.IN_PROGRESS) { ordersList ->
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
    
    // Método para obtener el punto de cocción de un item específico
    fun getCookingPointForItem(orderId: String, itemId: String): CookingPoint? {
        return _itemCookingPoints.value["${orderId}_${itemId}"]
    }
    
    // Método para obtener las observaciones de un item específico
    fun getObservationsForItem(orderId: String, itemId: String): String? {
        return _itemObservations.value["${orderId}_${itemId}"]
    }
    
    fun markOrderAsReady(orderId: String) {
        _isLoading.value = true
        
        firebaseOrderManager.updateOrderStatus(orderId, OrderStatus.READY) { success ->
            if (success) {
                _message.value = context.getString(R.string.admin_orders_marked_success)
            } else {
            }
            _isLoading.value = false
        }
    }
    
    fun clearMessage() {
        _message.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
    }
}