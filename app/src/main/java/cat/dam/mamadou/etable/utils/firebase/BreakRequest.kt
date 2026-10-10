package cat.dam.mamadou.etable.utils.firebase

import java.util.Date

/**
 * Modelo para solicitudes de descanso
 */
data class BreakRequest(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val fullName: String = "", // Añadimos el campo para el nombre completo
    val content: String = "",
    val reason: String = "",
    val timestamp: Date = Date(),
    val viewed: Boolean = false,
    val status: String = "pending"
)