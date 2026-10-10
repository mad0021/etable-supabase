package cat.dam.mamadou.etable.ui.screens.waiter.models

// Data class para representar una mesa
data class Mesa(
    val id: Int,
    val nombre: String = "",
    val estado: String,
    val clientes: Int = 0,
    val comandas: List<Comanda> = emptyList()
)

// Data class para representar una comanda
data class Comanda(
    val nombre: String,
    val cantidad: Int
)