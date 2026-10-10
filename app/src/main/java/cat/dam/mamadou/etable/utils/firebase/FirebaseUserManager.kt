package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import cat.dam.mamadou.etable.supabase.FirebaseAuth
import cat.dam.mamadou.etable.supabase.FirebaseUser
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext
import java.util.Date
import cat.dam.mamadou.etable.models.User

/**
 * Manager class for user-related operations in Firestore
 */
class FirebaseUserManager(private val firestore: FirebaseFirestore) {

    private val listeners = mutableListOf<ListenerRegistration>()

    /**
     * Updates an existing user in Firestore
     * @param userId ID of the user to update
     * @param userData New user data
     * @return Result with success or failure
     */
    suspend fun updateUser(userId: String, userData: Map<String, Any>): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Intentar actualizar en ambas colecciones para compatibilidad
                try {
                    // Primero intentar en la colección "user"
                    firestore.collection("user").document(userId)
                        .update(userData)
                        .await()

                    Log.d("FirebaseUserManager", "Usuario actualizado en colección 'user'")
                    return@withContext Result.success(true)
                } catch (e: Exception) {
                    Log.e("FirebaseUserManager", "Error actualizando en colección 'user': ${e.message}")
                    // Si falla, intentar en "users"
                    firestore.collection("users").document(userId)
                        .update(userData)
                        .await()
                    Log.d("FirebaseUserManager", "Usuario actualizado en colección 'users'")
                    return@withContext Result.success(true)
                }
            } catch (e: Exception) {
                Log.e("FirebaseUserManager", "Error actualizando usuario: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Listen to changes in users collection
     * @param callback Callback with list of users
     * @return ListenerRegistration to remove the listener
     */
    fun listenToUsers(callback: (List<Map<String, Any>>) -> Unit): ListenerRegistration {
        val listener = firestore.collection("user")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseUserManager", "Error listening to users: ${error.message}")
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["id"] = doc.id

                    // Obtener los temas permitidos como lista
                    val allowedThemes = data["allowedThemes"] as? List<String> ?: emptyList()

                    // Agregar los datos del usuario a la lista
                    data
                } ?: emptyList()

                callback(users)
            }

        listeners.add(listener)
        return listener
    }

    fun removeListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
    }

    /**
     * Actualiza las configuraciones del usuario
     * @param userId ID del usuario
     * @param configData Datos de configuración a actualizar
     * @return Result con éxito o fracaso
     */
    suspend fun updateUserConfig(userId: String, configData: Map<String, Any>): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Asegurar que los tipos de datos son correctos para Firestore
                val processedConfigData = configData.mapValues { (key, value) ->
                    // Convertir listas a ArrayList para Firestore
                    if ((key == "allowedThemes" || key == "allowedLanguages") && value is List<*>) {
                        ArrayList(value)
                    } else {
                        value
                    }
                }

                Log.d("FirebaseUserManager", "Actualizando configuración para usuario $userId: $processedConfigData")
                
                // Actualizar directamente en el documento raíz
                firestore.collection("user").document(userId)
                    .update(processedConfigData)
                    .await()
                
                Log.d("FirebaseUserManager", "✅ Configuración actualizada correctamente para usuario $userId")
                
                Result.success(true)
            } catch (e: Exception) {
                Log.e("FirebaseUserManager", "❌ Error actualizando config: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene las configuraciones de un usuario
     * @param userId ID del usuario
     * @return Result con los datos de configuración si es exitoso, o un error
     */
    suspend fun getUserConfig(userId: String): Result<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            try {
                // Intentar obtener de la colección "user"
                val userDoc = firestore.collection("user").document(userId).get().await()

                if (userDoc.exists()) {
                    val data = userDoc.data ?: emptyMap()
                    return@withContext Result.success(data)
                }

                // Si no existe en "user", intentar en "users"
                val usersDoc = firestore.collection("users").document(userId).get().await()

                if (usersDoc.exists()) {
                    val data = usersDoc.data ?: emptyMap()
                    return@withContext Result.success(data)
                }

                // Si no hay configuración, devolver un mapa vacío
                return@withContext Result.success(emptyMap())
            } catch (e: Exception) {
                Log.e("FirebaseUserManager", "Error obteniendo configuración de usuario: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Creates a new user with Firebase Auth and Firestore
     */
    suspend fun createUser(
        email: String,
        password: String,
        fullName: String,
        role: String
    ): Result<User> {
        val auth = FirebaseAuth.getInstance()
        
        return try {
            // Crea la cuenta en Supabase Auth sin cerrar la sesión del admin (RPC admin_create_user)
            val authResult = auth.createUserWithEmailAndPassword(email, password, fullName, role).await()
            val userId = authResult.user?.uid ?: throw Exception("Failed to get user ID")
            
            val user = User(
                id = userId,
                username = email.substringBefore("@"),
                fullName = fullName,
                email = email,
                role = role,
                isActive = true,
                darkThemeEnabled = false,
                textSize = 16f,
                language = "es",
                textToSpeechEnabled = false,
                accessibilityEnabled = false,
                canRequestBreak = true,
                canChangeTheme = true,
                canLogout = true,
                canChangeLanguage = true,
                canUseTTS = true,
                canChangeTextSize = true,
                allowedThemes = emptyList(),
                allowedLanguages = emptyList() // NUEVO: Campo para idiomas permitidos
            )
            
            // Convertir User a Map para guardar en Firestore
            val userData = mapOf(
                "id" to user.id,
                "username" to user.username,
                "fullName" to user.fullName,
                "email" to user.email,
                "role" to user.role,
                "isActive" to user.isActive,
                "darkThemeEnabled" to user.darkThemeEnabled,
                "textSize" to user.textSize,
                "language" to user.language,
                "textToSpeechEnabled" to user.textToSpeechEnabled,
                "accessibilityEnabled" to user.accessibilityEnabled,
                "canRequestBreak" to user.canRequestBreak,
                "canChangeTheme" to user.canChangeTheme,
                "canLogout" to user.canLogout,
                "canChangeLanguage" to user.canChangeLanguage,
                "canUseTTS" to user.canUseTTS,
                "canChangeTextSize" to user.canChangeTextSize,
                "allowedThemes" to user.allowedThemes,
                "allowedLanguages" to user.allowedLanguages // NUEVO: Incluir en userData
            )
            
            // El documento ya lo crea la base de datos; aquí solo se completan los campos
            firestore.collection("user").document(userId).update(userData).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes a user from Firestore and optionally from Auth
     * @param userId ID of the user to delete
     * @return Result with success or failure
     */
    suspend fun deleteUser(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Borra la cuenta de Supabase Auth y su documento (RPC admin_delete_user)
                FirebaseAuth.getInstance().deleteUser(userId).await()
                Result.success(true)
            } catch (e: Exception) {
                Log.e("FirebaseUserManager", "Error deleting user: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Obtiene un usuario por su ID y lo convierte al modelo User
     * @param userId ID del usuario
     * @param onComplete Callback con el usuario
     */
    fun getUserById(userId: String, onComplete: (User?) -> Unit) {
        firestore.collection("user").document(userId).get()
            .addOnSuccessListener { document ->
                val data = document.data
                if (data == null) {
                    onComplete(null)
                    return@addOnSuccessListener
                }
                try {
                    onComplete(User.fromMap(userId, data))
                } catch (e: Exception) {
                    Log.e("FirebaseUserManager", "Error al convertir usuario: ${e.message}", e)
                    onComplete(null)
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }
}