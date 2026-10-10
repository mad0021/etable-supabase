package cat.dam.mamadou.etable.utils.firebase

/**
 * Bus de eventos para notificaciones de solicitudes de descanso
 */
object BreakRequestEventBus {
    private val listeners = mutableListOf<(BreakRequest) -> Unit>()
    
    // Método para emitir notificación de nueva solicitud
    fun notifyNewBreakRequest(breakRequest: BreakRequest) {
        listeners.forEach { it(breakRequest) }
    }
    
    // Agregar listener
    fun addListener(listener: (BreakRequest) -> Unit): Any {
        listeners.add(listener)
        return listener
    }
    
    // Eliminar listener
    fun removeListener(listener: Any) {
        if (listener is Function1<*, *>) {
            @Suppress("UNCHECKED_CAST")
            listeners.remove(listener as (BreakRequest) -> Unit)
        }
    }
}