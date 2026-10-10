package cat.dam.mamadou.etable.ui.screens.admin.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.utils.OrderUtils
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import cat.dam.mamadou.etable.supabase.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.firebase.FirebaseModelConverter
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import kotlin.math.abs
import cat.dam.mamadou.etable.utils.firebase.FirebaseMenuManager

// Clase para representar órdenes combinadas
data class CombinedOrder(
    val id: String,
    val tableName: String,
    val timestamp: Long,
    val orders: List<Order>
)

class HistoryOrdersViewModel(
    private val firebaseOrderManager: FirebaseOrderManager,
    private val context: Context
) : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    // Nuevo StateFlow para órdenes combinadas
    private val _combinedOrders = MutableStateFlow<List<CombinedOrder>>(emptyList())
    val combinedOrders: StateFlow<List<CombinedOrder>> = _combinedOrders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _itemCookingPoints = MutableStateFlow<Map<String, CookingPoint?>>(emptyMap())
    val itemCookingPoints: StateFlow<Map<String, CookingPoint?>> = _itemCookingPoints.asStateFlow()
    
    private val _itemObservations = MutableStateFlow<Map<String, String?>>(emptyMap())
    val itemObservations: StateFlow<Map<String, String?>> = _itemObservations.asStateFlow()
    
    // REMOVER la línea problemática del FirebaseMenuManager
    // AÑADIR un StateFlow simple
    private val _menuItems = MutableStateFlow<Map<String, String>>(emptyMap())
    val menuItems: StateFlow<Map<String, String>> = _menuItems.asStateFlow()
    
    private var ordersListener: ListenerRegistration? = null
    
    // Tiempo máximo para combinar órdenes (1h 15min en milisegundos)
    private val MAX_COMBINE_TIME_MS = 75 * 60 * 1000L
    
    init {
        loadOrders()
    }
    
    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            
            ordersListener?.remove()
            
            // Consultar directamente la colección history_orders
            ordersListener = firestore.collection("history_orders")
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("HistoryOrdersVM", "Error cargando historial: ${error.message}", error)
                        _isLoading.value = false
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val ordersList = snapshot.documents.mapNotNull { document ->
                            try {
                                val data = document.data
                                if (data != null) {
                                    FirebaseModelConverter.mapToOrder(data, document.id)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                Log.e("HistoryOrdersVM", "Error convirtiendo orden: ${e.message}", e)
                                null
                            }
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
                        
                        _orders.value = ordersList
                        _itemCookingPoints.value = cookingPointsMap
                        _itemObservations.value = observationsMap
                        
                        // Combinar órdenes de la misma mesa
                        combineOrdersByTable(ordersList)
                        
                        _isLoading.value = false
                    } else {
                        _orders.value = emptyList()
                        _combinedOrders.value = emptyList()
                        _isLoading.value = false
                    }
                }
        }
    }
    
    /**
     * Combina órdenes de la misma mesa si ocurrieron dentro del intervalo de tiempo definido
     */
    private fun combineOrdersByTable(orders: List<Order>) {
        // Agrupar órdenes por mesa
        val ordersByTable = orders.groupBy { it.tableName }
        val combinedOrdersList = mutableListOf<CombinedOrder>()
        
        // Para cada mesa, combinar órdenes cercanas en tiempo
        ordersByTable.forEach { (tableName, tableOrders) -> 
            // Ordenar por timestamp
            val sortedOrders = tableOrders.sortedBy { it.timestamp }
            val groups = mutableListOf<MutableList<Order>>()
            
            for (order in sortedOrders) {
                // Buscar un grupo existente donde la orden pueda ser incluida
                val compatibleGroup = groups.find { group ->
                    val earliestTime = group.minOf { it.timestamp.time }
                    val latestTime = group.maxOf { it.timestamp.time }
                    
                    // Verificar si el tiempo entre la orden actual y el grupo está dentro del rango
                    kotlin.math.abs(order.timestamp.time - earliestTime) <= MAX_COMBINE_TIME_MS &&
                    kotlin.math.abs(order.timestamp.time - latestTime) <= MAX_COMBINE_TIME_MS
                }
                
                if (compatibleGroup != null) {
                    // Añadir orden al grupo existente
                    compatibleGroup.add(order)
                } else {
                    // Crear un nuevo grupo
                    groups.add(mutableListOf(order))
                }
            }
            
            // Convertir cada grupo en una CombinedOrder
            groups.forEach { group ->
                val earliestTimestamp = group.minOf { it.timestamp.time } // Usar el timestamp más temprano como Long
                combinedOrdersList.add(
                    CombinedOrder(
                        id = group.joinToString("_") { it.id },
                        tableName = tableName,
                        timestamp = earliestTimestamp,
                        orders = group.sortedBy { it.timestamp.time }
                    )
                )
            }
        }
        
        // Ordenar las órdenes combinadas por timestamp (más reciente primero)
        _combinedOrders.value = combinedOrdersList.sortedByDescending { it.timestamp }
    }
    
    /**
     * Obtiene el nombre localizado de una etapa para mostrar
     */
    fun getLocalizedStageName(stageKey: String): String {
        return when {
            stageKey.contains("PRIMERS", ignoreCase = true) -> 
                context.getString(R.string.admin_history_stage_primers)
            stageKey.contains("SEGONS", ignoreCase = true) -> 
                context.getString(R.string.admin_history_stage_segons)
            stageKey.contains("POSTRES", ignoreCase = true) -> 
                context.getString(R.string.admin_history_stage_postres)
            else -> context.getString(R.string.admin_history_stage_other)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
    }
}