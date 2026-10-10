package cat.dam.mamadou.etable.constants

import android.content.Context
import cat.dam.mamadou.etable.R

/**
 * Estados de mesa que se usan internamente en la aplicación y Firebase
 * IMPORTANTE: NO TRADUCIR estos valores, se usan como identificadores en la BD
 */
object TableStates {
    const val FREE = "FREE" // Antes "LLIURE"
    const val OCCUPIED = "OCCUPIED" // Antes "OCUPADA"
    const val RESERVED = "RESERVED" // Antes "RESERVADA"
    
    val ALL = listOf(FREE, OCCUPIED, RESERVED)
    
    // Función para convertir valores antiguos por compatibilidad
    fun fromLegacyValue(value: String): String {
        return when (value.uppercase()) {
            "LLIURE" -> FREE
            "OCUPADA" -> OCCUPIED
            "RESERVADA" -> RESERVED
            else -> value
        }
    }
    
    // Función para obtener el estado traducible basado en estado interno
    fun getResourceKey(state: String): String {
        return when (fromLegacyValue(state)) {
            FREE -> "state_free"
            OCCUPIED -> "state_occupied"
            RESERVED -> "state_reserved"
            else -> "state_free"
        }
    }
    
    // Nueva función para obtener el texto traducido directamente
    fun getLocalizedText(context: Context, state: String): String {
        val resourceKey = getResourceKey(state)
        val resourceId = context.resources.getIdentifier(resourceKey, "string", context.packageName)
        return if (resourceId != 0) context.getString(resourceId) else state
    }
    
    // Obtener el color asociado al estado (puede ser útil para UI)
    fun getStateColor(state: String): androidx.compose.ui.graphics.Color {
        return when (fromLegacyValue(state)) {
            FREE -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde
            OCCUPIED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Rojo
            RESERVED -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Naranja
            else -> androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Gris
        }
    }
}