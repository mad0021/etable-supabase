package cat.dam.mamadou.etable.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeFormatter {
    fun formatTime(context: Context, timestamp: Long): String {
        val currentLocale = context.resources.configuration.locales[0]
        val timeFormat = when (currentLocale.language) {
            "es", "ca" -> "HH:mm" // Formato 24h para español/catalán
            else -> "h:mm a" // Formato 12h para inglés
        }
        val formatter = SimpleDateFormat(timeFormat, currentLocale)
        return formatter.format(Date(timestamp))
    }
}