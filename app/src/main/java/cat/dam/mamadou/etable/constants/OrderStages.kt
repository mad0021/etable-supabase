package cat.dam.mamadou.etable.constants

import android.content.Context
import cat.dam.mamadou.etable.R

/**
 * Constantes para las etapas de pedidos
 * IMPORTANTE: NO TRADUCIR estos valores, son identificadores internos
 */
object OrderStages {
    // Valores internos para la BD
    const val STARTERS_DRINKS = "PRIMERS_BEGUDES"
    const val MAIN_COURSES = "SEGONS"
    const val DESSERTS = "POSTRES"
    const val COMPLETED = "FINALIZADA"
    
    // Lista completa de etapas
    val ALL = listOf(STARTERS_DRINKS, MAIN_COURSES, DESSERTS, COMPLETED)
    
    /**
     * Convierte valores antiguos al formato estándar
     * Necesario para compatibilidad con datos existentes
     */
    fun fromLegacyValue(value: String): String {
        return when (value.uppercase()) {
            "PRIMERS", "PRIMERS_BEGUDES", "STARTERS", "STARTERS_DRINKS" -> STARTERS_DRINKS
            "SEGONS", "MAIN", "MAIN_COURSES" -> MAIN_COURSES
            "POSTRES", "DESSERTS" -> DESSERTS
            "FINALIZADA", "COMPLETED" -> COMPLETED
            else -> value
        }
    }
    
    /**
     * Obtiene el ID del recurso string para una etapa
     */
    fun getResourceId(stage: String): Int {
        return when {
            stage.contains(STARTERS_DRINKS, ignoreCase = true) -> 
                R.string.stage_starters_drinks
                
            stage.contains(MAIN_COURSES, ignoreCase = true) -> 
                R.string.stage_main_courses
                
            stage.contains(DESSERTS, ignoreCase = true) -> 
                R.string.stage_desserts
                
            stage.contains(COMPLETED, ignoreCase = true) -> 
                R.string.stage_completed
                
            else -> R.string.stage_starters_drinks
        }
    }
    
    /**
     * Obtiene la clave del recurso string para una etapa
     */
    fun getResourceKey(stage: String): String {
        return when {
            stage.contains(STARTERS_DRINKS, ignoreCase = true) -> "stage_starters_drinks"
            stage.contains(MAIN_COURSES, ignoreCase = true) -> "stage_main_courses"
            stage.contains(DESSERTS, ignoreCase = true) -> "stage_desserts"
            stage.contains(COMPLETED, ignoreCase = true) -> "stage_completed"
            else -> "stage_starters_drinks"
        }
    }
    
    /**
     * Obtiene el texto traducido de una etapa
     */
    fun getLocalizedText(context: Context, stage: String): String {
        val resourceId = getResourceId(stage)
        return context.getString(resourceId)
    }
    
    /**
     * Verifica si una etapa pertenece a un tipo específico
     */
    fun matchesStage(itemStage: String, targetStage: String): Boolean {
        // Extraer la etapa base (sin punto de cocción ni observaciones)
        val baseStage = itemStage.substringBefore("_PUNTO:").substringBefore("_OBS:")
        
        // Verificar si la etapa base coincide con la etapa objetivo
        return when (targetStage) {
            STARTERS_DRINKS -> baseStage.contains("PRIMERS", ignoreCase = true) || 
                              baseStage.contains("BEGUDES", ignoreCase = true) ||
                              baseStage.contains("STARTERS", ignoreCase = true)
            MAIN_COURSES -> baseStage.contains("SEGONS", ignoreCase = true) ||
                          baseStage.contains("MAIN", ignoreCase = true)
            DESSERTS -> baseStage.contains("POSTRES", ignoreCase = true) ||
                      baseStage.contains("DESSERTS", ignoreCase = true)
            COMPLETED -> baseStage.contains("FINALIZADA", ignoreCase = true) ||
                       baseStage.contains("COMPLETED", ignoreCase = true)
            else -> false
        }
    }
}