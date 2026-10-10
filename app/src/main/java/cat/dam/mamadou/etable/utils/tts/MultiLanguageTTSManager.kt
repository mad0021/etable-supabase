package cat.dam.mamadou.etable.utils.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.LocaleManager
import java.util.*

/**
 * Sistema TTS multiidioma simplificado que se adapta automáticamente al idioma de la aplicación
 */
class MultiLanguageTTSManager private constructor(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isEnabled = false
    private var currentLocale: Locale = Locale.getDefault()
    
    // Helpers específicos por idioma
    private lateinit var catalanVoiceHelper: CatalanVoiceHelper
    private lateinit var spanishVoiceHelper: SpanishVoiceHelper
    private lateinit var englishVoiceHelper: EnglishVoiceHelper
    
    companion object {
        private const val TAG = "MultiLanguageTTSManager"
        private const val PREFS_NAME = "multilang_tts_preferences"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        
        @Volatile
        private var INSTANCE: MultiLanguageTTSManager? = null
        
        fun getInstance(context: Context): MultiLanguageTTSManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MultiLanguageTTSManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
        
        fun destroyInstance() {
            INSTANCE?.shutdown()
            INSTANCE = null
        }
    }
    
    init {
        initializeVoiceHelpers()
        loadTTSSettings()
        initializeTTS()
    }
    
    private fun initializeVoiceHelpers() {
        catalanVoiceHelper = CatalanVoiceHelper()
        spanishVoiceHelper = SpanishVoiceHelper()
        englishVoiceHelper = EnglishVoiceHelper()
    }
    
    private fun loadTTSSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean(KEY_TTS_ENABLED, false)
        
        // Usar el idioma actual de la aplicación
        currentLocale = LocaleManager.getCurrentLocale(context)
        
        Log.d(TAG, "🌍 TTS configurado para idioma: ${currentLocale.language}_${currentLocale.country}")
        Log.d(TAG, "📢 TTS habilitado: $isEnabled")
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                setupLanguageForCurrentLocale()
                Log.d(TAG, "✅ TTS inicializado correctamente")
            } else {
                Log.e(TAG, "❌ Error inicializando TTS: $status")
            }
        }
    }
    
    /**
     * Configura el idioma del TTS basado en el locale actual de la aplicación
     */
    private fun setupLanguageForCurrentLocale() {
        val appLocale = LocaleManager.getCurrentLocale(context)
        setupLanguage(appLocale)
    }
    
    /**
     * Configura el idioma específico del TTS
     */
    fun setupLanguage(locale: Locale) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS no inicializado, guardando idioma para configurar después")
            currentLocale = locale
            return
        }
        
        val success = when (locale.language) {
            "ca" -> catalanVoiceHelper.setupBestCatalanVoice(tts!!, context)
            "es" -> spanishVoiceHelper.setupBestSpanishVoice(tts!!, context)
            "en" -> englishVoiceHelper.setupBestEnglishVoice(tts!!, context)
            else -> englishVoiceHelper.setupBestEnglishVoice(tts!!, context) // Fallback a inglés
        }
        
        if (success) {
            currentLocale = locale
            Log.d(TAG, "✅ Idioma TTS configurado: ${locale.displayLanguage}")
            
            // Anunciar cambio de idioma si está habilitado
            if (isEnabled) {
                val languageName = getLanguageDisplayName(locale)
                val message = context.getString(R.string.tts_language_changed, languageName)
                speakText(message)
            }
        } else {
            Log.e(TAG, "❌ Error configurando idioma TTS: ${locale.displayLanguage}")
        }
    }
    
    /**
     * Obtiene el nombre display del idioma
     */
    private fun getLanguageDisplayName(locale: Locale): String {
        return when (locale.language) {
            "ca" -> "Català"
            "es" -> "Español"
            "en" -> "English"
            else -> locale.displayLanguage
        }
    }
    
    /**
     * Habilita o deshabilita el TTS
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        saveTTSSettings()
        
        if (enabled && isInitialized) {
            speakText(context.getString(R.string.tts_activated))
        }
        
        Log.d(TAG, "📢 TTS ${if (enabled) "habilitado" else "deshabilitado"}")
    }
    
    /**
     * Verifica si el TTS está habilitado
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Habla un texto específico
     */
    fun speakText(text: String) {
        if (!isEnabled || !isInitialized || tts == null) {
            Log.d(TAG, "TTS no disponible: enabled=$isEnabled, initialized=$isInitialized")
            return
        }
        
        Log.d(TAG, "🔊 Hablando: $text")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
    }
    
    /**
     * Actualiza el idioma cuando cambia en la aplicación
     */
    fun updateLanguageFromApp() {
        val newLocale = LocaleManager.getCurrentLocale(context)
        if (newLocale.language != currentLocale.language) {
            Log.d(TAG, "🔄 Detectado cambio de idioma: ${currentLocale.language} -> ${newLocale.language}")
            setupLanguage(newLocale)
        }
    }
    
    /**
     * Guarda la configuración del TTS
     */
    private fun saveTTSSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_TTS_ENABLED, isEnabled)
            .apply()
    }
    
    /**
     * Para compatibilidad con TTSManager existente
     */
    fun speakButtonAction(action: String) {
        if (!isEnabled) return
        
        val message = when (action) {
            "send_order" -> when (currentLocale.language) {
                "es" -> "Enviar pedido"
                "ca" -> "Enviar comanda"
                "en" -> "Send order"
                else -> "Send order"
            }
            "new_order" -> when (currentLocale.language) {
                "es" -> "Nuevo pedido"
                "ca" -> "Nova comanda"
                "en" -> "New order"
                else -> "New order"
            }
            else -> action
        }
        speakText(message)
    }
    
    fun speakQuantityChange(dishName: String, quantity: Int, isIncrease: Boolean) {
        if (!isEnabled) return
        
        val action = when (currentLocale.language) {
            "es" -> if (isIncrease) "aumentado" else "disminuido"
            "ca" -> if (isIncrease) "augmentat" else "disminuït"
            "en" -> if (isIncrease) "increased" else "decreased"
            else -> if (isIncrease) "increased" else "decreased"
        }
        
        val message = when (currentLocale.language) {
            "es" -> "$dishName: cantidad $action a $quantity"
            "ca" -> "$dishName: quantitat $action a $quantity"
            "en" -> "$dishName: quantity $action to $quantity"
            else -> "$dishName: quantity $action to $quantity"
        }
        
        speakText(message)
    }
    
    fun speakTableClientsChange(tableNumber: String, newCount: Int, isIncrease: Boolean) {
        if (!isEnabled) return
        
        val action = when (currentLocale.language) {
            "es" -> if (isIncrease) "aumentados" else "disminuidos"
            "ca" -> if (isIncrease) "augmentats" else "disminuïts"
            "en" -> if (isIncrease) "increased" else "decreased"
            else -> if (isIncrease) "increased" else "decreased"
        }
        
        val message = when (currentLocale.language) {
            "es" -> "Mesa $tableNumber: clientes $action a $newCount"
            "ca" -> "Taula $tableNumber: clients $action a $newCount"
            "en" -> "Table $tableNumber: clients $action to $newCount"
            else -> "Table $tableNumber: clients $action to $newCount"
        }
        
        speakText(message)
    }
    
    /**
     * Cierra y libera recursos
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        INSTANCE = null
    }
}