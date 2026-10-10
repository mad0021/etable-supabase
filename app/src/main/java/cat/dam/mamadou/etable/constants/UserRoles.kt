package cat.dam.mamadou.etable.constants

import android.content.Context
import cat.dam.mamadou.etable.R

/**
 * Roles de usuario que se usan internamente en la aplicación
 * IMPORTANTE: No traducir estos valores, se usan como identificadores en la BD
 */
object UserRoles {
    const val ADMIN = "admin"
    const val WAITER = "cambrer"
    
    /**
     * Obtiene el string resource para mostrar un rol traducido
     */
    fun getResourceId(role: String): Int {
        return when (role) {
            ADMIN -> R.string.user_role_display_admin
            WAITER -> R.string.user_role_display_waiter
            else -> R.string.user_role_display_waiter
        }
    }
    
    /**
     * Obtiene el nombre traducido de un rol
     */
    fun getLocalizedName(context: Context, role: String): String {
        return context.getString(getResourceId(role))
    }
}