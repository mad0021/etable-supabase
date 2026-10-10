package cat.dam.mamadou.etable.utils

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.tts.TTSLanguageManager
import cat.dam.mamadou.etable.utils.tts.TTSMessagesProvider
import java.util.*

class TTSManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isEnabled = false
    private var currentLocale: Locale = Locale.getDefault()
    
    // Managers para idiomas y mensajes
    private val languageManager = TTSLanguageManager()
    private val messagesProvider = TTSMessagesProvider(context)
    
    companion object {
        private const val TAG = "TTSManager"
        private const val PREFS_NAME = "tts_preferences"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_CURRENT_LANGUAGE = "tts_current_language"
        private const val KEY_CURRENT_COUNTRY = "tts_current_country"
        
        @Volatile
        private var INSTANCE: TTSManager? = null
        
        fun getInstance(context: Context): TTSManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TTSManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        loadTTSSettings()
        initializeTTS()
    }
    
    private fun loadTTSSettings() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isEnabled = prefs.getBoolean(KEY_TTS_ENABLED, false)
        
        // Cargar idioma guardado o usar el del sistema
        val savedLanguage = prefs.getString(KEY_CURRENT_LANGUAGE, null)
        val savedCountry = prefs.getString(KEY_CURRENT_COUNTRY, null)
        
        currentLocale = if (savedLanguage != null) {
            if (savedCountry != null) Locale(savedLanguage, savedCountry) 
            else Locale(savedLanguage)
        } else {
            LocaleManager.getCurrentLocale(context)
        }
        
        Log.d(TAG, "TTS configurado como: ${if (isEnabled) "activado" else "desactivado"}")
        Log.d(TAG, "Idioma TTS cargado: ${currentLocale.language}_${currentLocale.country}")
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                setupTTSLanguage(currentLocale)
                isInitialized = true
                Log.d(TAG, "TTS inicializado correctamente")
                
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "TTS iniciado: $utteranceId")
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "TTS completado: $utteranceId")
                    }
                    
                    override fun onError(utteranceId: String?) {
                        Log.e(TAG, "Error en TTS: $utteranceId")
                    }
                })
            } else {
                Log.e(TAG, "Error inicializando TTS: $status")
            }
        }
    }
    
    private fun setupTTSLanguage(locale: Locale) {
        if (tts == null) return
        
        Log.d(TAG, "🌍 Configurando TTS para idioma: ${locale.language}_${locale.country}")
        
        // Configurar idioma específico con la mejor voz disponible
        val result = languageManager.setupLanguage(tts!!, locale)
        
        if (result) {
            currentLocale = locale
            saveTTSLanguage(locale)
            
            // Configurar parámetros específicos del idioma
            setupLanguageSpecificSettings(locale)
            
            Log.d(TAG, "✅ TTS configurado correctamente para: ${locale.displayLanguage}")
        } else {
            Log.w(TAG, "⚠️ No se pudo configurar TTS para: ${locale.displayLanguage}")
        }
    }
    
    private fun setupLanguageSpecificSettings(locale: Locale) {
        when (locale.language) {
            "es" -> {
                // Configuración para español
                tts?.setSpeechRate(0.85f) // Más lento para español
                tts?.setPitch(1.0f)
            }
            "ca" -> {
                // Configuración para catalán
                tts?.setSpeechRate(0.9f) // Velocidad media para catalán
                tts?.setPitch(1.1f) // Pitch ligeramente más alto
            }
            "en" -> {
                // Configuración para inglés
                tts?.setSpeechRate(0.95f) // Velocidad estándar para inglés
                tts?.setPitch(1.0f)
            }
            else -> {
                // Configuración por defecto
                tts?.setSpeechRate(0.9f)
                tts?.setPitch(1.0f)
            }
        }
    }
    
    private fun saveTTSLanguage(locale: Locale) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_CURRENT_LANGUAGE, locale.language)
            putString(KEY_CURRENT_COUNTRY, locale.country)
            apply()
        }
    }
    
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
        Log.d(TAG, "TTS ${if (enabled) "activado" else "desactivado"}")
        
        if (enabled && isInitialized) {
            speak(messagesProvider.getTTSActivatedMessage(currentLocale))
        }
    }
    
    fun isEnabled(): Boolean = isEnabled
    
    fun speak(text: String, priority: Int = TextToSpeech.QUEUE_ADD) {
        if (!isEnabled || !isInitialized || tts == null) {
            return
        }
        
        Log.d(TAG, "Reproduciendo TTS [${currentLocale.language}]: $text")
        val utteranceId = UUID.randomUUID().toString()
        
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        
        tts?.speak(text, priority, params, utteranceId)
    }
    
    // *** FUNCIONES ESPECÍFICAS MULTIIDIOMA ***
    
    fun speakNotification(message: String) {
        val localizedMessage = messagesProvider.getNotificationMessage(currentLocale, message)
        speak(localizedMessage, TextToSpeech.QUEUE_FLUSH)
    }
    
    fun speakTableStatus(tableNumber: String, status: String, clientCount: Int) {
        if (!isEnabled) return
        val message = messagesProvider.getTableStatusMessage(currentLocale, tableNumber, status, clientCount)
        speak(message)
    }
    
    fun speakTableClientsChange(tableNumber: String, newCount: Int, isIncrease: Boolean) {
        if (!isEnabled) return
        val message = messagesProvider.getTableClientsChangeMessage(currentLocale, tableNumber, newCount, isIncrease)
        speak(message)
    }
    
    fun speakQuantityChange(dishName: String, quantity: Int, isIncrease: Boolean) {
        if (!isEnabled) return
        val message = messagesProvider.getQuantityChangeMessage(currentLocale, dishName, quantity, isIncrease)
        speak(message)
    }
    
    fun speakDishSelected(dishName: String) {
        if (!isEnabled) return
        val message = messagesProvider.getDishSelectedMessage(currentLocale, dishName)
        speak(message)
    }
    
    fun speakStageChange(stageName: String) {
        if (!isEnabled) return
        val message = messagesProvider.getStageChangeMessage(currentLocale, stageName)
        speak(message)
    }
    
    fun speakCategoryChange(categoryName: String) {
        if (!isEnabled) return
        val message = messagesProvider.getCategoryChangeMessage(currentLocale, categoryName)
        speak(message)
    }
    
    fun speakCategoryNotification(categoryName: String) {
        if (!isEnabled) return
        val message = messagesProvider.getCategoryNotificationMessage(currentLocale, categoryName)
        speak(message, TextToSpeech.QUEUE_FLUSH)
    }
    
    fun speakButtonAction(buttonType: String) {
        if (!isEnabled) return
        val message = messagesProvider.getButtonActionMessage(currentLocale, buttonType)
        speak(message)
    }
    
    fun speakNavigationAction(destination: String) {
        if (!isEnabled) return
        val message = messagesProvider.getNavigationActionMessage(currentLocale, destination)
        speak(message)
    }
    
    fun speakOrderStatus(status: String) {
        if (!isEnabled) return
        val message = messagesProvider.getOrderStatusMessage(currentLocale, status)
        speak(message)
    }
    
    /**
     * NUEVO: Función específica para notificaciones de descanso usando strings de recursos
     */
    fun speakBreakNotification(tableNumber: String, stageKey: String) {
        if (!isEnabled) return
        
        // Usar recursos específicos para nombres de etapas TTS
        val stageName = when(stageKey) {
            "PRIMERS_BEGUDES" -> context.getString(R.string.tts_stage_primers_begudes)
            "PRIMERS" -> context.getString(R.string.tts_stage_primers)
            "BEGUDES" -> context.getString(R.string.tts_stage_begudes)
            "SEGONS" -> context.getString(R.string.tts_stage_segons)
            "POSTRES" -> context.getString(R.string.tts_stage_postres)
            else -> context.getString(R.string.tts_stage_unknown)
        }
        
        // Usar string de recursos para el mensaje completo
        val message = context.getString(R.string.tts_notification_full, tableNumber, stageName)
        speak(message, TextToSpeech.QUEUE_FLUSH)
    }
    
    /**
     * NUEVO: Función específica para notificaciones de órdenes usando strings de recursos
     */
    fun speakOrderNotification(tableNumber: String, stageKey: String) {
        if (!isEnabled) return
        
        // Usar la misma función que para break notifications ya que el formato es igual
        speakBreakNotification(tableNumber, stageKey)
    }
    
    /**
     * NUEVO: Función mejorada para acciones de botones usando strings de recursos
     */
    fun speakButtonActionWithResources(buttonType: String) {
        if (!isEnabled) return
        
        val message = when (buttonType) {
            "served" -> context.getString(R.string.tts_button_served)
            "dismiss" -> context.getString(R.string.tts_button_dismiss)
            "new_order" -> context.getString(R.string.tts_button_new_order)
            "continue_order" -> context.getString(R.string.tts_button_continue_order)
            "send_order" -> context.getString(R.string.tts_button_send_order)
            "update_table" -> context.getString(R.string.tts_button_update_table)
            else -> {
                // Fallback a la función existente
                val fallbackMessage = messagesProvider.getButtonActionMessage(currentLocale, buttonType)
                speak(fallbackMessage)
                return
            }
        }
        speak(message)
    }
    
    /**
     * Actualiza el idioma del TTS cuando cambia el idioma de la aplicación
     */
    fun updateLanguage(locale: Locale) {
        if (!isInitialized || tts == null) {
            currentLocale = locale
            saveTTSLanguage(locale)
            return
        }
        
        Log.d(TAG, "🔄 Actualizando idioma TTS de ${currentLocale.language} a ${locale.language}")
        setupTTSLanguage(locale)
    }
    
    /**
     * Sincroniza el idioma del TTS con el idioma actual de la aplicación
     */
    fun synchronizeWithAppLanguage() {
        val appLocale = LocaleManager.getCurrentLocale(context)
        if (appLocale.language != currentLocale.language) {
            Log.d(TAG, "🔄 Sincronizando idioma TTS con aplicación")
            updateLanguage(appLocale)
        }
    }
    
    fun getCurrentLocale(): Locale = currentLocale
    
    fun getAvailableLanguages(): List<Locale> {
        return languageManager.getSupportedLanguages()
    }
    
    fun stop() {
        tts?.stop()
    }
    
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        isInitialized = false
        INSTANCE = null
    }
}