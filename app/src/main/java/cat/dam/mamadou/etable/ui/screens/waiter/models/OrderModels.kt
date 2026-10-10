package cat.dam.mamadou.etable.ui.screens.waiter.models

import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.OrderStatus
import java.util.Date

// Enum para las etapas de la comanda
enum class ComandaStage {
    PRIMERS_BEGUDES, SEGONS, POSTRES, FINALIZADA
}


// Clase para representar un plato
data class Plato(
    val id: String = "",
    val nombre: String,
    val categoria: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val imagenUrl: String? = null,
    val disponible: Boolean = true,
    val imagen: Int = R.drawable.imagen, // Imagen local por defecto
    val requiresCookingPoint: Boolean = false // Propiedad para punto de cocción
)

// Clase para representar un item de orden
data class OrderItem(
    val plato: Plato,
    val cantidad: Int,
    val cookingPoint: CookingPoint? = null,  // Punto de cocción para carnes
    val observations: String = ""             // Observaciones generales
)

// Clase para representar una orden completa
data class Order(
    val id: String = "",
    val mesaId: Int? = null,
    val mesaNombre: String = "",
    val items: List<OrderItem> = emptyList(),
    val fecha: Date = Date(),
    val estado: OrderStatus = OrderStatus.PENDING,
    val currentStage: String? = null,
    val completedStages: List<String> = emptyList(), // Lista de etapas completadas
)

// Clase para representar una comanda para Firebase
data class ComandaOrder(
    val nombre: String,
    val cantidad: Int,
    val etapa: String = "",
    val cookingPoint: String? = null,         // Punto de cocción serializado
    val observations: String = ""             // Observaciones generales
)
