package cat.dam.mamadou.etable.utils

import android.content.Context
import cat.dam.mamadou.etable.models.CookingPoint

/**
 * Helper para gestionar los puntos de cocción y su presentación localizada
 */
object CookingPointHelper {
    
    /**
     * Obtiene todos los puntos de cocción localizados según el idioma actual de la app
     */
    fun getAllLocalizedCookingPoints(context: Context): List<String> {
        return CookingPoint.values().map { cookingPoint ->
            cookingPoint.getLocalizedString(context)
        }
    }
    
    /**
     * Convierte de texto localizado a CookingPoint enum
     */
    fun fromLocalizedString(context: Context, localizedText: String): CookingPoint? {
        return CookingPoint.values().find { cookingPoint ->
            cookingPoint.getLocalizedString(context) == localizedText
        }
    }
    
    /**
     * Obtiene el texto localizado para un CookingPoint específico
     */
    fun getLocalizedText(context: Context, cookingPoint: CookingPoint): String {
        return cookingPoint.getLocalizedString(context)
    }
    
    /**
     * Convierte de nombre de enum interno a texto localizado
     */
    fun fromEnumNameToLocalized(context: Context, enumName: String): String? {
        return try {
            val cookingPoint = CookingPoint.valueOf(enumName.uppercase())
            cookingPoint.getLocalizedString(context)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Función de compatibilidad con código anterior
     */
    fun getLocalizedCookingPoints(context: Context): List<String> {
        return getAllLocalizedCookingPoints(context)
    }
}