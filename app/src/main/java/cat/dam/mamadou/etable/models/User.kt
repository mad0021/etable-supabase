package cat.dam.mamadou.etable.models

import android.content.Context
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.UserRoles // AÑADIR ESTE IMPORT

/**
 * Modelo de datos para usuarios
 */
data class User(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "cambrer",
    val isActive: Boolean = true,
    val darkThemeEnabled: Boolean = false,
    val textSize: Float = 16f,
    val language: String = "es",
    val textToSpeechEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val canRequestBreak: Boolean = true,
    val canChangeTheme: Boolean = true,
    val canLogout: Boolean = true, 
    val canChangeLanguage: Boolean = true,
    val canUseTTS: Boolean = true,
    val canChangeTextSize: Boolean = true,
    val allowedThemes: List<String> = emptyList(),
    val allowedLanguages: List<String> = emptyList() // ["es", "ca", "en"]
) {
    /**
     * Obtiene el nombre traducido del rol
     */
    fun getLocalizedRole(context: Context): String {
        return UserRoles.getLocalizedName(context, role)
    }
    
    /**
     * Obtiene el estado traducido del usuario
     */
    fun getLocalizedStatus(context: Context): String {
        return if (isActive) 
            context.getString(R.string.user_state_active)
        else 
            context.getString(R.string.user_status_inactive)
    }

    fun getRoleDisplayName(context: Context): String {
        return when (role) {
            "admin" -> context.getString(R.string.user_role_display_admin)
            "cambrer" -> context.getString(R.string.user_role_display_waiter)
            else -> role.uppercase()
        }
    }
    
    fun getStatusDisplayName(context: Context): String {
        return if (isActive) {
            context.getString(R.string.user_state_active)
        } else {
            context.getString(R.string.user_status_inactive)
        }
    }

    companion object {
        /** Construye un User a partir del documento de la colección "user", con valores por defecto si falta algún campo. */
        fun fromMap(id: String, data: Map<String, Any?>): User {
            val email = data["email"] as? String ?: ""
            fun bool(key: String, default: Boolean) = data[key] as? Boolean ?: default
            fun strings(key: String) = when (val v = data[key]) {
                is List<*> -> v.filterIsInstance<String>()
                is Array<*> -> v.filterIsInstance<String>()
                else -> emptyList()
            }
            return User(
                id = id,
                username = data["username"] as? String ?: email.substringBefore("@"),
                fullName = data["fullName"] as? String ?: "",
                email = email,
                role = data["role"] as? String ?: "cambrer",
                isActive = bool("isActive", true),
                darkThemeEnabled = bool("darkThemeEnabled", false),
                textSize = (data["textSize"] as? Number)?.toFloat() ?: 16f,
                language = data["language"] as? String ?: "es",
                textToSpeechEnabled = bool("textToSpeechEnabled", false),
                accessibilityEnabled = bool("accessibilityEnabled", false),
                canRequestBreak = bool("canRequestBreak", true),
                canChangeTheme = bool("canChangeTheme", true),
                canLogout = bool("canLogout", true),
                canChangeLanguage = bool("canChangeLanguage", true),
                canUseTTS = bool("canUseTTS", true),
                canChangeTextSize = bool("canChangeTextSize", true),
                allowedThemes = strings("allowedThemes"),
                allowedLanguages = strings("allowedLanguages")
            )
        }
    }
}