package cat.dam.mamadou.etable.ui.theme

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import cat.dam.mamadou.etable.R

/**
 * Controlador global de temas que usa un patrón observable
 * para mantener el tema sincronizado en toda la aplicación
 */
object ThemeController {
    private const val TAG = "ThemeController"
    // Estado global observable para el tema actual
    private val _currentTheme = MutableLiveData<AppTheme>(AppTheme.LIGHT)
    val currentTheme: LiveData<AppTheme> = _currentTheme

    // Estado global para el modo oscuro
    private val _isDarkMode = MutableLiveData<Boolean>(false)
    val isDarkMode: LiveData<Boolean> = _isDarkMode

    // Estado Compose para acceso directo en composables
    val themeState = mutableStateOf<AppTheme>(AppTheme.LIGHT)
    val darkModeState = mutableStateOf(false)

    // Preferencias constantes
    private const val PREFS_NAME = "theme_preferences"
    private const val KEY_ADMIN_THEME = "admin_theme"
    private const val KEY_WAITER_THEME = "waiter_theme"
    private const val KEY_ADMIN_DARK_MODE = "admin_dark_mode"
    private const val KEY_WAITER_DARK_MODE = "waiter_dark_mode"

    /**
     * Inicializar el controlador - llamar en Application.onCreate()
     */
    fun initialize(context: Context) {
        Log.d(TAG, "🚀 Inicializando con tema: ${ThemeManager.getTheme(context)}, isDark: ${ThemeManager.isDarkMode(context)}")
        val isAdmin = false // Por defecto, inicializar con modo camarero
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        val themeName = if (isAdmin) {
            prefs.getString(KEY_ADMIN_THEME, AppTheme.LIGHT.name)
        } else {
            prefs.getString(KEY_WAITER_THEME, AppTheme.LIGHT.name)
        }
        
        val isDark = if (isAdmin) {
            prefs.getBoolean(KEY_ADMIN_DARK_MODE, false)
        } else {
            prefs.getBoolean(KEY_WAITER_DARK_MODE, false)
        }
        
        try {
            val theme = themeName?.let { AppTheme.valueOf(it) } ?: AppTheme.LIGHT
            Log.d("ThemeController", "🌈 Inicializando con tema: $theme, isDark: $isDark")
            
            // Actualizar estados
            _currentTheme.value = theme
            _isDarkMode.value = isDark
            themeState.value = theme
            darkModeState.value = isDark
        } catch (e: Exception) {
            Log.e("ThemeController", "Error al inicializar tema", e)
            // Usar valores por defecto
            _currentTheme.value = AppTheme.LIGHT
            _isDarkMode.value = false
            themeState.value = AppTheme.LIGHT
            darkModeState.value = false
        }
    }

    private var isChangingTheme = false
    private var pendingThemeChange: Triple<Context, AppTheme, Boolean>? = null

    /**
     * Cambiar el tema actual
     */
    fun setTheme(context: Context, theme: AppTheme, isDark: Boolean, isAdmin: Boolean = false) {
        if (isChangingTheme) {
            // Si ya hay un cambio en progreso, guarda este como pendiente
            Log.d(TAG, "⏳ Cambio de tema en progreso, encolando solicitud: $theme")
            pendingThemeChange = Triple(context, theme, isDark)
            return
        }
        
        isChangingTheme = true
        Log.d(TAG, "🌈 Cambiando tema a: $theme, isDark: $isDark, isAdmin: $isAdmin")
        
        // Guardar el tema sin recrear la actividad todavía
        ThemeManager.saveTheme(context, theme, isDark)
        
        if (context is Activity) {
            try {
                // Crear un overlay para la transición suave
                val fadeFrameLayout = FrameLayout(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(context.getColor(android.R.color.white))
                    alpha = 0f
                }
                
                val rootView = context.window.decorView.rootView as? FrameLayout
                rootView?.addView(fadeFrameLayout)
                
                // Animación de fade out
                val fadeOut = AlphaAnimation(0f, 0.5f)
                fadeOut.duration = 200
                fadeOut.fillAfter = true
                
                fadeOut.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    
                    override fun onAnimationEnd(animation: Animation?) {
                        // Aplicar cambio de tema sin animación de sistema
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                // Recrear la actividad sin animación
                                val intent = context.intent
                                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                context.finish()
                                context.overridePendingTransition(0, 0)
                                context.startActivity(intent)
                                context.overridePendingTransition(0, 0)
                                
                                // Resetear el estado después de que se complete el cambio
                                Handler(Looper.getMainLooper()).postDelayed({
                                    isChangingTheme = false
                                    
                                    // Si hay un cambio pendiente, procesarlo
                                    pendingThemeChange?.let { (pendingContext, pendingTheme, pendingIsDark) ->
                                        pendingThemeChange = null
                                        setTheme(pendingContext, pendingTheme, pendingIsDark, isAdmin)
                                    }
                                }, 300)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al recrear actividad: ${e.message}")
                                context.recreate()
                                isChangingTheme = false
                            }
                        }, 100)
                    }
                    
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
                
                fadeFrameLayout.startAnimation(fadeOut)
            } catch (e: Exception) {
                // En caso de error, usar el enfoque tradicional
                Log.e(TAG, "Error en animación: ${e.message}")
                context.recreate()
                isChangingTheme = false
            }
        } else {
            // Si no es una actividad, simplemente marcar como terminado
            isChangingTheme = false
        }
    }

    /**
     * Aplica un tema sin recrear la actividad
     */
    fun applyThemeWithoutRecreation(context: Context, theme: AppTheme, isDark: Boolean, isAdmin: Boolean) {
        Log.d(TAG, "🌈 Cambiando tema a: $theme, isDark: $isDark, isAdmin: $isAdmin")
        
        // Guardar el tema en preferencias
        ThemeManager.saveTheme(context, theme, isDark)
        
        // No llamamos a recreateActivity(), solo guardamos la configuración
        // El cambio se reflejará gracias al estado en ETableThemeWrapper
        
        // Emitir evento de cambio de tema para que otros componentes reaccionen
        ThemeManager.notifyThemeChanged(theme, isDark)
    }
}