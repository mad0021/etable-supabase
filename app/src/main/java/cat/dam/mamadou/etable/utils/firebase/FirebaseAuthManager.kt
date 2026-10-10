package cat.dam.mamadou.etable.utils.firebase

import android.content.Context
import android.util.Log
import cat.dam.mamadou.etable.utils.SessionManager
import cat.dam.mamadou.etable.supabase.FirebaseAuth
import cat.dam.mamadou.etable.supabase.FirebaseUser
import kotlinx.coroutines.Dispatchers
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext

class FirebaseAuthManager(private val auth: FirebaseAuth) {
    
    private var sessionManager: SessionManager? = null
    
    /**
     * Inicializa el SessionManager
     * @param context Contexto de la aplicación
     */
    fun initSessionManager(context: Context) {
        if (sessionManager == null) {
            sessionManager = SessionManager(context)
        }
    }
    
    /**
     * Verifica si hay un usuario autenticado y restaura la sesión si es necesario
     * @return true si hay un usuario autenticado, false en caso contrario
     */
    fun checkAuthState(context: Context): Boolean {
        // Inicializar SessionManager si es necesario
        initSessionManager(context)
        
        // Verificar si hay un usuario autenticado en Firebase
        val currentUser = auth.currentUser
        
        // CASO 1: Usuario en Firebase pero no en SessionManager
        if (currentUser != null && !sessionManager!!.isLoggedIn()) {
            // Obtener el rol del usuario desde Firestore y restaurar la sesión
            getUserRole(currentUser.uid) { role ->
                if (role != null) {
                    Log.d("FirebaseAuthManager", "Restaurando sesión para: ${currentUser.email}, rol: $role")
                    sessionManager!!.saveUserSession(
                        currentUser.uid, 
                        currentUser.email ?: "", 
                        role
                    )
                }
            }
            return true
        }
        
        // CASO 2: Usuario en SessionManager pero no en Firebase
        if (sessionManager!!.isLoggedIn() && currentUser == null) {
            Log.d("FirebaseAuthManager", "Sesión local activa pero usuario Firebase null, requiere reautenticación")
            
            // No eliminamos la sesión local, dejamos que el sistema decida si debe reautenticar
            // al usuario en lugar de forzar un cierre de sesión
            return false
        }
        
        // CASO 3: Usuario tanto en Firebase como en SessionManager
        if (currentUser != null && sessionManager!!.isLoggedIn()) {
            // Comprobar que los IDs coinciden
            val sessionUserId = sessionManager!!.getUserId()
            if (sessionUserId != currentUser.uid) {
                Log.w("FirebaseAuthManager", "IDs de usuario no coinciden, actualizando sesión")
                getUserRole(currentUser.uid) { role ->
                    if (role != null) {
                        sessionManager!!.saveUserSession(
                            currentUser.uid, 
                            currentUser.email ?: "", 
                            role
                        )
                    }
                }
            }
            return true
        }
        
        return currentUser != null
    }
    
    /**
     * Obtiene el rol del usuario desde Firestore
     * @param userId ID del usuario
     * @param callback Callback con el rol del usuario
     */
    private fun getUserRole(userId: String, callback: (String?) -> Unit) {
        // Implementar la lógica para obtener el rol del usuario desde Firestore
        val firestore = cat.dam.mamadou.etable.supabase.FirebaseFirestore.getInstance()
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val role = if (document.getBoolean("isActive") == false) null else document.getString("role")
                callback(role)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
    /**
     * Inicia sesión con email y contraseña (versión no suspendida con callback)
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param role Rol del usuario (opcional)
     * @param callback Callback con resultado de autenticación y error
     */
    fun signInWithEmailAndPassword(
        email: String,
        password: String,
        role: String? = null,
        callback: (authResult: cat.dam.mamadou.etable.supabase.AuthResult?, error: Exception?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("FirebaseAuthManager", "Error en inicio de sesión: ${task.exception?.message}")
                    val msg = task.exception?.message.orEmpty()
                    callback(null, Exception(if ("invalid_credentials" in msg) "Email o contraseña incorrectos" else msg))
                    return@addOnCompleteListener
                }
                val user = task.result.user!!
                // El rol elegido en pantalla debe coincidir con el guardado; antes bastaba con pulsar "Admin"
                getUserRole(user.uid) { realRole ->
                    if (realRole == null || (role != null && realRole != role)) {
                        auth.signOut()
                        callback(null, Exception(if (realRole == null) "Usuario no encontrado o desactivado" else "Este usuario no tiene el rol seleccionado"))
                        return@getUserRole
                    }
                    if (sessionManager != null && role != null) {
                        sessionManager?.saveUserSession(user.uid, user.email ?: "", role)
                    }
                    callback(task.result, null)
                }
            }
    }
    /**
     * Cierra la sesión del usuario actual
     */
    fun signOut() {
        // Limpiar datos de sesión
        sessionManager?.logoutUser()
        auth.signOut()
    }
    
    /**
     * Obtiene el usuario actualmente autenticado
     * @return FirebaseUser si hay sesión iniciada, null en caso contrario
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
    
    /**
     * Obtiene el rol del usuario de la sesión actual
     * @return Rol del usuario o cadena vacía si no hay sesión
     */
    fun getUserRole(): String {
        return sessionManager?.getUserRole() ?: ""
    }
}