package cat.dam.mamadou.etable.utils

import android.util.Log
import cat.dam.mamadou.etable.models.CookingPoint

/**
 * Clase auxiliar para manejar la localización de órdenes
 */
object OrderLocalizationHelper {
    
    private const val TAG = "OrderLocalizationHelper"
    
    /**
     * Extrae punto de cocción de manera segura, independiente del idioma
     */
    fun extractCookingPoint(rawText: String?): CookingPoint? {
        if (rawText.isNullOrEmpty()) return null
        
        return try {
            // Intentar convertir directamente
            try {
                CookingPoint.valueOf(rawText.uppercase())
            } catch (e: IllegalArgumentException) {
                // Si falla la conversión directa, usar el método de compatibilidad
                Log.d(TAG, "Intentando conversión legacy para: $rawText")
                CookingPoint.fromLocalizedValue(rawText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo punto de cocción: $rawText", e)
            null
        }
    }
    
    /**
     * Prepara la etapa (stage) de un item para guardar en Firebase
     * Garantiza que el punto de cocción sea almacenado en formato neutro al idioma
     */
    fun prepareItemStage(etapa: String, cookingPoint: CookingPoint?, observation: String?): String {
        val baseStage = etapa
        val cpPart = if (cookingPoint != null) "_PUNTO:${cookingPoint.name}" else ""
        val obsPart = if (!observation.isNullOrEmpty()) "_OBS:$observation" else ""
        
        return baseStage + cpPart + obsPart
    }
    
    /**
     * Extrae todas las partes de la etapa de un item
     */
    fun extractItemStageParts(fullStage: String): Triple<String, CookingPoint?, String?> {
        val baseStage = if (fullStage.contains("_PUNTO:") || fullStage.contains("_OBS:")) {
            fullStage.substringBefore("_PUNTO:").substringBefore("_OBS:")
        } else {
            fullStage
        }
        
        val cookingPoint = if (fullStage.contains("_PUNTO:")) {
            val cpText = fullStage.substringAfter("_PUNTO:")
                .substringBefore("_OBS:")
            extractCookingPoint(cpText)
        } else null
        
        val observation = if (fullStage.contains("_OBS:")) {
            fullStage.substringAfter("_OBS:")
        } else null
        
        return Triple(baseStage, cookingPoint, observation)
    }
}