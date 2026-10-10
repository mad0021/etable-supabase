package cat.dam.mamadou.etable.utils

import android.content.Context
import android.util.Log
import cat.dam.mamadou.etable.constants.OrderStages
import cat.dam.mamadou.etable.constants.TableStates

/**
 * Helper para la migración de datos antiguos y la compatibilidad
 */
object LegacyDataHelper {
    private const val TAG = "LegacyDataHelper"
    
    /**
     * Convierte un estado de mesa legacy al nuevo formato
     */
    fun convertTableState(oldState: String?): String {
        if (oldState == null) return TableStates.FREE
        
        return try {
            TableStates.fromLegacyValue(oldState)
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo estado de mesa: $oldState", e)
            TableStates.FREE
        }
    }
    
    /**
     * Convierte una etapa de pedido legacy al nuevo formato
     */
    fun convertOrderStage(oldStage: String?): String {
        if (oldStage == null) return OrderStages.STARTERS_DRINKS
        
        return try {
            OrderStages.fromLegacyValue(oldStage)
        } catch (e: Exception) {
            Log.e(TAG, "Error convirtiendo etapa de pedido: $oldStage", e)
            OrderStages.STARTERS_DRINKS
        }
    }
    
    /**
     * Obtiene el texto traducido para un estado de mesa
     */
    fun getTableStateText(context: Context, state: String): String {
        val normalizedState = convertTableState(state)
        val resourceKey = TableStates.getResourceKey(normalizedState)
        
        val resourceId = context.resources.getIdentifier(resourceKey, "string", context.packageName)
        return if (resourceId != 0) context.getString(resourceId) else state
    }
    
    /**
     * Obtiene el texto traducido para una etapa de pedido
     */
    fun getOrderStageText(context: Context, stage: String): String {
        val normalizedStage = convertOrderStage(stage)
        val resourceKey = OrderStages.getResourceKey(normalizedStage)
        
        val resourceId = context.resources.getIdentifier(resourceKey, "string", context.packageName)
        return if (resourceId != 0) context.getString(resourceId) else stage
    }
}