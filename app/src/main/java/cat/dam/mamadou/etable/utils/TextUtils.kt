// c:\Users\AA\AndroidStudioProjects\etable\app\src\main\java\cat\dam\mamadou\etable\utils\TextUtils.kt

package cat.dam.mamadou.etable.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Remove these duplicate declarations and import them instead
// import the WindowSizeClass and WindowSizeUtils from the proper file

object TextUtils {

    // Constantes para los tamaños de texto
    private const val DEFAULT_TEXT_SIZE = 16f
    private const val MIN_TEXT_SIZE = 12f
    private const val MAX_TEXT_SIZE = 24f

    // Constantes para las preferencias
    private const val PREFS_NAME = "TextPreferences"
    private const val TEXT_SIZE_KEY = "text_size"


    /**
     * Formatea el texto según el rol del usuario.
     * @param text Texto a formatear
     * @param isAdmin Indica si el usuario es administrador
     * @return Texto formateado según el rol
     */
    fun formatTextByRole(text: String, isAdmin: Boolean): String {
        return if (isAdmin) {
            text.lowercase()
        } else {
            text.uppercase()
        }
    }

    /**
     * Formatea el texto según el rol del usuario utilizando un recurso de cadena.
     * @param context Contexto de la aplicación
     * @param stringResId ID del recurso de cadena
     * @param isAdmin Indica si el usuario es administrador
     * @return Texto formateado según el rol
     */
    fun formatTextByRole(context: Context, stringResId: Int, isAdmin: Boolean): String {
        val stringValue = context.getString(stringResId)
        // Aplicar formato según el rol si es necesario
        return stringValue
    }

    /**
     * Obtiene el tamaño de texto guardado en las preferencias.
     * @param context Contexto de la aplicación
     * @return Tamaño de texto guardado o el valor por defecto
     */
    fun getSavedTextSize(context: Context): Float {
        val prefs = getPreferences(context)
        return prefs.getFloat(TEXT_SIZE_KEY, DEFAULT_TEXT_SIZE)
    }

    /**
     * Guarda el tamaño de texto en las preferencias.
     * @param context Contexto de la aplicación
     * @param size Tamaño de texto a guardar
     */
    fun saveTextSize(context: Context, size: Float) {
        val prefs = getPreferences(context)
        prefs.edit().putFloat(TEXT_SIZE_KEY, size).apply()
    }

    /**
     * Incrementa el tamaño de texto.
     * @param context Contexto de la aplicación
     * @return Nuevo tamaño de texto
     */
    fun increaseTextSize(context: Context): Float {
        val currentSize = getSavedTextSize(context)
        val newSize = minOf(currentSize + 2f, MAX_TEXT_SIZE)
        saveTextSize(context, newSize)
        return newSize
    }

    /**
     * Decrementa el tamaño de texto.
     * @param context Contexto de la aplicación
     * @return Nuevo tamaño de texto
     */
    fun decreaseTextSize(context: Context): Float {
        val currentSize = getSavedTextSize(context)
        val newSize = maxOf(currentSize - 2f, MIN_TEXT_SIZE)
        saveTextSize(context, newSize)
        return newSize
    }

    /**
     * Obtiene un estilo de texto adaptativo basado en el tamaño de pantalla.
     * @param context Contexto de la aplicación
     * @param weight Peso de la fuente
     * @param scaleFactor Factor de escala adicional (opcional)
     * @return Estilo de texto adaptado al tamaño de pantalla
     */
    @Composable
    fun getAdaptiveTextStyle(
        context: Context,
        weight: FontWeight = FontWeight.Normal,
        scaleFactor: Float = 1.0f
    ): TextStyle {
        val baseSize = getSavedTextSize(context)
        val windowWidthClass = WindowSizeUtils.getWindowWidthClass()

        // Ajustar el tamaño según el tipo de dispositivo
        val adjustedSize = when (windowWidthClass) {
            WindowSizeClass.COMPACT -> baseSize
            WindowSizeClass.MEDIUM -> baseSize * 1.2f
            WindowSizeClass.EXPANDED -> baseSize * 1.4f
        } * scaleFactor

        return TextStyle(
            fontSize = adjustedSize.sp,
            fontWeight = weight
        )
    }

    /**
     * Obtiene las preferencias compartidas.
     * @param context Contexto de la aplicación
     * @return Preferencias compartidas
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}