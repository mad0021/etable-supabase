package cat.dam.mamadou.etable.utils

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat.recreate
import java.util.*

/**
 * Administrador de idiomas para mantener persistencia y aplicar cambios
 */
object LocaleManager {
    // Constantes existentes
    const val PREFS_LANGUAGE = "app_language_preferences"
    const val KEY_SELECTED_LANGUAGE = "selected_language"
    const val KEY_SELECTED_COUNTRY = "selected_country"
    
    // Agregar TAG para logs
    private const val TAG = "LocaleManager"
    
    /**
     * Obtiene el locale guardado o el locale por defecto del sistema
     */
    fun getLocale(context: Context): Locale {
        val prefs = context.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_SELECTED_LANGUAGE, null)
        val country = prefs.getString(KEY_SELECTED_COUNTRY, null)
        
        return if (language != null) {
            if (country != null) Locale(language, country) else Locale(language)
        } else {
            // Si no hay idioma guardado, usar el idioma actual del sistema
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales[0]
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale
            }
        }
    }
    
    /**
     * Guarda y aplica el idioma seleccionado
     */
    fun setLocale(context: Context, languageCode: String): Context {
        return setLocale(context, Locale(languageCode))
    }
    
    /**
     * Guarda y aplica el idioma seleccionado con un Locale completo
     */
    fun setLocale(context: Context, locale: Locale): Context {
        Log.d(TAG, "🌍 Estableciendo idioma: ${locale.language}_${locale.country}")
        
        // Guardar en preferencias
        saveLocale(context, locale)
        
        // NUEVO: Sincronizar TTS con el cambio de idioma
        syncTTSLanguage(context, locale)
        
        // Aplicar el idioma
        return updateResources(context, locale)
    }
    
    /**
     * NUEVO: Sincroniza el idioma del TTS con el cambio de idioma de la aplicación
     */
    private fun syncTTSLanguage(context: Context, locale: Locale) {
        try {
            val ttsManager = TTSManager.getInstance(context)
            ttsManager.updateLanguage(locale)
            Log.d(TAG, "✅ TTS sincronizado con idioma: ${locale.language}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sincronizando TTS: ${e.message}", e)
        }
    }
    
    /**
     * Actualiza los recursos con el nuevo idioma
     * Ahora es público para que pueda ser accedido desde otras clases
     */
    fun updateResources(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Guarda el idioma seleccionado en preferencias
     */
    private fun saveLocale(context: Context, locale: Locale) {
        val prefs = context.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SELECTED_LANGUAGE, locale.language)
            putString(KEY_SELECTED_COUNTRY, locale.country)
            apply()
        }
        Log.d(TAG, "💾 Idioma guardado: ${locale.language}_${locale.country}")
    }

    /**
     * Obtiene el idioma guardado en preferencias
     */
    fun getPersistedLocale(context: Context): Locale {
        val prefs = context.getSharedPreferences(PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_SELECTED_LANGUAGE, "ca") ?: "ca"
        val country = prefs.getString(KEY_SELECTED_COUNTRY, "ES") ?: "ES"
        
        val locale = Locale(language, country)
        Log.d(TAG, "📖 Idioma recuperado: ${locale.language}_${locale.country}")
        return locale
    }

    /**
     * Obtiene el idioma actual del sistema
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }

    /**
     * Recrea la actividad para aplicar el cambio de idioma
     */
    fun recreateActivity(context: Context) {
        if (context is Activity) {
            Log.d(TAG, "🔄 Recreando actividad para aplicar cambio de idioma")
            recreate(context)
        }
    }

    /**
     * Verifica si el idioma actual es diferente al guardado
     */
    fun isLanguageChanged(context: Context): Boolean {
        val currentLocale = getCurrentLocale(context)
        val savedLocale = getPersistedLocale(context)
        
        val changed = currentLocale.language != savedLocale.language
        Log.d(TAG, "🔍 ¿Idioma cambiado? $changed (actual: ${currentLocale.language}, guardado: ${savedLocale.language})")
        return changed
    }
    
    /**
     * NUEVO: Inicializa la sincronización del TTS al arrancar la aplicación
     */
    fun initializeTTSSync(context: Context) {
        try {
            val currentLocale = getCurrentLocale(context)
            val ttsManager = TTSManager.getInstance(context)
            ttsManager.synchronizeWithAppLanguage()
            Log.d(TAG, "🔄 TTS inicializado para idioma: ${currentLocale.language}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error inicializando sincronización TTS: ${e.message}", e)
        }
    }
}