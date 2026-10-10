package cat.dam.mamadou.etable.utils

import android.content.Context
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.theme.AppTheme

/**
 * Ayudante para manejo de temas y su traducción
 */
object ThemeHelper {
    // Lista de temas disponibles (constantes internas) - SIN SYSTEM
    val AVAILABLE_THEMES = listOf(
        "LIGHT", "DARK", "OCEAN", "SUNSET", "NATURE", 
        "PURPLE", "MONOCHROME", "AUTUMN", "VINTAGE", "FIRE", "MIDNIGHT", "FOREST"
    )
    
    /**
     * Obtiene el ID de recurso para el nombre del tema
     */
    fun getThemeNameResourceId(theme: String): Int {
        return when (theme) {
            "LIGHT" -> R.string.theme_name_light
            "DARK" -> R.string.theme_name_dark
            "OCEAN" -> R.string.theme_name_ocean
            "SUNSET" -> R.string.theme_name_sunset
            "NATURE" -> R.string.theme_name_nature
            "PURPLE" -> R.string.theme_name_purple
            "MONOCHROME" -> R.string.theme_name_monochrome
            "AUTUMN" -> R.string.theme_name_autumn
            "VINTAGE" -> R.string.theme_name_vintage
            "FIRE" -> R.string.theme_name_fire
            "MIDNIGHT" -> R.string.theme_name_midnight
            "FOREST" -> R.string.theme_name_forest
            else -> R.string.theme_name_light // Agregar else
        }
    }
    
    /**
     * Obtiene el nombre traducido de un tema
     */
    fun getLocalizedThemeName(context: Context, theme: String): String {
        return context.getString(getThemeNameResourceId(theme))
    }
    
    /**
     * Obtiene el ID del recurso para la descripción del tema
     */
    fun getThemeDescriptionResourceId(theme: String): Int {
        return when (theme) {
            "LIGHT" -> R.string.theme_desc_light
            "DARK" -> R.string.theme_desc_dark
            "OCEAN" -> R.string.theme_desc_ocean
            "SUNSET" -> R.string.theme_desc_sunset
            "NATURE" -> R.string.theme_desc_nature
            "PURPLE" -> R.string.theme_desc_purple
            "MONOCHROME" -> R.string.theme_desc_monochrome
            "AUTUMN" -> R.string.theme_desc_autumn
            "VINTAGE" -> R.string.theme_desc_vintage
            "FIRE" -> R.string.theme_desc_fire
            "MIDNIGHT" -> R.string.theme_desc_midnight
            "FOREST" -> R.string.theme_desc_forest
            else -> R.string.theme_desc_light // Agregar else
        }
    }
    
    /**
     * Obtiene el nombre traducido del tema
     */
    fun getThemeName(context: Context, theme: AppTheme): String {
        val resourceId = getThemeNameResourceId(theme.name)
        return context.getString(resourceId)
    }
    
    /**
     * Obtiene la descripción traducida del tema
     */
    fun getThemeDescription(context: Context, theme: AppTheme): String {
        val resourceId = getThemeDescriptionResourceId(theme.name)
        return context.getString(resourceId)
    }

    /**
     * Obtiene el ID de recurso para el nombre del tema (sobrecarga para AppTheme)
     */
    fun getThemeNameResourceId(theme: AppTheme): Int {
        return getThemeNameResourceId(theme.name)
    }

    /**
     * Obtiene el ID de recurso para la descripción del tema (sobrecarga para AppTheme)
     */
    fun getThemeDescriptionResourceId(theme: AppTheme): Int {
        return getThemeDescriptionResourceId(theme.name)
    }
}