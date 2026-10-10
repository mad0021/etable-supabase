package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import cat.dam.mamadou.etable.constants.ReservationConstants
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import cat.dam.mamadou.etable.supabase.Query
import cat.dam.mamadou.etable.supabase.Task
import cat.dam.mamadou.etable.supabase.DocumentReference
import kotlinx.coroutines.Dispatchers
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Manager class for reservation-related operations in Firestore
 */
class FirebaseReservationManager(private val firestore: FirebaseFirestore) {
    
    /**
     * Crea una nueva reserva
     * @param reservation Datos de la reserva
     * @return Task con resultado de la operación
     */
    fun createReservation(reservation: Map<String, Any>): Task<DocumentReference> {
        // Normalizar el estado antes de guardar
        val updatedReservation = reservation.toMutableMap()
        if (reservation.containsKey("estado")) {
            val normalizedStatus = ReservationConstants.fromLegacyStatus(reservation["estado"] as String)
            updatedReservation["estado"] = normalizedStatus
        }
        
        return firestore.collection("reservations")
            .add(updatedReservation)
            .addOnSuccessListener {
                Log.d("FirebaseReservationManager", "Reservation created successfully")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseReservationManager", "Error creating reservation: ${e.message}", e)
            }
    }
    
    /**
     * Actualiza una reserva existente
     * @param reservationId ID de la reserva
     * @param reservationData Nuevos datos de la reserva
     * @return Task con resultado de la operación
     */
    fun updateReservation(reservationId: String, reservationData: Map<String, Any>): Task<Unit> {
        return firestore.collection("reservations")
            .document(reservationId)
            .update(reservationData)
            .addOnSuccessListener {
                Log.d("FirebaseReservationManager", "Reserva actualizada correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseReservationManager", "Error actualizando reserva: ${e.message}", e)
            }
    }
    
    /**
     * Elimina una reserva
     * @param reservationId ID de la reserva
     * @return Task con resultado de la operación
     */
    fun deleteReservation(reservationId: String): Task<Unit> {
        return firestore.collection("reservations")
            .document(reservationId)
            .delete()
            .addOnSuccessListener {
                Log.d("FirebaseReservationManager", "Reserva eliminada correctamente")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseReservationManager", "Error eliminando reserva: ${e.message}", e)
            }
    }
    
    /**
     * Escucha cambios en las reservas en tiempo real
     * @param onReservationsUpdated Callback que se llama cuando hay cambios en las reservas
     * @return ListenerRegistration para detener la escucha cuando sea necesario
     */
    private val listeners = mutableListOf<ListenerRegistration>()
    
    fun listenToReservations(onReservationsUpdated: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        val listener = firestore.collection("reservations")
            .orderBy("fecha", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseReservationManager", "Error escuchando reservas: ${error.message}", error)
                    onReservationsUpdated(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val reservations = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data
                            if (data != null) {
                                // Asegurarse de que el ID del documento esté incluido en los datos
                                (data as MutableMap<String, Any>)["id"] = doc.id
                            }
                            data
                        } catch (e: Exception) {
                            Log.e("FirebaseReservationManager", "Error mapeando reserva: ${e.message}", e)
                            null
                        }
                    }
                    Log.d("FirebaseReservationManager", "Actualizadas ${reservations.size} reservas")
                    onReservationsUpdated(reservations)
                }
            }
        
        listeners.add(listener)
        return listener
    }
    
    fun removeListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }
    
    /**
     * Obtiene reservas para una fecha específica
     * @param date Fecha para filtrar
     * @param onReservationsUpdated Callback con las reservas
     * @return ListenerRegistration para detener la escucha
     */
    fun listenToReservationsByDate(date: Date, onReservationsUpdated: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        // Crear límites para el día (inicio y fin)
        val startOfDay = Date(date.year, date.month, date.date, 0, 0, 0)
        val endOfDay = Date(date.year, date.month, date.date, 23, 59, 59)
        
        return firestore.collection("reservations")
                .whereGreaterThanOrEqualTo("fecha", startOfDay)
                .whereLessThanOrEqualTo("fecha", endOfDay)
                .orderBy("fecha", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseReservationManager", "Error escuchando reservas por fecha: ${error.message}", error)
                        onReservationsUpdated(emptyList())
                        return@addSnapshotListener
                    }
                    
                    if (snapshot != null) {
                        val reservations = snapshot.documents.mapNotNull { doc ->
                            try {
                                val data = doc.data
                                if (data != null) {
                                    (data as MutableMap<String, Any>)["id"] = doc.id
                                }
                                data
                            } catch (e: Exception) {
                                Log.e("FirebaseReservationManager", "Error mapeando reserva por fecha: ${e.message}", e)
                                null
                            }
                        }
                        Log.d("FirebaseReservationManager", "Actualizadas ${reservations.size} reservas para la fecha seleccionada")
                        onReservationsUpdated(reservations)
                    }
                }
    }
    
    /**
     * Obtiene una reserva específica por ID de mesa
     * @param tableId ID de la mesa
     * @param callback Callback con la reserva encontrada, o null si no existe
     */
    fun getReservationByTableId(tableId: Int, callback: (Map<String, Any>?) -> Unit) {
        firestore.collection("reservations")
            .whereEqualTo("tableId", tableId)
            .whereNotEqualTo("estado", "CANCELADA") // Excluir reservas canceladas
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.d("FirebaseReservationManager", "No se encontró reserva para la mesa $tableId")
                    callback(null)
                    return@addOnSuccessListener
                }
                
                // Tomar la primera reserva activa para esa mesa
                val reservation = snapshot.documents.firstOrNull()?.data
                if (reservation != null) {
                    // Asegurarse de que el ID del documento esté incluido en los datos
                    (reservation as MutableMap<String, Any>)["id"] = snapshot.documents.first().id
                    Log.d("FirebaseReservationManager", "Reserva encontrada para mesa $tableId: ${reservation["nombreCliente"]}")
                }
                callback(reservation)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseReservationManager", "Error buscando reserva por mesa: ${e.message}", e)
                callback(null)
            }
    }
}
