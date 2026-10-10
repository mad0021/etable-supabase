package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.models.OrderItem
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.models.Table
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuCategories
import cat.dam.mamadou.etable.ui.screens.waiter.models.Comanda
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import cat.dam.mamadou.etable.ui.screens.waiter.models.Plato
import java.util.Date

/**
 * Clase de utilidad para convertir entre modelos de Firebase y modelos de UI
 */
object FirebaseModelConverter {
    /**
     * Convierte un Map de Firestore a un modelo Order
     */
    fun mapToOrder(data: Map<String, Any>, docId: String): Order {
        try {
            Log.d("FirebaseModelConverter", "Convirtiendo documento $docId a Order")
            
            val tableId = (data["tableId"] as? Number)?.toInt()
            val tableName = data["tableName"] as? String ?: "Mesa ${tableId ?: 0}"
            val statusStr = data["status"] as? String ?: OrderStatus.PENDING.toString()
            val timestamp = (data["timestamp"] as? cat.dam.mamadou.etable.supabase.Timestamp)?.toDate() ?: java.util.Date()
            val currentStage = data["currentStage"] as? String
            
            // Convertir items con mejor soporte para punto de cocción
            val itemsData = data["items"] as? List<Map<String, Any>> ?: emptyList()
            Log.d("FirebaseModelConverter", "Encontrados ${itemsData.size} items en la orden")
            
            val items = itemsData.map { itemData ->
                val etapa = itemData["etapa"] as? String ?: ""
                
                // Log para verificar la información de cocción y observaciones
                if (etapa.contains("_PUNTO:") || etapa.contains("_OBS:")) {
                    Log.d("FirebaseModelConverter", "Item con info especial: ${itemData["name"]}, etapa: $etapa")
                }
                
                OrderItem(
                    id = itemData["id"] as? String ?: "",
                    name = itemData["name"] as? String ?: "",
                    quantity = (itemData["quantity"] as? Number)?.toInt() ?: 0,
                    etapa = etapa,
                    price = (itemData["price"] as? Number)?.toDouble() ?: 0.0
                )
            }
            
            return Order(
                id = docId,
                tableId = tableId,
                tableName = tableName,
                items = items,
                timestamp = timestamp,
                status = try { OrderStatus.valueOf(statusStr) } catch (e: Exception) { OrderStatus.PENDING },
                currentStage = currentStage
            )
        } catch (e: Exception) {
            Log.e("FirebaseModelConverter", "Error convirtiendo orden: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Convierte un modelo MenuItem a un Map para Firestore
     */
    fun menuItemToMap(menuItem: MenuItem): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        
        map["name"] = menuItem.name
        map["category"] = menuItem.category.name
        map["isAvailable"] = menuItem.isAvailable
        map["requiresCookingPoint"] = menuItem.requiresCookingPoint
        
        menuItem.imageUrl?.let { map["imageUrl"] = it }
        
        return map
    }

    /**
     * Extrae el punto de cocción y las observaciones de una orden
     */
    fun extractCookingPointAndObservations(order: Order): Pair<Map<String, CookingPoint?>, Map<String, String?>> {
        val cookingPoints = mutableMapOf<String, CookingPoint?>()
        val observations = mutableMapOf<String, String?>()
        
        Log.d("FirebaseModelConverter", "Procesando orden ${order.id} con ${order.items.size} items")
        
        order.items.forEach { item ->
            val itemKey = "${order.id}_${item.id}"
            
            // Extraer punto de cocción
            if (item.etapa.contains("_PUNTO:")) {
                val cookingPointText = item.etapa.substringAfter("_PUNTO:")
                    .substringBefore("_OBS:")
                
                try {
                    val cookingPoint = try {
                        // Primero intentar convertir directamente por nombre de enum
                        CookingPoint.valueOf(cookingPointText.uppercase())
                    } catch (e: IllegalArgumentException) {
                        // Si falla, usar el método de compatibilidad para textos localizados
                        CookingPoint.fromLocalizedValue(cookingPointText)
                    }
                    cookingPoints[itemKey] = cookingPoint
                } catch (e: Exception) {
                    Log.e("FirebaseModelConverter", "Error extrayendo punto cocción: $cookingPointText", e)
                }
            }
            
            // Extraer observaciones
            if (item.etapa.contains("_OBS:")) {
                val observation = item.etapa.substringAfter("_OBS:")
                observations[itemKey] = observation
                
                Log.d("FirebaseModelConverter", "Observación extraída para ${item.name}: $observation")
            }
        }
        
        return Pair(cookingPoints, observations)
    }
}
