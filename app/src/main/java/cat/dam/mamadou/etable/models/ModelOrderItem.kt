package cat.dam.mamadou.etable.models

/**
 * Modelo que representa un ítem de orden para el administrador/cocina
 */
data class ModelOrderItem(
    val id: String,
    val name: String,
    val price: Double = 0.0,
    val quantity: Int = 0,
    val etapa: String = "",
    val cookingPoint: String? = null,    // Punto de cocción para carnes
    val observations: String = "",       // Observaciones adicionales
    val ready: Boolean = false           // Indica si el ítem está listo
)