package cat.dam.mamadou.etable.models

import java.util.Date

/**
 * Modelo para las solicitudes de descanso de los camareros
 */
data class BreakRequest(
    val id: String,
    val userId: String,
    val userName: String,
    val timestamp: Date,
    val reason: String,
    val status: String,
    val isViewed: Boolean = false, // Propiedad para rastrear si la solicitud ha sido vista
    val fullName: String = "" // Nombre completo opcional del solicitante
)