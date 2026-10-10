package cat.dam.mamadou.etable.ui.screens.admin.models

/**
 * Estados de mesa
 */
object TableStates {
    const val LLIURE = "LLIURE"
    const val OCUPADA = "OCUPADA"
    const val RESERVADA = "RESERVADA"
    
    val ALL = listOf(LLIURE, OCUPADA, RESERVADA)
}