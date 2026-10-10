package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import android.content.Context
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.NotificationInfo
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.Query
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import cat.dam.mamadou.etable.supabase.DocumentChange
import cat.dam.mamadou.etable.supabase.DocumentSnapshot
import cat.dam.mamadou.etable.supabase.FieldPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.models.OrderItem
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.utils.OrderReadyEventBus
import java.util.Date
import java.util.UUID

/**
 * Manager class for order-related operations in Firestore
 */
class FirebaseOrderManager(private val firestore: FirebaseFirestore) {

    companion object {
        // Constante para el TAG de logging
        private const val TAG = "FirebaseOrderManager"
    }

    /**
     * Updates a table's orders or creates a new order
     * @param tableId ID of the table
     * @param tableName Name of the table
     * @param items Items in the order
     * @param stageTag Stage tag (PRIMERS_BEGUDES, SEGONS, POSTRES)
     * @param forceNewOrder Si es true, siempre crea una nueva orden en lugar de actualizar una existente
     * @param onComplete Callback with the result
     */
    fun updateTableOrders(
        tableId: Int,
        tableName: String,
        items: List<OrderItem>,
        stageTag: String,
        forceNewOrder: Boolean = false,
        onComplete: (Boolean) -> Unit
    ) {
        if (items.isEmpty()) {
            Log.w(TAG, "No hay elementos para actualizar")
            onComplete(false)
            return
        }
        
        Log.d(TAG, "Actualizando órdenes para mesa $tableId con ${items.size} elementos")
        
        if (forceNewOrder) {
            val total = items.sumOf { it.price * it.quantity }
            
            val order = hashMapOf(
                "id" to UUID.randomUUID().toString(),
                "tableId" to tableId,
                "tableName" to tableName,
                "status" to OrderStatus.PENDING.toString(),
                "timestamp" to cat.dam.mamadou.etable.supabase.Timestamp.now(),
                "currentStage" to stageTag,
                "completedStages" to listOf<String>(), // Lista vacía de etapas completadas
                "items" to items.map { item ->
                    mapOf(
                        "id" to item.id,
                        "name" to item.name,
                        "quantity" to item.quantity,
                        "price" to item.price,
                        "etapa" to item.etapa  // Asegúrate de que etapa contiene "_OBS:texto" si hay observaciones
                    )
                }
            )
    
            // Guardar también en SharedPreferences
            val context = firestore.app.applicationContext
            val sharedPrefs = context.getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("table_$tableId", stageTag).apply()
            Log.d(TAG, "📝 Guardada etapa $stageTag para mesa $tableId en preferencias locales")

            firestore.collection("orders")
                .add(order)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "Nueva orden creada con éxito: ${documentReference.id}")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al crear la orden", e)
                    onComplete(false)
                }
            return
        }
        
        firestore.collection("orders")
            .whereEqualTo("tableId", tableId)
            .whereEqualTo("status", OrderStatus.PENDING.toString())
            .get()
            .addOnSuccessListener { existingOrders -> 
                if (!existingOrders.isEmpty) {
                    val existingOrderDoc = existingOrders.documents[0]
                    Log.d(TAG, "Orden existente encontrada: ${existingOrderDoc.id}")
                    
                    val currentItems = existingOrderDoc.get("items") as? List<Map<String, Any>> ?: listOf()
                    
                    val filteredItems = currentItems.filter { item ->
                        val itemEtapa = item["etapa"] as? String
                        itemEtapa != stageTag
                    }
                    
                    val newItemsData = items.map { item ->
                        mapOf(
                            "id" to item.id,
                            "name" to item.name,
                            "quantity" to item.quantity,
                            "price" to item.price,
                            "etapa" to item.etapa  // Aquí está la clave, asegurarnos que etapa contiene puntos y observaciones
                        )
                    }
                    
                    val updatedItems = filteredItems + newItemsData
                    
                    val total = updatedItems.sumOf { item ->
                        val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                        val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                        price * quantity
                    }
                    
                    // Obtener etapas completadas actuales
                    val completedStages = existingOrderDoc.get("completedStages") as? List<String> ?: listOf()
                    
                    // Determinar etapa anterior para agregarla a completedStages si no está
                    val previousStage = when(stageTag) {
                        "SEGONS" -> "PRIMERS_BEGUDES"
                        "POSTRES" -> "SEGONS"
                        "FINALIZADA" -> "POSTRES"
                        else -> null
                    }
                    
                    // Crear lista actualizada de etapas completadas
                    val updatedCompletedStages = if (previousStage != null && !completedStages.contains(previousStage)) {
                        completedStages + previousStage
                    } else {
                        completedStages
                    }
                    
                    existingOrderDoc.reference.update(
                        "items", updatedItems,
                        "timestamp", cat.dam.mamadou.etable.supabase.Timestamp.now(),
                        "currentStage", stageTag,
                        "completedStages", updatedCompletedStages,
                        "total", total,
                        "tableId", tableId,
                        "tableName", tableName
                    )
                    .addOnSuccessListener {
                        Log.d(TAG, "Orden actualizada con éxito")
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al actualizar la orden", e)
                        onComplete(false)
                    }
                } else {
                    val total = items.sumOf { it.price * it.quantity }
                    
                    val order = hashMapOf(
                        "id" to UUID.randomUUID().toString(),
                        "tableId" to tableId,
                        "tableName" to tableName,
                        "status" to OrderStatus.PENDING.toString(),
                        "timestamp" to cat.dam.mamadou.etable.supabase.Timestamp.now(),
                        "currentStage" to stageTag,
                        "completedStages" to listOf<String>(), // Lista vacía de etapas completadas
                        "items" to items.map { item ->
                            mapOf(
                                "id" to item.id,
                                "name" to item.name,
                                "quantity" to item.quantity,
                                "price" to item.price,
                                "etapa" to item.etapa  // Asegúrate de que etapa contiene "_OBS:texto" si hay observaciones
                            )
                        }
                    )
    
                    // Guardar también en SharedPreferences
                    val context = firestore.app.applicationContext
                    val sharedPrefs = context.getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
                    sharedPrefs.edit().putString("table_$tableId", stageTag).apply()
                    Log.d(TAG, "📝 Guardada etapa $stageTag para mesa $tableId en preferencias locales")

                    firestore.collection("orders")
                        .add(order)
                        .addOnSuccessListener {
                            Log.d(TAG, "Nueva orden creada con éxito: ${it.id}")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error al crear la orden", e)
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al buscar órdenes existentes", e)
                onComplete(false)
            }
    }

    /**
     * Updates the stage of an order
     * @param orderId ID of the order
     * @param stage New stage
     * @param onComplete Callback with the result
     */
    fun updateOrderStage(orderId: String, stage: String, onComplete: (Boolean) -> Unit) {
        firestore.collection("orders").document(orderId)
            .update("currentStage", stage)
            .addOnSuccessListener {
                Log.d(TAG, "Etapa de la orden actualizada a $stage")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar la etapa de la orden", e)
                onComplete(false)
            }
    }

    /**
     * Configura un listener para órdenes con un estado específico
     * @param status Estado de las órdenes a escuchar
     * @param onOrdersUpdated Callback con la lista de órdenes actualizada
     * @return ListenerRegistration para poder cancelar el listener
     */
    fun listenOrdersByStatus(status: OrderStatus, onOrdersUpdated: (List<Order>) -> Unit): ListenerRegistration {
        val collectionName = if (status == OrderStatus.COMPLETED) "history_orders" else "orders"
        
        return firestore.collection(collectionName)
            .whereEqualTo("status", status.toString())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for orders: ${error.message}")
                    onOrdersUpdated(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { document ->
                        try {
                            val data = document.data
                            if (data != null) {
                                val tableId = data["tableId"] as? Number
                                val tableName = data["tableName"] as? String ?: "Mesa ${tableId?.toInt() ?: 0}"
                                
                                val orderData = data.toMutableMap()
                                if (tableId != null) {
                                    orderData["tableId"] = tableId
                                }
                                orderData["tableName"] = tableName
                                
                                FirebaseModelConverter.mapToOrder(orderData, document.id)
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error converting order: ${e.message}")
                            null
                        }
                    }
                    
                    val sortedOrders = orders.sortedByDescending { it.timestamp }
                    onOrdersUpdated(sortedOrders)
                    
                } else {
                    onOrdersUpdated(emptyList())
                }
            }
    }

    /**
     * Updates the status of an order
     * @param orderId ID of the order to update
     * @param newStatus New status
     * @param callback Callback with the result
     */
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus, callback: (Boolean) -> Unit) {
        val orderRef = firestore.collection("orders").document(orderId)
        
        orderRef.update("status", newStatus.toString())
            .addOnSuccessListener {
                Log.d(TAG, "Estado de orden actualizado a $newStatus: $orderId")
                
                if (newStatus == OrderStatus.READY) {
                    Log.d(TAG, "🔔 Orden lista para servir: $orderId - Notificando")
                    notifyOrderUpdated(orderId)
                }
                
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar estado de orden: ${e.message}")
                callback(false)
            }
    }

    /**
     * Marca una orden como completada y envía notificación al camarero
     * También guarda una copia en la colección history_orders
     */
    fun markOrderAsCompleted(orderId: String, onComplete: (Boolean) -> Unit) {
        Log.d(TAG, "📋 Iniciando proceso de completar orden: $orderId")
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.e(TAG, "Orden no encontrada: $orderId")
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                val order = document.toObject(Order::class.java) ?: return@addOnSuccessListener
                val tableId = order.tableId ?: -1
                val tableName = order.tableName ?: "Mesa $tableId"
                val currentStage = order.currentStage ?: "UNKNOWN"
                
                // Obtener todos los datos originales de la orden
                val orderData = document.data
                if (orderData != null) {
                    Log.d(TAG, "📋 Datos de orden obtenidos correctamente: ${orderData.keys.joinToString()}")
                    
                    // Timestamp para completedAt (importante para ordenar en historial)
                    val completedAtTimestamp = cat.dam.mamadou.etable.supabase.Timestamp.now()
                    
                    // Preparar los datos para history_orders asegurando estructura correcta
                    val historyData = HashMap<String, Any>().apply {
                        // Copiar todos los campos originales
                        putAll(orderData)
                        // Actualizar campos necesarios
                        this["status"] = OrderStatus.COMPLETED.toString()
                        this["completedAt"] = completedAtTimestamp
                        this["tableId"] = tableId
                        this["tableName"] = tableName
                        this["currentStage"] = currentStage
                        
                        // Asegurar que los items tienen la estructura correcta
                        val items = orderData["items"] as? List<Map<String, Any>> ?: emptyList()
                        if (items.isNotEmpty()) {
                            this["items"] = items
                        }
                    }
                    
                    Log.d(TAG, "🔍 Intentando guardar en history_orders con ID: $orderId")
                    
                    // 1. Guardar en history_orders con set explícito y ID específico
                    firestore.collection("history_orders")
                        .document(orderId)
                        .set(historyData)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Orden guardada en historial con ID: $orderId")
                            
                            // 2. Ya está en el historial: quitarla de "orders" (nada lee pedidos COMPLETED de aquí)
                            firestore.collection("orders").document(orderId)
                                .delete()
                                .addOnSuccessListener {
                                    Log.d(TAG, "✅ Orden $orderId movida al historial")

                                    // 3. Crear notificación para camarero
                                    createNotificationForCompletedOrder(orderId, tableId, tableName, currentStage, onComplete)
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "❌ Error borrando la orden de orders: ${e.message}")
                                    createNotificationForCompletedOrder(orderId, tableId, tableName, currentStage, onComplete)
                                }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Error guardando orden en historial: ${e.message}", e)
                            
                            // Si falla el guardado en historial, intentar actualizar estado de todos modos
                            firestore.collection("orders").document(orderId)
                                .update("status", OrderStatus.COMPLETED.toString())
                                .addOnSuccessListener {
                                    Log.d(TAG, "✅ Orden $orderId marcada como completada (sin historial)")
                                    createNotificationForCompletedOrder(orderId, tableId, tableName, currentStage, onComplete)
                                }
                                .addOnFailureListener { updateError -> 
                                    Log.e(TAG, "❌ Error doble al guardar/actualizar: ${updateError.message}")
                                    onComplete(false)
                                }
                        }
                } else {
                    Log.e(TAG, "❌ No se pudo obtener los datos de la orden")
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error buscando orden: ${e.message}")
                onComplete(false)
            }
    }

    /**
     * Método auxiliar para crear notificación de orden completada
     */
    private fun createNotificationForCompletedOrder(
        orderId: String, 
        tableId: Int, 
        tableName: String, 
        currentStage: String,
        onComplete: (Boolean) -> Unit
    ) {
        // Usar ID único para la notificación (timestamp + orderId)
        val notificationId = "${System.currentTimeMillis()}_${orderId}"
        
        // Crear la notificación
        val notification = hashMapOf(
            "orderId" to orderId,
            "tableId" to tableId,
            "tableName" to tableName,
            "stage" to currentStage,
            "timestamp" to cat.dam.mamadou.etable.supabase.Timestamp.now(),
            "read" to false
        )
        
        // Guardar con ID específico para facilitar búsqueda
        firestore.collection("orderNotifications")
            .document(notificationId)
            .set(notification)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Notificación creada con ID: $notificationId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error creando notificación: ${e.message}")
                // La orden se completó pero falló la notificación
                onComplete(true)
            }
    }

    /**
     * Configura un listener para órdenes completadas
     * @param onOrdersUpdated Callback con la lista de órdenes completadas
     * @return ListenerRegistration para poder cancelar el listener
     */
    fun listenToCompletedOrders(onOrdersUpdated: (List<Order>) -> Unit): ListenerRegistration {
        return listenOrdersByStatus(OrderStatus.COMPLETED, onOrdersUpdated)
    }

    /**
     * Obtiene las órdenes activas para una mesa específica
     * @param tableId ID de la mesa
     * @param onComplete Callback con la lista de órdenes activas
     */
    fun getActiveOrdersForTable(tableId: Int, onComplete: (List<Order>) -> Unit) {
        firestore.collection("orders")
            .whereEqualTo("tableId", tableId)
            .whereNotEqualTo("status", OrderStatus.COMPLETED.toString())
            .get()
            .addOnSuccessListener { snapshot ->
                val orders = snapshot.documents.mapNotNull { document ->
                    try {
                        val data = document.data
                        if (data != null) {
                            FirebaseModelConverter.mapToOrder(data, document.id)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error convirtiendo orden: ${e.message}")
                        null
                    }
                }
                onComplete(orders)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo órdenes activas para mesa $tableId: ${e.message}", e)
                onComplete(emptyList())
            }
    }

    /**
     * Método para obtener la orden activa de una mesa específica
     */
    fun getActiveOrderForTable(tableId: Int, onComplete: (DocumentSnapshot?) -> Unit) {
        Log.d(TAG, "Buscando orden activa para mesa $tableId")
        
        firestore.collection("orders")
            .whereEqualTo("tableId", tableId)
            .whereNotEqualTo("status", OrderStatus.COMPLETED.toString())
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    // Obtener la orden más reciente si hay varias
                    val orderDoc = snapshot.documents.maxByOrNull { 
                        it.getTimestamp("createdAt")?.toDate()?.time ?: 0 
                    }
                    
                    Log.d(TAG, "Orden activa encontrada: ${orderDoc?.id}, etapa: ${orderDoc?.getString("currentStage")}")
                    onComplete(orderDoc)
                } else {
                    Log.d(TAG, "No se encontró orden activa para mesa $tableId")
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error buscando orden activa: ${e.message}")
                onComplete(null)
            }
    }

    /**
     * Marca los elementos de una orden como listos para servir
     * @param orderId ID de la orden
     * @param stageTag Etapa de la comanda (PRIMERS_BEGUDES, SEGONS, POSTRES)
     * @param onComplete Callback con el resultado
     */
    fun markOrderStageAsReady(orderId: String, stageTag: String, onComplete: (Boolean) -> Unit) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.e(TAG, "Orden no encontrada: $orderId")
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                val tableId = document.getLong("tableId")?.toInt() ?: -1
                val tableName = document.getString("tableName") ?: "Mesa $tableId"
                
                firestore.collection("orders").document(orderId)
                    .update("stages.$stageTag.ready", true)
                    .addOnSuccessListener {
                        Log.d(TAG, "Etapa $stageTag marcada como lista")
                        
                        // Creamos la notificación con un ID único para facilitar su recuperación
                        val notificationId = "${orderId}_${stageTag}_${System.currentTimeMillis()}"
                        val notification = hashMapOf(
                            "id" to notificationId,  // Añadir ID explícito
                            "orderId" to orderId,
                            "tableId" to tableId,
                            "tableName" to tableName,
                            "stage" to stageTag,
                            "timestamp" to cat.dam.mamadou.etable.supabase.Timestamp.now(),
                            "read" to false,
                            "processed" to false  // Añadir flag para seguimiento
                        )
                        
                        // Guardamos explícitamente con ID específico
                        Log.d(TAG, "🔔 Enviando notificación a Firestore: Orden $orderId, Mesa $tableId, Etapa $stageTag")
                        firestore.collection("orderNotifications")
                            .document(notificationId)  // Usar ID específico en lugar de auto-ID
                            .set(notification)  // Usar set en lugar de add
                            .addOnSuccessListener { 
                                Log.d(TAG, "✅ Notificación guardada correctamente con ID: $notificationId")
                                
                                CoroutineScope(Dispatchers.Main).launch {
                                    notifyOrderReady(document.toObject(Order::class.java) ?: Order(), stageTag)
                                }
                                
                                onComplete(true)
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "❌ Error al guardar notificación: ${e.message}")
                                onComplete(false)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al marcar etapa como lista: ${e.message}", e)
                        onComplete(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al buscar orden: ${e.message}", e)
                onComplete(false)
            }
    }

    /**
     * Marca una etapa como completada en Firebase
     */
    fun markStageAsCompleted(
        orderId: String? = null,
        tableId: Int? = null,
        stageTag: String,
        onComplete: (Boolean) -> Unit
    ) {
        Log.d(TAG, "Marcando etapa como completada: $stageTag")
        
        if (orderId != null) {
            // Si tenemos el ID de la orden, actualizar directamente
            firestore.collection("orders").document(orderId)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        updateStageCompletedInOrder(documentSnapshot.id, stageTag, onComplete)
                    } else {
                        Log.e(TAG, "No se encontró la orden con ID: $orderId")
                        onComplete(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error buscando orden por ID: ${e.message}")
                    onComplete(false)
                }
        } else if (tableId != null) {
            // Si no tenemos ID pero sí tableId, buscar la orden primero
            firestore.collection("orders")
                .whereEqualTo("tableId", tableId)
                .whereNotEqualTo("status", OrderStatus.COMPLETED.toString())
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val document = snapshot.documents.first()
                        updateStageCompletedInOrder(document.id, stageTag, onComplete)
                    } else {
                        Log.d(TAG, "No se encontró orden activa para la mesa $tableId")
                        onComplete(false)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error buscando orden por tableId: ${e.message}")
                    onComplete(false)
                }
        } else {
            Log.e(TAG, "Se requiere orderId o tableId para marcar una etapa como completada")
            onComplete(false)
        }
    }

    /**
     * Método auxiliar para actualizar la etapa en una orden
     */
    private fun updateStageCompletedInOrder(orderId: String, stageTag: String, onComplete: (Boolean) -> Unit) {
        firestore.collection("orders").document(orderId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Obtener etapas completadas actuales
                    val currentStages = document.get("completedStages") as? List<String> ?: listOf()
                    
                    // Determinar la siguiente etapa
                    val nextStage = when(stageTag) {
                        "PRIMERS_BEGUDES" -> "SEGONS"
                        "SEGONS" -> "POSTRES"
                        "POSTRES" -> "FINALIZADA"
                        else -> "FINALIZADA"
                    }
                    
                    // Crear una lista actualizada que incluya la etapa actual
                    val updatedStages = currentStages.toMutableList()
                    if (!updatedStages.contains(stageTag)) {
                        updatedStages.add(stageTag)
                    }
                    
                    // Determinar si todas las etapas están completadas
                    val allStagesCompleted = updatedStages.containsAll(
                        listOf("PRIMERS_BEGUDES", "SEGONS", "POSTRES")
                    )
                    
                    // Modificación clave: Sólo marcar como COMPLETED cuando se procesa la etapa FINALIZADA
                    val newStatus = if (stageTag == "FINALIZADA") {
                        OrderStatus.COMPLETED.toString()
                    } else {
                        OrderStatus.IN_PROGRESS.toString()
                    }
                    
                    // Actualizar en Firebase
                    document.reference.update(
                        mapOf(
                            "completedStages" to updatedStages,
                            "currentStage" to nextStage,
                            "status" to newStatus
                        )
                    )
                    .addOnSuccessListener {
                        Log.d(TAG, "✅ Etapa $stageTag marcada como completada en Firebase")
                        Log.d(TAG, "✅ Etapas completadas: $updatedStages")
                        Log.d(TAG, "✅ Nueva etapa actual: $nextStage")
                        
                        // Notificar que la orden ha sido actualizada
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error actualizando etapa: ${e.message}")
                        onComplete(false)
                    }
                } else {
                    Log.e(TAG, "Orden no encontrada: $orderId")
                    onComplete(false)
                }
            }
    }

    /**
     * Método para comprobar el estado de las órdenes de una mesa
     */
    fun checkTableOrderStatus(
        tableId: Int,
        onResult: (hasPendingOrders: Boolean, currentStage: String?, completedStages: List<String>) -> Unit
    ) {
        Log.d(TAG, "Verificando estado de órdenes para mesa $tableId")
        
        firestore.collection("orders")
            .whereEqualTo("tableId", tableId)
            .whereNotEqualTo("status", OrderStatus.COMPLETED.toString())
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val document = snapshot.documents.first()
                    val currentStage = document.getString("currentStage")
                    val orderStatus = document.getString("status")
                    val completedStages = document.get("completedStages") as? List<String> ?: emptyList()
                    
                    // Determinar si hay órdenes pendientes basado en etapa actual y estado
                    val pendingOrders = currentStage != "FINALIZADA" && orderStatus != OrderStatus.COMPLETED.toString()
                    
                    Log.d(TAG, "Mesa $tableId: órdenes pendientes: $pendingOrders, " +
                        "etapa: $currentStage, estado: $orderStatus, " +
                        "etapas completadas: $completedStages")
                    
                    onResult(pendingOrders, currentStage, completedStages)
                } else {
                    Log.d(TAG, "Mesa $tableId: no tiene órdenes activas")
                    onResult(false, null, emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error verificando estado de orden: ${e.message}")
                onResult(false, null, emptyList())
            }
    }

    /**
     * Determina la siguiente etapa después de la actual
     */
    private fun nextStage(currentStage: String): String {
        return when(currentStage) {
            "PRIMERS_BEGUDES" -> "SEGONS"
            "SEGONS" -> "POSTRES"
            "POSTRES" -> "FINALIZADA"
            else -> "FINALIZADA"
        }
    }

    /**
     * Notifica que una orden está lista
     */
    fun notifyOrderReady(order: Order, stage: String) {
        Log.d(TAG, "Notificando orden lista: ${order.id}, Mesa ${order.tableName}, Etapa $stage")
        OrderReadyEventBus.notifyOrderReady(
            orderId = order.id,
            tableName = order.tableName ?: "Mesa ${order.tableId}",
            stage = stage
        )
    }

    /**
     * Notifica que una orden ha sido actualizada
     */
    fun notifyOrderUpdated(orderId: String) {
        Log.d(TAG, "Notificando actualización de orden: $orderId")
        OrderReadyEventBus.notifyOrderUpdated(orderId)
    }

    /**
     * Configura un listener para notificaciones de órdenes
     * @param onNotification Callback que se ejecuta cuando hay una nueva notificación
     * @return ListenerRegistration para poder cancelar el listener
     */
    fun listenForOrderNotifications(onNotification: (String, String, String) -> Unit): ListenerRegistration {
        return firestore.collection("orderNotifications")
            .whereEqualTo("read", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error escuchando notificaciones: ${e.message}")
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val data = change.document.data
                        val orderId = data["orderId"] as? String ?: change.document.id
                        val tableName = data["tableName"] as? String ?: "Mesa desconocida"
                        val stage = data["stage"] as? String ?: "Etapa desconocida"
                        
                        Log.d(TAG, "📱 Nueva notificación recibida: Mesa $tableName, Etapa: $stage")
                        onNotification(orderId, tableName, stage)
                    }
                }
            }
    }

    /**
     * Método para obtener notificaciones pendientes
     * @param onComplete Callback con la lista de notificaciones pendientes
     */
    fun getPendingNotifications(onComplete: (List<NotificationInfo>) -> Unit) {
        Log.d(TAG, "Consultando notificaciones pendientes...")
        
        // Consulta más directa para mejorar el rendimiento
        firestore.collection("orderNotifications")
            .whereEqualTo("read", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)  // Limitar cantidad para mejor rendimiento
            .get()
            .addOnSuccessListener { snapshot ->
                val notifications = mutableListOf<NotificationInfo>()
                
                for (document in snapshot.documents) {
                    val data = document.data ?: continue
                    
                    // Extraer datos con comprobaciones de tipo más seguras
                    val orderId = data["orderId"] as? String ?: document.id
                    val tableId = data["tableName"] as? String ?: "Mesa desconocida"
                    val stage = data["stage"] as? String ?: "Etapa desconocida"
                    val timestamp = (data["timestamp"] as? cat.dam.mamadou.etable.supabase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
                    
                    notifications.add(NotificationInfo(
                        id = document.id,  // Usar el ID del documento
                        tableId = tableId,
                        stage = stage,
                        timestamp = timestamp
                    ))
                }
                
                Log.d(TAG, "📋 ${notifications.size} notificaciones pendientes obtenidas")
                onComplete(notifications)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error obteniendo notificaciones pendientes: ${e.message}")
                onComplete(emptyList())
            }
    }

    /**
     * Guarda o actualiza una orden
     */
    fun saveOrder(
        tableId: Int,
        tableName: String,
        items: List<OrderItem>,
        stageTag: String,
        completedStages: List<String> = emptyList(),
        onComplete: (String?, Boolean) -> Unit
    ) {
        if (items.isEmpty()) {
            onComplete(null, false)
            return
        }
        
        Log.d(TAG, "Guardando orden para mesa $tableId con etapa $stageTag")
        
        // Primero verificamos si ya existe una orden activa para esta mesa
        firestore.collection("orders")
            .whereEqualTo("tableId", tableId)
            .whereNotEqualTo("status", OrderStatus.COMPLETED.toString())
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    // Actualizar orden existente
                    val document = snapshot.documents.first()
                    val orderId = document.id
                    
                    // Obtener las etapas completadas existentes y añadir las nuevas
                    val existingCompletedStages = document.get("completedStages") as? List<String> ?: emptyList()
                    val updatedCompletedStages = (existingCompletedStages + completedStages).distinct()
                    
                    val updatedItems = items.map { item ->
                        mapOf(
                            "id" to item.id,
                            "name" to item.name,
                            "quantity" to item.quantity,
                            "price" to item.price,
                            "etapa" to item.etapa
                        )
                    }
                    
                    val updates = hashMapOf<String, Any>(
                        "items" to updatedItems,
                        "currentStage" to stageTag,
                        "completedStages" to updatedCompletedStages,
                        "status" to OrderStatus.IN_PROGRESS.toString()
                    )
                    
                    document.reference.update(updates)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Orden actualizada con ID: $orderId, etapa: $stageTag")
                            onComplete(orderId, true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Error actualizando orden: ${e.message}")
                            onComplete(orderId, false)
                        }
                } else {
                    // Crear nueva orden
                    val orderId = UUID.randomUUID().toString()
                    
                    val order = hashMapOf(
                        "id" to orderId,
                        "tableId" to tableId,
                        "tableName" to tableName,
                        "status" to OrderStatus.PENDING.toString(),
                        "timestamp" to cat.dam.mamadou.etable.supabase.Timestamp.now(),
                        "currentStage" to stageTag,
                        "completedStages" to completedStages,
                        "items" to items.map { item ->
                            mapOf(
                                "id" to item.id,
                                "name" to item.name,
                                "quantity" to item.quantity,
                                "price" to item.price,
                                "etapa" to item.etapa
                            )
                        }
                    )
                    
                    firestore.collection("orders").document(orderId)
                        .set(order)
                        .addOnSuccessListener {
                            Log.d(TAG, "✅ Nueva orden creada con ID: $orderId, etapa: $stageTag")
                            onComplete(orderId, true)
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "❌ Error creando orden: ${e.message}")
                            onComplete(null, false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error verificando órdenes existentes: ${e.message}")
                onComplete(null, false)
            }
    }

    /**
     * Normaliza la etapa con el punto de cocción
     */
    private fun normalizeStage(etapa: String, cookingPoint: CookingPoint?, observations: String?): String {
        var normalizedStage = etapa
        
        // Agregar punto de cocción si existe (siempre guardar el valor interno)
        if (cookingPoint != null) {
            normalizedStage += "_PUNTO:${cookingPoint.name}"
        }
        
        // Agregar observaciones si existen
        if (!observations.isNullOrBlank()) {
            normalizedStage += "_OBS:$observations"
        }
        
        return normalizedStage
    }
}
