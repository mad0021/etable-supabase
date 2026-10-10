package cat.dam.mamadou.etable.supabase

import cat.dam.mamadou.etable.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

val supabaseClient by lazy {
    createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}

internal lateinit var appContext: android.content.Context

/**
 * Empieza a cargar la sesión guardada en segundo plano. No bloquea: un runBlocking aquí congelaba la app
 * (ANR) cuando la carga necesitaba el hilo principal o la red. La pantalla de arranque espera con awaitSupabaseReady().
 */
fun initSupabase(context: android.content.Context) {
    appContext = context.applicationContext
    ioScope.launch { supabaseClient.auth.awaitInitialization() }
}

/** Espera (sin bloquear el hilo) a que la sesión guardada esté cargada; antes de preguntar por el usuario actual. */
suspend fun awaitSupabaseReady() = supabaseClient.auth.awaitInitialization()

// ───────── API estilo FirebaseAuth ─────────

class FirebaseUser internal constructor(val uid: String, val email: String?) {
    val displayName: String? get() = null
    fun getIdToken(forceRefresh: Boolean): Task<GetTokenResult> = Task {
        if (forceRefresh) supabaseClient.auth.refreshCurrentSession()
        GetTokenResult(supabaseClient.auth.currentAccessTokenOrNull())
    }
}

class GetTokenResult internal constructor(val token: String?)
class AuthResult internal constructor(val user: FirebaseUser?)

class FirebaseAuth private constructor() {
    val currentUser: FirebaseUser?
        get() = supabaseClient.auth.currentUserOrNull()?.let { FirebaseUser(it.id, it.email) }

    fun signInWithEmailAndPassword(email: String, password: String): Task<AuthResult> = Task {
        supabaseClient.auth.signInWith(Email) { this.email = email.trim(); this.password = password }
        AuthResult(currentUser)
    }

    /** Alta hecha por un admin (RPC admin_create_user): no cambia la sesión actual, a diferencia de signUp. */
    fun createUserWithEmailAndPassword(email: String, password: String, fullName: String = "", role: String = "cambrer"): Task<AuthResult> = Task {
        val id = supabaseClient.postgrest.rpc("admin_create_user", buildJsonObject {
            put("p_email", email.trim()); put("p_password", password); put("p_full_name", fullName); put("p_role", role)
        }).data.trim('"')
        AuthResult(FirebaseUser(id, email.trim()))
    }

    fun deleteUser(userId: String): Task<Unit> = Task {
        supabaseClient.postgrest.rpc("admin_delete_user", buildJsonObject { put("p_user_id", userId) })
    }

    // Firebase cierra sesión de forma síncrona; aquí se borra la sesión local al momento
    fun signOut() = runBlocking { supabaseClient.auth.clearSession() }

    companion object {
        private val instance = FirebaseAuth()
        fun getInstance() = instance
    }
}
