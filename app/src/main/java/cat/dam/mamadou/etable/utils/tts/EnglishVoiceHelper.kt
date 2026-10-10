package cat.dam.mamadou.etable.utils.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class EnglishVoiceHelper {
    
    companion object {
        private const val TAG = "EnglishVoiceHelper"
    }
    
    fun setupBestEnglishVoice(tts: TextToSpeech, context: Context): Boolean {
        val englishLocale = Locale("en", "US")
        
        Log.d(TAG, "🇺🇸 Configurando voz inglesa")
        
        val result = tts.setLanguage(englishLocale)
        
        when (result) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                Log.d(TAG, "✅ Inglés disponible")
                setupEnglishVoiceSettings(tts)
                return true
            }
            else -> {
                Log.e(TAG, "❌ Inglés no disponible - esto es crítico")
                return false
            }
        }
    }
    
    private fun setupEnglishVoiceSettings(tts: TextToSpeech) {
        // Configuraciones específicas para inglés
        tts.setSpeechRate(0.95f) // Velocidad estándar
        tts.setPitch(1.0f) // Pitch estándar
        
        Log.d(TAG, "🎵 Configuración inglesa aplicada: velocidad=0.95, pitch=1.0")
    }
}