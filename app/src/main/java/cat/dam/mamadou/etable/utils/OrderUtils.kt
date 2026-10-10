package cat.dam.mamadou.etable.utils

import android.util.Log
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.OrderItem

/**
 * Utilidades para procesar órdenes y sus elementos
 */
object OrderUtils {

    private const val TAG = "OrderUtils"
    
    /**
     * Extrae la etapa base de un elemento (sin punto de cocción ni observaciones)
     */
    fun extractBaseStage(item: OrderItem): String {
        val etapa = item.etapa ?: return ""
        return if (etapa.contains("_PUNTO:") || etapa.contains("_OBS:")) {
            etapa.substringBefore("_")
        } else {
            etapa
        }
    }
    
    /**
     * Extrae el punto de cocción de un elemento de la orden
     */
    fun extractCookingPoint(item: OrderItem): CookingPoint? {
        if (!item.etapa.contains("_PUNTO:")) return null
        
        val texto = item.etapa.substringAfter("_PUNTO:")
            .substringBefore("_OBS:")
        
        Log.d(TAG, "Extrayendo punto cocción de ${item.name}, texto: $texto")
        
        return try {
            // Intentar conversión directa
            try {
                CookingPoint.valueOf(texto.uppercase())
            } catch (e: IllegalArgumentException) {
                // Si falla, intentar con el método de compatibilidad
                CookingPoint.fromLocalizedValue(texto)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo punto cocción: $texto", e)
            null
        }
    }
    
    /**
     * Extrae observaciones de un elemento de la orden
     */
    fun extractObservations(item: OrderItem): String? {
        val etapa = item.etapa
        if (etapa.isNullOrEmpty()) return null
        
        return if (etapa.contains("_OBS:")) {
            val observations = etapa.substringAfter("_OBS:")
            // Devolver null si las observaciones están vacías
            if (observations.isNotBlank()) observations else null
        } else {
            null
        }
    }

    /**
     * Construye el string de etapa completo con punto de cocción y observaciones
     */
    fun buildStageString(
        baseStage: String,
        cookingPoint: CookingPoint? = null,
        observations: String? = null
    ): String {
        var result = baseStage
        
        if (cookingPoint != null) {
            result += "_PUNTO:${cookingPoint.name}"
        }
        
        if (!observations.isNullOrBlank()) {
            result += "_OBS:$observations"
        }
        
        return result
    }
    
    /**
     * Extrae el punto de cocción de una etapa de orden
     */
    fun extractCookingPoint(texto: String?): CookingPoint? {
        if (texto.isNullOrEmpty()) return null
        
        Log.d(TAG, "Extrayendo punto cocción de POLLASTRE, texto: $texto")
        
        return try {
            // Usar el helper de localización para extraer de manera segura
            OrderLocalizationHelper.extractCookingPoint(texto)
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo punto cocción: $texto", e)
            null
        }
    }
    
    /**
     * Formatea el punto de cocción para guardar en Firebase de manera independiente del idioma
     */
    fun formatCookingPoint(cookingPoint: CookingPoint?): String {
        return cookingPoint?.name ?: ""
    }
}