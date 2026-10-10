package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Manager class for break-related operations in Firestore
 */
class FirebaseBreakManager(private val firestore: FirebaseFirestore) {
    
    /**
     * Sends a break request to Firestore
     * @param breakRequest The break request to send
     * @return Result with success or failure
     */
    suspend fun sendBreakRequest(breakRequest: BreakRequest): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Verificar que userName no esté vacío
                val finalUserName = if (breakRequest.userName.isBlank()) {
                    Log.w("FirebaseBreakManager", "userName está vacío, usando valor por defecto")
                    "Usuario"
                } else {
                    breakRequest.userName
                }
                
                // Prepare data for Firestore con userName verificado
                val breakData = mapOf(
                    "id" to breakRequest.id,
                    "userId" to breakRequest.userId,
                    "userName" to finalUserName,
                    "fullName" to breakRequest.fullName,
                    "content" to breakRequest.content,
                    "reason" to breakRequest.reason,
                    "timestamp" to breakRequest.timestamp,
                    "status" to breakRequest.status,
                    "viewed" to breakRequest.viewed
                )
                
                Log.d("FirebaseBreakManager", "Guardando solicitud con userName: $finalUserName")
                
                // Save to Firestore
                firestore.collection("break_requests")
                    .document(breakRequest.id)
                    .set(breakData)
                    .await()
                
                Log.d("FirebaseBreakManager", "Break request sent successfully")
                Result.success(true)
            } catch (e: Exception) {
                Log.e("FirebaseBreakManager", "Error sending break request: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Gets all break requests from Firestore
     * @param onComplete Callback with the list of break requests
     */
    fun getBreakRequests(onComplete: (List<BreakRequest>) -> Unit) {
        firestore.collection("break_requests")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                val requests = snapshot.documents.mapNotNull { doc ->
                    try {
                        val id = doc.getString("id") ?: doc.id
                        val userId = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: ""
                        val content = doc.getString("content") ?: ""
                        val reason = doc.getString("reason") ?: ""
                        val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                        val viewed = doc.getBoolean("viewed") ?: false
                        val status = doc.getString("status") ?: "pending"
                        
                        BreakRequest(
                            id = id,
                            userId = userId,
                            userName = userName,
                            content = content,
                            reason = reason,
                            timestamp = timestamp,
                            viewed = viewed,
                            status = status
                        )
                    } catch (e: Exception) {
                        Log.e("FirebaseBreakManager", "Error parsing break request: ${e.message}")
                        null
                    }
                }
                onComplete(requests)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseBreakManager", "Error getting break requests: ${e.message}")
                onComplete(emptyList())
            }
    }
    
    /**
     * Sets up a listener for real-time break request updates
     * @param onUpdate Callback that executes when changes occur
     * @return Listener registration to cancel the listener
     */
    fun listenForBreakRequests(onUpdate: (List<BreakRequest>) -> Unit): ListenerRegistration {
        return firestore.collection("break_requests")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseBreakManager", "Error listening for break requests: ${error.message}")
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val requests = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val userId = doc.getString("userId") ?: ""
                            val userName = doc.getString("userName") ?: ""
                            val content = doc.getString("content") ?: "Solicitud de descanso"
                            val reason = doc.getString("reason") ?: ""
                            val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: Date()
                            val viewed = doc.getBoolean("viewed") ?: false
                            val status = doc.getString("status") ?: "pending"
                            
                            // Depuración para identificar problemas con userName
                            Log.d("FirebaseBreakManager", "Procesando solicitud: id=$id, userName=$userName")
                            
                            BreakRequest(
                                id = id,
                                userId = userId,
                                userName = userName.ifEmpty { "Usuario" }, // Asegurar que nunca esté vacío
                                content = content,
                                reason = reason,
                                timestamp = timestamp,
                                viewed = viewed,
                                status = status
                            )
                        } catch (e: Exception) {
                            Log.e("FirebaseBreakManager", "Error parsing break request: ${e.message}")
                            null
                        }
                    }
                    onUpdate(requests)
                }
            }
    }
    
    /**
     * Updates the status of a break request
     * @param requestId ID of the break request to update
     * @param status New status (approved, rejected, pending)
     * @param onComplete Callback with the result
     */
    fun updateBreakRequestStatus(requestId: String, status: String, onComplete: (Boolean) -> Unit) {
        firestore.collection("break_requests")
            .document(requestId)
            .update("status", status)
            .addOnSuccessListener {
                Log.d("FirebaseBreakManager", "Break request status updated to $status")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseBreakManager", "Error updating break request status: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Marks a break request as viewed
     * @param requestId ID of the break request to mark as viewed
     * @param onComplete Callback with the result
     */
    fun markBreakRequestAsViewed(requestId: String, onComplete: (Boolean) -> Unit) {
        firestore.collection("break_requests")
            .document(requestId)
            .update("viewed", true)
            .addOnSuccessListener {
                Log.d("FirebaseBreakManager", "Break request marked as viewed")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseBreakManager", "Error marking break request as viewed: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Obtiene la última solicitud de descanso pendiente y no vista
     * @param callback Función que recibe la solicitud o null si no hay ninguna
     */
    fun getLatestBreakRequest(callback: (BreakRequest?) -> Unit) {
        firestore.collection("break_requests")
            .whereEqualTo("viewed", false)  // Solo solicitudes no vistas
            .whereEqualTo("status", "pending")
            .orderBy("timestamp", cat.dam.mamadou.etable.supabase.Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    try {
                        val id = document.getString("id") ?: document.id
                        val userId = document.getString("userId") ?: ""
                        val userName = document.getString("userName") ?: ""
                        val content = document.getString("content") ?: ""
                        val reason = document.getString("reason") ?: ""
                        val timestamp = document.getTimestamp("timestamp")?.toDate() ?: Date()
                        val viewed = document.getBoolean("viewed") ?: false
                        val status = document.getString("status") ?: "pending"
                        
                        val breakRequest = BreakRequest(
                            id = id,
                            userId = userId,
                            userName = userName,
                            content = content,
                            reason = reason,
                            timestamp = timestamp,
                            viewed = viewed,
                            status = status
                        )
                        
                        callback(breakRequest)
                    } catch (e: Exception) {
                        Log.e("FirebaseBreakManager", "Error al parsear la solicitud: ${e.message}")
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseBreakManager", "Error obteniendo solicitud: ${e.message}", e)
                callback(null)
            }
    }
}