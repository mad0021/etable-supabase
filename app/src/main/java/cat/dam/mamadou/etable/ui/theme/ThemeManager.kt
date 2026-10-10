package cat.dam.mamadou.etable.ui.theme

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

// Cambiar la enumeración AppTheme:
enum class AppTheme(
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val textColor: Color
) {
    LIGHT(
        primaryColor = Color(0xFF4CAF50),    // Verde
        secondaryColor = Color(0xFF8BC34A),  // Verde claro
        backgroundColor = Color(0xFFF5F5F5), // Gris claro
        textColor = Color(0xFF333333)        // Gris oscuro
    ),
    
    DARK(
        primaryColor = Color(0xFF388E3C),    // Verde oscuro
        secondaryColor = Color(0xFF689F38),  // Verde claro oscuro
        backgroundColor = Color(0xFF121212), // Fondo oscuro
        textColor = Color(0xFFEEEEEE)        // Texto claro
    ),
    
    OCEAN(
        primaryColor = Color(0xFF0277BD),    // Azul profundo
        secondaryColor = Color(0xFF4FC3F7),  // Azul claro
        backgroundColor = Color(0xFFE1F5FE), // Fondo azul muy claro
        textColor = Color(0xFF01579B)        // Texto azul oscuro
    ),
    
    SUNSET(
        primaryColor = Color(0xFFE64A19),    // Naranja oscuro
        secondaryColor = Color(0xFFFFB74D),  // Ámbar claro
        backgroundColor = Color(0xFFFBE9E7), // Fondo rojo muy claro
        textColor = Color(0xFFBF360C)        // Texto naranja oscuro
    ),
    
    NATURE(
        primaryColor = Color(0xFF689F38),    // Verde oliva
        secondaryColor = Color(0xFFAED581),  // Verde lima claro
        backgroundColor = Color(0xFFF1F8E9), // Fondo verde muy claro
        textColor = Color(0xFF33691E)        // Texto verde oscuro
    ),
    
    PURPLE(
        primaryColor = Color(0xFF7B1FA2),    // Púrpura
        secondaryColor = Color(0xFFCE93D8),  // Lavanda
        backgroundColor = Color(0xFFF3E5F5), // Fondo púrpura muy claro
        textColor = Color(0xFF4A148C)        // Texto púrpura oscuro
    ),
    
    MONOCHROME(
        primaryColor = Color(0xFF424242),    // Gris oscuro
        secondaryColor = Color(0xFF9E9E9E),  // Gris
        backgroundColor = Color(0xFFFAFAFA), // Fondo casi blanco
        textColor = Color(0xFF212121)        // Texto casi negro
    ),
    
    AUTUMN(
        primaryColor = Color(0xFFE65100),    // Naranja otoñal
        secondaryColor = Color(0xFFFFA726),  // Ámbar
        backgroundColor = Color(0xFFFFF8E1), // Fondo crema claro
        textColor = Color(0xFF5D4037)        // Texto marrón
    ),
    
    VINTAGE(
        primaryColor = Color(0xFF795548),    // Marrón vintage
        secondaryColor = Color(0xFFBCAAA4),  // Beige
        backgroundColor = Color(0xFFF5F5DC), // Fondo beige claro
        textColor = Color(0xFF4E342E)        // Texto marrón oscuro
    ),
    
    FIRE(
        primaryColor = Color(0xFFD50000),    // Rojo fuego
        secondaryColor = Color(0xFFFF5722),  // Naranja quemado
        backgroundColor = Color(0xFFFFF3E0), // Fondo naranja muy claro
        textColor = Color(0xFFB71C1C)        // Texto rojo oscuro
    ),
    
    MIDNIGHT(
        primaryColor = Color(0xFF1A237E),    // Azul medianoche
        secondaryColor = Color(0xFF3949AB),  // Azul índigo
        backgroundColor = Color(0xFF121212), // Fondo negro
        textColor = Color(0xFFE0E0E0)        // Texto gris claro
    ),
    
    FOREST(
        primaryColor = Color(0xFF1B5E20),    // Verde bosque
        secondaryColor = Color(0xFF4CAF50),  // Verde
        backgroundColor = Color(0xFFE8F5E9), // Fondo verde muy claro
        textColor = Color(0xFF1B5E20)        // Texto verde oscuro
    );
    
    companion object {
        fun fromString(value: String): AppTheme {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                LIGHT // Predeterminado a tema LIGHT si la cadena no coincide
            }
        }
    }
}

object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val THEME_PREFERENCES = "theme_role_prefs"
    
    // Constants for role-specific theme preferences
    private const val ADMIN_THEME_PREF = "admin_theme_pref"
    private const val ADMIN_DARK_MODE_PREF = "admin_dark_mode_pref"
    private const val WAITER_THEME_PREF = "waiter_theme_pref"
    private const val WAITER_DARK_MODE_PREF = "waiter_dark_mode_pref"
    
    private const val TAG = "ThemeManager"
        
    // Observable para cambios de tema
    private val themeChangeSubject = MutableStateFlow<Pair<AppTheme, Boolean>?>(null)
    
    // Flow observable para cambios de tema
    val themeChanges = themeChangeSubject.filterNotNull()
    
    /**
     * Notifica que se ha cambiado el tema
     */
    fun notifyThemeChanged(theme: AppTheme, isDark: Boolean) {
        themeChangeSubject.value = Pair(theme, isDark)
        Log.d(TAG, "⚡ TEMA CAMBIADO: $theme, isDark: $isDark")
    }

    // Reemplazar completamente la función saveTheme:
    fun saveTheme(context: Context, theme: AppTheme, isDarkMode: Boolean) {
        Log.d("ThemeManager", "⚡ GUARDANDO TEMA: $theme, isDarkMode: $isDarkMode")
        
        // Determinar si el contexto es de una actividad de admin o camarero
        val isAdmin = context.javaClass.simpleName.contains("Admin")
        
        // IMPORTANTE: UTILIZAR UNA ÚNICA PREFERENCIA PARA EVITAR CONFLICTOS
        val sharedPreferences = context.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE)
        
        // Guardar el tema según el rol
        if (isAdmin) {
            sharedPreferences.edit()
                .putString(ADMIN_THEME_PREF, theme.name)
                .putBoolean(ADMIN_DARK_MODE_PREF, isDarkMode)
                .apply()
            Log.d("ThemeManager", "⚡ Tema ADMIN guardado: $theme")
        } else {
            sharedPreferences.edit()
                .putString(WAITER_THEME_PREF, theme.name)
                .putBoolean(WAITER_DARK_MODE_PREF, isDarkMode)
                .apply()
            Log.d("ThemeManager", "⚡ Tema WAITER guardado: $theme")
        }
        
        // NO guardar en las preferencias generales para evitar conflictos
    }

    // Reemplazar la función getTheme para ser más robusta
    fun getTheme(context: Context): AppTheme {
        // Determinar si el contexto es de admin o camarero
        val isAdmin = context.javaClass.simpleName.contains("Admin")
        val prefName = if (isAdmin) ADMIN_THEME_PREF else WAITER_THEME_PREF
        
        // IMPORTANTE: Usar únicamente las preferencias específicas del rol
        val sharedPreferences = context.getSharedPreferences(THEME_PREFERENCES, Context.MODE_PRIVATE)
        val themeName = sharedPreferences.getString(prefName, AppTheme.LIGHT.name)
        
        Log.d("ThemeManager", "⚡ LEYENDO TEMA para ${if (isAdmin) "ADMIN" else "WAITER"}: $themeName")
        
        return try {
            AppTheme.valueOf(themeName ?: AppTheme.LIGHT.name)
        } catch (e: IllegalArgumentException) {
            Log.e("ThemeManager", "Error al leer tema, usando LIGHT como respaldo", e)
            AppTheme.LIGHT
        }
    }

    // Reemplazar la función applyTheme para ser más directa y simple
    fun applyTheme(context: Context, theme: AppTheme, isDarkMode: Boolean) {
        Log.d("ThemeManager", "⚡ APLICANDO TEMA: $theme, isDarkMode: $isDarkMode")
        
        // 1. PRIMERO guardar el tema
        saveTheme(context, theme, isDarkMode)
        
        // 2. Establecer el modo noche según corresponda
        val nightMode = when (theme) {
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            else -> if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
        // 3. IMPORTANTE: Recrear actividad después de un retraso significativo
        if (context is Activity) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    Log.d("ThemeManager", "⚡ Recreando actividad con tema: $theme")
                    context.recreate()
                } catch (e: Exception) {
                    Log.e("ThemeManager", "Error al recrear actividad", e)
                }
            }, 300) // Retraso más largo para asegurar que las preferencias se guarden completamente
        }
    }

    // Asegurate de que este método exista:
    fun isDarkMode(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE)
        val isAdmin = context.javaClass.simpleName.contains("Admin")
        return if (isAdmin) {
            sharedPreferences.getBoolean("admin_dark_mode_pref", false)
        } else {
            sharedPreferences.getBoolean("waiter_dark_mode_pref", false)
        }
    }
}


/**
 * Determina si el sistema está en modo oscuro sin usar funciones @Composable
 */
fun isSystemInDarkTheme(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return when (uiModeManager.nightMode) {
        UiModeManager.MODE_NIGHT_YES -> true
        else -> false
    }
}