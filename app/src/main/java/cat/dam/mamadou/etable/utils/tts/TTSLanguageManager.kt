package cat.dam.mamadou.etable.utils.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.*

class TTSLanguageManager {
    
    companion object {
        private const val TAG = "TTSLanguageManager"
        
        // Idiomas soportados por la aplicación
        val SUPPORTED_LANGUAGES = listOf(
            Locale("es", "ES"), // Español (España)
            Locale("ca", "ES"), // Catalán
            Locale("en", "US")  // Inglés (Estados Unidos)
        )
    }
    
    /**
     * Configura el idioma específico en el TTS
     */
    fun setupLanguage(tts: TextToSpeech, locale: Locale): Boolean {
        Log.d(TAG, "🌍 Configurando idioma: ${locale.language}_${locale.country}")
        
        // Primero intentar configurar el idioma específico
        val result = tts.setLanguage(locale)
        
        return when (result) {
            TextToSpeech.LANG_AVAILABLE -> {
                Log.d(TAG, "✅ Idioma ${locale.displayLanguage} disponible")
                selectBestVoice(tts, locale)
                true
            }
            TextToSpeech.LANG_COUNTRY_AVAILABLE -> {
                Log.d(TAG, "✅ País ${locale.displayCountry} disponible para ${locale.displayLanguage}")
                selectBestVoice(tts, locale)
                true
            }
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                Log.d(TAG, "✅ Variante disponible para ${locale.displayLanguage}")
                selectBestVoice(tts, locale)
                true
            }
            TextToSpeech.LANG_MISSING_DATA -> {
                Log.w(TAG, "⚠️ Datos faltantes para ${locale.displayLanguage}")
                tryFallbackLanguage(tts, locale)
            }
            TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.w(TAG, "⚠️ Idioma ${locale.displayLanguage} no soportado")
                tryFallbackLanguage(tts, locale)
            }
            else -> {
                Log.e(TAG, "❌ Error desconocido configurando ${locale.displayLanguage}")
                false
            }
        }
    }
    
    /**
     * Intenta un idioma de respaldo si el principal no funciona
     */
    private fun tryFallbackLanguage(tts: TextToSpeech, originalLocale: Locale): Boolean {
        val fallbackLocale = when (originalLocale.language) {
            "ca" -> {
                // Para catalán, intentar español como respaldo
                Log.d(TAG, "🔄 Catalán no disponible, intentando español como respaldo")
                Locale("es", "ES")
            }
            "es" -> {
                // Para español, intentar inglés como respaldo
                Log.d(TAG, "🔄 Español no disponible, intentando inglés como respaldo")
                Locale("en", "US")
            }
            else -> {
                // Para cualquier otro, usar inglés
                Log.d(TAG, "🔄 Idioma no disponible, usando inglés como respaldo")
                Locale("en", "US")
            }
        }
        
        val result = tts.setLanguage(fallbackLocale)
        return if (result >= TextToSpeech.LANG_AVAILABLE) {
            Log.d(TAG, "✅ Respaldo ${fallbackLocale.displayLanguage} configurado")
            selectBestVoice(tts, fallbackLocale)
            true
        } else {
            Log.e(TAG, "❌ Incluso el respaldo falló")
            false
        }
    }
    
    /**
     * Selecciona la mejor voz disponible para el idioma
     */
    private fun selectBestVoice(tts: TextToSpeech, locale: Locale) {
        try {
            val voices = tts.voices
            if (voices == null) {
                Log.w(TAG, "⚠️ No se pudieron obtener las voces disponibles")
                return
            }
            
            Log.d(TAG, "🔍 Buscando mejor voz para ${locale.displayLanguage}")
            
            // Filtrar voces por idioma
            val compatibleVoices = voices.filter { voice ->
                voice.locale.language == locale.language
            }
            
            if (compatibleVoices.isEmpty()) {
                Log.w(TAG, "⚠️ No se encontraron voces compatibles para ${locale.displayLanguage}")
                return
            }
            
            // Seleccionar la mejor voz basada en calidad y características
            val bestVoice = selectOptimalVoice(compatibleVoices, locale)
            
            if (bestVoice != null) {
                val result = tts.setVoice(bestVoice)
                if (result == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "✅ Voz seleccionada: ${bestVoice.name} para ${locale.displayLanguage}")
                } else {
                    Log.w(TAG, "⚠️ No se pudo establecer la voz: ${bestVoice.name}")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error seleccionando voz: ${e.message}", e)
        }
    }
    
    /**
     * Selecciona la voz óptima basada en criterios de calidad
     */
    private fun selectOptimalVoice(voices: List<Voice>, locale: Locale): Voice? {
        // Criterios de selección por orden de prioridad:
        // 1. Voz del país específico
        // 2. Voz femenina de alta calidad
        // 3. Voz masculina de alta calidad
        // 4. Cualquier voz de calidad normal
        
        val countrySpecific = voices.filter { 
            it.locale.country == locale.country 
        }
        
        val highQuality = voices.filter { 
            it.quality == Voice.QUALITY_VERY_HIGH || it.quality == Voice.QUALITY_HIGH 
        }
        
        // Prioridad 1: País específico + alta calidad
        countrySpecific.intersect(highQuality.toSet()).firstOrNull()?.let { return it }
        
        // Prioridad 2: País específico
        countrySpecific.firstOrNull()?.let { return it }
        
        // Prioridad 3: Alta calidad, preferir femenina (sin FEATURE_MALE)
        // CORREGIDO: Verificar si la voz tiene características masculinas
        highQuality.find { voice ->
            // Verificar si la voz NO tiene características masculinas
            !voice.features.contains("male") && !voice.features.contains("MALE")
        }?.let { return it }
        
        // Prioridad 4: Alta calidad, cualquier género
        highQuality.firstOrNull()?.let { return it }
        
        // Prioridad 5: Cualquier voz disponible
        return voices.firstOrNull()
    }
    
    /**
     * Obtiene la lista de idiomas soportados por la aplicación
     */
    fun getSupportedLanguages(): List<Locale> {
        return SUPPORTED_LANGUAGES
    }
    
    /**
     * Verifica si un idioma está soportado
     */
    fun isLanguageSupported(locale: Locale): Boolean {
        return SUPPORTED_LANGUAGES.any { 
            it.language == locale.language 
        }
    }
    
    /**
     * Obtiene información detallada de las voces disponibles
     */
    fun getVoiceInfo(tts: TextToSpeech): Map<String, List<String>> {
        val voiceInfo = mutableMapOf<String, MutableList<String>>()
        
        try {
            tts.voices?.forEach { voice ->
                val language = voice.locale.displayLanguage
                val voiceDetails = "${voice.name} (${voice.locale.displayCountry}) - Calidad: ${voice.quality}"
                
                voiceInfo.getOrPut(language) { mutableListOf() }.add(voiceDetails)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo información de voces: ${e.message}", e)
        }
        
        return voiceInfo
    }
}