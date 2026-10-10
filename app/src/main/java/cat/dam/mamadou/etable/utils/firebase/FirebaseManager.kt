package cat.dam.mamadou.etable.utils.firebase

import android.content.Context
import cat.dam.mamadou.etable.supabase.FirebaseAuth
import cat.dam.mamadou.etable.supabase.FirebaseFirestore

/**
 * Manager class for Firebase operations
 */
class FirebaseManager(private val context: Context? = null) {
    
    // Firebase instances
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Manager instances
    val authManager = FirebaseAuthManager(auth)
    val userManager = FirebaseUserManager(firestore)
    val menuManager = FirebaseMenuManager(firestore)
    val tableManager = FirebaseTableManager(firestore)
    val orderManager = FirebaseOrderManager(firestore)
    val breakManager = FirebaseBreakManager(firestore)
    val reservationManager = FirebaseReservationManager(firestore)

    
    // Getter for Firestore (for backward compatibility)
    fun getFirestore(): FirebaseFirestore {
        return firestore
    }
}
