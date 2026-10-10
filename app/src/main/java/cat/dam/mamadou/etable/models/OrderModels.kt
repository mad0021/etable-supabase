package cat.dam.mamadou.etable.models

import java.util.Date

/**
 * Estados posibles para una orden
 */
enum class OrderStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    READY,
    REJECTED,
}

/**
 * Modelo para un ítem de orden
 */
data class OrderItem(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
    val etapa: String = ""
)

/**
 * Modelo para una orden completa
 */
data class Order(
    val id: String = "",
    val tableId: Int? = null,
    val tableName: String = "",
    val items: List<OrderItem> = emptyList(),
    val timestamp: Date = Date(),
    val status: OrderStatus = OrderStatus.PENDING,
    val currentStage: String? = null,
    val completedStages: List<String> = emptyList(), // Lista de etapas completadas
) {
    // Método para convertir a modelo de UI
    fun toUiModel(): cat.dam.mamadou.etable.ui.screens.waiter.models.Order {
        return cat.dam.mamadou.etable.ui.screens.waiter.models.Order(
            id = id,
            mesaId = tableId,
            mesaNombre = tableName,
            items = items.map { item ->
                cat.dam.mamadou.etable.ui.screens.waiter.models.OrderItem(
                    plato = cat.dam.mamadou.etable.ui.screens.waiter.models.Plato(
                        id = item.id,
                        nombre = item.name,
                    ),
                    cantidad = item.quantity
                )
            },
            fecha = timestamp,
            estado = status,
            currentStage = currentStage,
            completedStages = completedStages
        )
    }
    
    companion object {
        // Método para convertir desde modelo de UI
        fun fromUiModel(order: cat.dam.mamadou.etable.ui.screens.waiter.models.Order): Order {
            return Order(
                id = order.id,
                tableId = order.mesaId,
                tableName = order.mesaNombre,
                items = order.items.map { item ->
                    OrderItem(
                        id = item.plato.id,
                        name = item.plato.nombre,
                        quantity = item.cantidad
                    )
                },
                timestamp = order.fecha,
                status = order.estado,
                currentStage = order.currentStage,
                completedStages = order.completedStages
            )
        }
    }
}