package cat.dam.mamadou.etable.utils.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*

class CatalanVoiceHelper {
    
    companion object {
        private const val TAG = "CatalanVoiceHelper"
    }
    
    fun setupBestCatalanVoice(tts: TextToSpeech, context: Context): Boolean {
        val catalanLocale = Locale("ca", "ES")
        
        Log.d(TAG, "🇪🇸 Configurando voz catalana")
        
        val result = tts.setLanguage(catalanLocale)
        
        when (result) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> {
                Log.d(TAG, "✅ Catalán disponible")
                setupCatalanVoiceSettings(tts)
                return true
            }
            else -> {
                Log.w(TAG, "⚠️ Catalán no disponible, usando español como respaldo")
                return SpanishVoiceHelper().setupBestSpanishVoice(tts, context)
            }
        }
    }
    
    private fun setupCatalanVoiceSettings(tts: TextToSpeech) {
        // Configuraciones específicas para catalán
        tts.setSpeechRate(0.9f) // Velocidad media
        tts.setPitch(1.1f) // Pitch ligeramente más alto
        
        Log.d(TAG, "🎵 Configuración catalana aplicada: velocidad=0.9, pitch=1.1")
    }
}