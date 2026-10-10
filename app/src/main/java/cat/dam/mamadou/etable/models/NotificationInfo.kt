package cat.dam.mamadou.etable.models

data class NotificationInfo(
    val id: String = "",
    val tableId: String = "",
    val stage: String = "",
    val timestamp: Long = 0L
)