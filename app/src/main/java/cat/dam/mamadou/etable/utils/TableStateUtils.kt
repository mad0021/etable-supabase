package cat.dam.mamadou.etable.utils

import android.content.Context
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates

object TableStateUtils {
    /**
     * Convierte un estado interno de mesa a su representación localizada
     */
    fun getLocalizedTableState(context: Context, internalState: String): String {
        return when (TableStates.fromLegacyValue(internalState)) {
            TableStates.FREE -> context.getString(R.string.table_state_free)
            TableStates.OCCUPIED -> context.getString(R.string.table_state_occupied)
            TableStates.RESERVED -> context.getString(R.string.table_state_reserved)
            else -> internalState // Fallback al valor original
        }
    }
    
    /**
     * Convierte un estado localizado a su valor interno
     */
    fun getInternalTableState(context: Context, localizedState: String): String {
        return when (localizedState) {
            context.getString(R.string.table_state_free) -> TableStates.FREE
            context.getString(R.string.table_state_occupied) -> TableStates.OCCUPIED
            context.getString(R.string.table_state_reserved) -> TableStates.RESERVED
            else -> localizedState // Fallback al valor original
        }
    }
}