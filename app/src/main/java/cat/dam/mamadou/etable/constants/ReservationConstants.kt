package cat.dam.mamadou.etable.constants

import cat.dam.mamadou.etable.R

/**
 * Estados de reserva usados internamente en la base de datos
 * NOTA: NO traducir estas constantes, son identificadores internos
 */
object ReservationConstants {
    // Estados de reserva
    const val STATUS_CONFIRMED = "CONFIRMED"  // Antes "CONFIRMADA"
    const val STATUS_PENDING = "PENDING"      // Antes "PENDIENTE"
    const val STATUS_CANCELLED = "CANCELLED"  // Antes "CANCELADA"
    
    // Para compatibilidad con valores existentes
    fun fromLegacyStatus(status: String): String {
        return when (status.uppercase()) {
            "CONFIRMADA" -> STATUS_CONFIRMED
            "PENDIENTE" -> STATUS_PENDING
            "CANCELADA" -> STATUS_CANCELLED
            else -> status
        }
    }
    
    // Para mostrar el estado traducido en la UI
    fun getStatusResourceId(status: String): Int {
        return when (fromLegacyStatus(status)) {
            STATUS_CONFIRMED -> R.string.reservation_status_confirmed
            STATUS_PENDING -> R.string.reservation_status_pending
            STATUS_CANCELLED -> R.string.reservation_status_cancelled
            else -> R.string.reservation_status_confirmed
        }
    }
}