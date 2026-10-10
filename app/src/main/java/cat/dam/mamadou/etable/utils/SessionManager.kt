package cat.dam.mamadou.etable.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Gestor de sesiones para mantener la sesión del usuario entre reinicios de la aplicación
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val PREF_NAME = "etable_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val TAG = "SessionManager"
        
        // Instancia única para patrón Singleton
        @Volatile private var instance: SessionManager? = null
        
        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // Usar MODE_PRIVATE para asegurar que los datos persistan incluso si la app se cierra
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedPreferences.edit()
    
    /**
     * Guarda los datos de sesión del usuario
     * @param userId ID del usuario
     * @param email Email del usuario
     * @param role Rol del usuario (admin, cambrer, etc.)
     */
    fun saveUserSession(userId: String, email: String, role: String) {
        try {
            editor.apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_USER_ID, userId)
                putString(KEY_USER_EMAIL, email)
                putString(KEY_USER_ROLE, role.lowercase()) // Asegurar consistencia en minúsculas
                // La sesión (tokens) la guarda supabase-kt; aquí ya no se copia el JWT en texto plano
                remove(KEY_AUTH_TOKEN)

                // Usar commit() para escritura síncrona garantizada
                val success = commit()
                
                if (success) {
                    Log.d(TAG, "Sesión guardada correctamente para: $email, rol: $role")
                } else {
                    Log.e(TAG, "Error al guardar la sesión")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al guardar sesión: ${e.message}", e)
        }
    }
    
    /**
     * Verifica si el usuario tiene una sesión activa
     * @return true si hay una sesión activa, false en caso contrario
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d(TAG, "isLoggedIn: $isLoggedIn")
        return isLoggedIn
    }
    
    /**
     * Obtiene el ID del usuario actual
     * @return ID del usuario o cadena vacía si no hay sesión
     */
    fun getUserId(): String {
        return sharedPreferences.getString(KEY_USER_ID, "") ?: ""
    }
    
    /**
     * Obtiene el email del usuario actual
     * @return Email del usuario o cadena vacía si no hay sesión
     */
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }
    
    /**
     * Obtiene el rol del usuario actual
     * @return Rol del usuario o cadena vacía si no hay sesión
     */
    fun getUserRole(): String {
        return sharedPreferences.getString(KEY_USER_ROLE, "") ?: ""
    }
    
    /**
     * Cierra la sesión del usuario actual
     */
    fun logoutUser() {
        editor.apply {
            clear()
            commit()
        }
        Log.d(TAG, "Sesión cerrada")
    }
}