package cat.dam.mamadou.etable.utils

data class Reservation(
    val id: String = "",
    val tableId: Int = 0,
    val fecha: String = "",
    val hora: String = "",
    val nombreCliente: String = "",
    val numPersonas: Int = 0,
    val telefono: String = "",
    val estado: String = "CONFIRMADA"
)
