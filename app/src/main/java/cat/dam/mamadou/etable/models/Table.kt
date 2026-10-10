package cat.dam.mamadou.etable.models

import android.content.Context
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.ui.screens.waiter.models.Comanda
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa

/**
 * Model for a table in the restaurant
 */
data class Table(
    val id: Int = 0,
    val name: String = "",
    val status: String = TableStates.FREE, // Usando constante en lugar de hardcoded "LLIURE"
    val customers: Int = 0,
    val orders: List<Comanda> = emptyList(),
    val isOccupied: Boolean = false
) {
    companion object {
        // Method to convert from UI model with backwards compatibility
        fun fromUiModel(mesa: Mesa): Table {
            // Convertir estados legacy a nuevos estados
            val normalizedStatus = TableStates.fromLegacyValue(mesa.estado)
            
            return Table(
                id = mesa.id,
                name = mesa.nombre,
                status = normalizedStatus,
                customers = mesa.clientes,
                orders = mesa.comandas,
                isOccupied = normalizedStatus == TableStates.OCCUPIED || 
                              normalizedStatus == TableStates.RESERVED || 
                              mesa.clientes > 0
            )
        }
        
        // Método para obtener el texto traducido del estado
        fun getStatusTranslation(context: Context, status: String): String {
            return TableStates.getLocalizedText(context, status)
        }
    }
    
    // Method to convert to UI model
    fun toUiModel(): Mesa {
        return Mesa(
            id = id,
            nombre = name,
            estado = status,
            clientes = customers,
            comandas = orders
        )
    }
    
    // Método para obtener el estado traducido
    fun getTranslatedStatus(context: Context): String {
        return getStatusTranslation(context, status)
    }
}