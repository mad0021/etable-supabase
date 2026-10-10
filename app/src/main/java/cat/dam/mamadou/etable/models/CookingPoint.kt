package cat.dam.mamadou.etable.models

import android.content.Context
import cat.dam.mamadou.etable.R

/**
 * Enumeración de los posibles puntos de cocción para carnes
 */
enum class CookingPoint {
    RARE,
    MEDIUM_RARE,
    MEDIUM,
    MEDIUM_WELL,
    WELL_DONE;
    
    /**
     * Propiedad display para usar cuando no hay contexto disponible
     * Proporciona un valor predeterminado en inglés para compatibilidad
     */
    val display: String
        get() = when (this) {
            RARE -> "Rare"
            MEDIUM_RARE -> "Medium rare"
            MEDIUM -> "Medium"
            MEDIUM_WELL -> "Medium well" 
            WELL_DONE -> "Well done"
        }
    
    /**
     * Retorna una representación localizada del punto de cocción
     * Se debe preferir este método cuando haya contexto disponible
     */
    fun getLocalizedString(context: Context): String {
        return when (this) {
            RARE -> context.getString(R.string.cooking_point_rare)
            MEDIUM_RARE -> context.getString(R.string.cooking_point_medium_rare)
            MEDIUM -> context.getString(R.string.cooking_point_medium)
            MEDIUM_WELL -> context.getString(R.string.cooking_point_medium_well)
            WELL_DONE -> context.getString(R.string.cooking_point_well_done)
        }
    }
    
    companion object {
        /**
         * Convierte valores localizados a valores del enum
         */
        fun fromLocalizedValue(value: String?): CookingPoint? {
            if (value == null) return null
            
            return when (value.lowercase()) {
                // Español
                "poco hecho" -> RARE
                "poco al punto" -> MEDIUM_RARE
                "al punto" -> MEDIUM
                "hecho" -> MEDIUM_WELL
                "muy hecho" -> WELL_DONE
                
                // Catalán
                "poc fet" -> RARE
                "poc al punt" -> MEDIUM_RARE
                "al punt" -> MEDIUM
                "fet" -> MEDIUM_WELL
                "molt fet" -> WELL_DONE
                
                // Inglés
                "rare" -> RARE
                "medium rare" -> MEDIUM_RARE
                "medium" -> MEDIUM
                "medium well" -> MEDIUM_WELL
                "well done", "well-done" -> WELL_DONE
                
                // Si no coincide con nada, devolver null
                else -> null
            }
        }
        
        /**
         * Obtiene todos los puntos de cocción traducidos al idioma actual
         */
        fun getAllLocalizedValues(context: Context): List<String> {
            return values().map { it.getLocalizedString(context) }
        }
    }
}