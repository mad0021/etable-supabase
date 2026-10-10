package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext

/**
 * Manager class for table-related operations in Firestore
 */
class FirebaseTableManager(private val firestore: FirebaseFirestore) {
    
    /**
     * Gets all tables from Firestore
     * @return Result with list of tables if successful, or an error
     */
    // Change from suspend function to regular function with callback
    fun getTables(callback: (Result<List<Map<String, Any>>>) -> Unit) {
        firestore.collection("tables")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val tablesList = mutableListOf<Map<String, Any>>()
                for (document in querySnapshot.documents) {
                    val tableData = document.data
                    if (tableData != null) {
                        // Ensure the table has an ID
                        val tableWithId = tableData.toMutableMap()
                        tableWithId["id"] = document.id.toIntOrNull() ?: continue
                        tablesList.add(tableWithId)
                    }
                }
                callback(Result.success(tablesList))
            }
            .addOnFailureListener { exception ->
                callback(Result.failure(exception))
            }
    }
    
    /**
     * Gets a table by ID
     * @param tableId ID of the table to retrieve
     * @return Result with table data if successful, or an error
     */
    suspend fun getTableById(tableId: Int): Result<Map<String, Any>?> {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = firestore.collection("tables")
                    .whereEqualTo("id", tableId)
                    .get()
                    .await()

                val tableData = if (!snapshot.isEmpty) snapshot.documents[0].data else null
                Result.success(tableData)
            } catch (e: Exception) {
                Log.e("FirebaseTableManager", "Error getting table by ID: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Gets a table by ID using callback pattern
     * @param tableId ID of the table to retrieve
     * @param callback Callback function that receives the table data
     */
    fun getTableById(tableId: Int, callback: (Map<String, Any>?) -> Unit) {
        firestore.collection("tables")
            .whereEqualTo("id", tableId)
            .get()
            .addOnSuccessListener { snapshot ->
                val tableData = if (!snapshot.isEmpty) snapshot.documents[0].data else null
                callback(tableData)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseTableManager", "Error getting table by ID: ${exception.message}", exception)
                callback(null)
            }
    }

    
    /**
     * Actualiza el estado de una mesa
     * @param tableId ID de la mesa a actualizar
     * @param estado Nuevo estado ("LLIURE", "OCUPADA", "RESERVADA")
     * @param clientes Número de clientes
     * @param onComplete Callback con el resultado
     */
    fun updateTableStatus(
        tableId: Int,
        estado: String,
        clientes: Int,
        onComplete: (Boolean) -> Unit
    ) {
        // Normalizar el estado usando TableStates
        val normalizedState = TableStates.fromLegacyValue(estado)
        
        // Primero, encontrar el documento con el tableId correspondiente
        firestore.collection("tables")
            .whereEqualTo("id", tableId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.e("FirebaseTableManager", "Mesa no encontrada: $tableId")
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                // Actualizar el estado de la mesa
                val tableDoc = snapshot.documents[0]
                val isOccupied = normalizedState == TableStates.OCCUPIED ||
                                  normalizedState == TableStates.RESERVED || 
                                  clientes > 0
                
                // Obtener el nombre actual de la mesa para preservarlo
                val currentName = tableDoc.getString("name") ?: ""
                
                // Usar los nombres de campo que se ven en Firebase
                tableDoc.reference.update(
                    mapOf(
                        "status" to normalizedState,
                        "clientes" to clientes,
                        "isOccupied" to isOccupied,
                        "name" to currentName // Preservar el nombre actual
                    )
                )
                .addOnSuccessListener {
                    Log.d("FirebaseTableManager", "Estado de mesa actualizado: $tableId")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseTableManager", "Error actualizando estado de mesa: ${e.message}", e)
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTableManager", "Error buscando mesa: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Actualiza una mesa completa
     * @param mesa Modelo de UI de la mesa
     * @param onComplete Callback con el resultado
     */
    fun updateTable(mesa: cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa, onComplete: (Boolean) -> Unit) {
        // Convertir el modelo de UI a modelo de Firebase
        val table = cat.dam.mamadou.etable.models.Table.fromUiModel(mesa)
        
        // Actualizar en Firestore
        firestore.collection("tables")
            .whereEqualTo("id", mesa.id)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // Si no existe, crear nueva mesa
                    firestore.collection("tables").add(table.toMap())
                        .addOnSuccessListener {
                            Log.d("FirebaseTableManager", "Mesa creada: ${mesa.id}")
                            onComplete(true)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FirebaseTableManager", "Error creando mesa: ${e.message}", e)
                            onComplete(false)
                        }
                } else {
                    // Si existe, actualizar
                    snapshot.documents[0].reference.update(table.toMap())
                        .addOnSuccessListener {
                            Log.d("FirebaseTableManager", "Mesa actualizada: ${mesa.id}")
                            onComplete(true)
                        }
                        .addOnFailureListener { e -> 
                            Log.e("FirebaseTableManager", "Error actualizando mesa: ${e.message}", e)
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseTableManager", "Error buscando mesa: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Escucha cambios en las mesas en tiempo real
     * @param onTablesUpdated Callback que se llama cuando hay cambios en las mesas
     * @return ListenerRegistration para detener la escucha cuando sea necesario
     */
    fun listenToTables(onTablesUpdated: (List<cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa>) -> Unit): cat.dam.mamadou.etable.supabase.ListenerRegistration {
        return firestore.collection("tables")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FirebaseTableManager", "Error escuchando mesas: ${e.message}", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val tables = snapshot.documents.mapNotNull { doc ->
                        try {
                            // Obtener datos con manejo de tipos más seguro
                            val data = doc.data ?: return@mapNotNull null
                            
                            // Usar Long para id y convertir a Int
                            val id = when (val idValue = data["id"]) {
                                is Long -> idValue.toInt()
                                is Int -> idValue
                                is Number -> idValue.toInt()
                                is String -> idValue.toIntOrNull() ?: return@mapNotNull null
                                else -> return@mapNotNull null
                            }
                            
                            // Obtener el nombre de la mesa, si existe
                            val nombre = when {
                                data.containsKey("name") -> data["name"] as? String
                                data.containsKey("nombre") -> data["nombre"] as? String
                                else -> ""
                            } ?: ""
                            
                            // Comprobar tanto "estado" como "status" para mayor compatibilidad
                            val estado = when {
                                data.containsKey("estado") -> TableStates.fromLegacyValue(data["estado"] as? String ?: "")
                                data.containsKey("status") -> TableStates.fromLegacyValue(data["status"] as? String ?: "")
                                else -> TableStates.FREE
                            } ?: TableStates.FREE
                            
                            // Comprobar tanto "clientes" como "customers" para mayor compatibilidad
                            val clientes = when {
                                data.containsKey("clientes") -> when (val clientesValue = data["clientes"]) {
                                    is Long -> clientesValue.toInt()
                                    is Int -> clientesValue
                                    is Number -> clientesValue.toInt()
                                    is String -> clientesValue.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                data.containsKey("customers") -> when (val customersValue = data["customers"]) {
                                    is Long -> customersValue.toInt()
                                    is Int -> customersValue
                                    is Number -> customersValue.toInt()
                                    is String -> customersValue.toIntOrNull() ?: 0
                                    else -> 0
                                }
                                else -> 0
                            }
                            
                            // Obtener comandas con verificación de tipo
                            val ordersData = when (val ordersValue = data["orders"]) {
                                is List<*> -> ordersValue
                                else -> emptyList<Any>()
                            }
                            
                            val orders = ordersData.mapNotNull { orderItem ->
                                if (orderItem is Map<*, *>) {
                                    val nombreComanda = orderItem["nombre"] as? String ?: ""
                                    val cantidad = when (val cantidadValue = orderItem["cantidad"]) {
                                        is Long -> cantidadValue.toInt()
                                        is Int -> cantidadValue
                                        is Number -> cantidadValue.toInt()
                                        is String -> cantidadValue.toIntOrNull() ?: 0
                                        else -> 0
                                    }
                                    
                                    cat.dam.mamadou.etable.ui.screens.waiter.models.Comanda(
                                        nombre = nombreComanda,
                                        cantidad = cantidad
                                    )
                                } else null
                            }
                            
                            // Crear y devolver el objeto Mesa
                            val mesa = cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa(
                                id = id,
                                nombre = nombre,
                                estado = estado,
                                clientes = clientes,
                                comandas = orders
                            )
                            
                            // Registrar para depuración
                            Log.d("FirebaseTableManager", "Mesa mapeada: id=${mesa.id}, nombre=${mesa.nombre}, estado=${mesa.estado}, clientes=${mesa.clientes}")
                            
                            mesa
                        } catch (ex: Exception) {
                            Log.e("FirebaseTableManager", "Error mapeando mesa: ${ex.message}", ex)
                            null
                        }
                    }
                    
                    // Ordenar las mesas por ID para una visualización consistente
                    val sortedTables = tables.sortedBy { it.id }
                    onTablesUpdated(sortedTables)
                }
            }
    }
    
    // Método de extensión para convertir Table a Map
    private fun cat.dam.mamadou.etable.models.Table.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "status" to status,
            "customers" to customers,
            "isOccupied" to isOccupied,
            "orders" to orders.map { comanda ->
                mapOf(
                    "nombre" to comanda.nombre,
                    "cantidad" to comanda.cantidad
                )
            }
        )
    }
    
    /**
     * Creates or updates a table (callback version)
     * @param tableData Table data to store
     * @param onComplete Callback with result
     */
    fun saveTable(tableData: Map<String, Any>, onComplete: (Result<Boolean>) -> Unit) {
        val tableId = tableData["id"] as? Int
        if (tableId == null) {
            onComplete(Result.failure(Exception("Table ID is required")))
            return
        }
        
        // Check if table exists
        firestore.collection("tables")
            .whereEqualTo("id", tableId)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    if (snapshot.isEmpty) {
                        // Create new table
                        firestore.collection("tables").add(tableData)
                            .addOnSuccessListener {
                                onComplete(Result.success(true))
                            }
                            .addOnFailureListener { e ->
                                onComplete(Result.failure(e))
                            }
                    } else {
                        // Update existing table
                        snapshot.documents[0].reference.set(tableData)
                            .addOnSuccessListener {
                                onComplete(Result.success(true))
                            }
                            .addOnFailureListener { e ->
                                onComplete(Result.failure(e))
                            }
                    }
                } catch (e: Exception) {
                    onComplete(Result.failure(e))
                }
            }
            .addOnFailureListener { e ->
                onComplete(Result.failure(e))
            }
    }
    
    /**
     * Elimina una mesa
     * @param tableId ID de la mesa a eliminar
     * @param onComplete Callback con el resultado de la operación (éxito o fracaso)
     */
    fun deleteTable(tableId: Int, onComplete: (Boolean) -> Unit) {
        firestore.collection("tables")
            .whereEqualTo("id", tableId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                // Eliminar la mesa
                snapshot.documents[0].reference.delete()
                    .addOnSuccessListener {
                        // Obtener y reordenar todas las mesas restantes
                        firestore.collection("tables").get()
                            .addOnSuccessListener { allTables ->
                                val sortedTables = allTables.documents.sortedBy { 
                                    (it["id"] as? Number)?.toInt() ?: 0 
                                }
                                
                                // Actualizar IDs secuencialmente
                                val batch = firestore.batch()
                                sortedTables.forEachIndexed { index, document ->
                                    val newId = index + 1
                                    if ((document["id"] as? Int) != newId) {
                                        batch.update(document.reference, "id", newId)
                                    }
                                }
                                
                                batch.commit().addOnSuccessListener {
                                    onComplete(true)
                                }.addOnFailureListener { 
                                    onComplete(false) 
                                }
                            }
                    }
                    .addOnFailureListener { 
                        onComplete(false) 
                    }
            }
    }
}