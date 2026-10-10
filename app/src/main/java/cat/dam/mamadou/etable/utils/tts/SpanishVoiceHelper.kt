package cat.dam.mamadou.etable.utils.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class SpanishVoiceHelper {
    
    companion object {
        private const val TAG = "SpanishVoiceHelper"
    }
    
    fun setupBestSpanishVoice(tts: TextToSpeech, context: Context): Boolean {
        val spanishLocale = Locale("es", "ES")
        
        Log.d(TAG, "🇪🇸 Configurando voz española")
        
        val result = tts.setLanguage(spanishLocale)
        
        when (result) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                Log.d(TAG, "✅ Español disponible")
                setupSpanishVoiceSettings(tts)
                return true
            }
            else -> {
                Log.w(TAG, "⚠️ Español no disponible, usando inglés como respaldo")
                return EnglishVoiceHelper().setupBestEnglishVoice(tts, context)
            }
        }
    }
    
    private fun setupSpanishVoiceSettings(tts: TextToSpeech) {
        // Configuraciones específicas para español
        tts.setSpeechRate(0.85f) // Más lento para español
        tts.setPitch(1.0f) // Pitch estándar
        
        Log.d(TAG, "🎵 Configuración española aplicada: velocidad=0.85, pitch=1.0")
    }
}