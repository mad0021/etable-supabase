package cat.dam.mamadou.etable.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cat.dam.mamadou.etable.R

/**
 * Utilidad para traducir categorías de menú
 */
object CategoryTranslator {
    /**
     * Obtiene las categorías visibles según el menú disponible
     * NUEVA LÓGICA: Separar PRIMERS y BEGUDES si ambos están disponibles
     */
    fun getCategoriesForStage(
        context: Context,
        availableMenuCategories: Set<String>,
        stage: String
    ): List<String> {
        return when (stage.uppercase()) {
            "PRIMERS_BEGUDES" -> {
                val hasPrimers = availableMenuCategories.any { it.contains("PRIMERS", ignoreCase = true) }
                val hasBegudes = availableMenuCategories.any { it.contains("BEGUDES", ignoreCase = true) }

                when {
                    hasPrimers && hasBegudes -> listOf("PRIMERS", "BEGUDES") // SEPARADOS
                    hasPrimers -> listOf("PRIMERS") // SOLO ENTRANTES
                    hasBegudes -> listOf("BEGUDES") // SOLO BEBIDAS
                    else -> listOf("PRIMERS_BEGUDES") // FALLBACK
                }
            }
            "SEGONS" -> listOf("SEGONS")
            "POSTRES" -> listOf("POSTRES")
            else -> emptyList()
        }
    }

    /**
     * Traduce una categoría individual al idioma actual
     * MANTENER toda la funcionalidad existente + añadir nuevas traducciones
     */
    fun getTranslatedCategory(context: Context, category: String): String {
        return when (category.uppercase()) {
            "PRIMERS" -> context.getString(R.string.category_primers_only)
            "BEGUDES" -> context.getString(R.string.category_begudes_only)
            "PRIMERS_BEGUDES" -> context.getString(R.string.category_primers_begudes_combined)
            "SEGONS" -> context.getString(R.string.category_segons)
            "POSTRES" -> context.getString(R.string.category_postres)
            else -> category
        }
    }

    /**
     * Versión @Composable para usar en funciones de composición
     */
    @Composable
    fun getTranslatedCategory(internalCategory: String): String {
        return when (internalCategory) {
            "PRIMERS_BEGUDES" -> stringResource(R.string.category_primers_begudes)
            "PRIMERS" -> stringResource(R.string.category_primers_begudes)
            "BEGUDES" -> stringResource(R.string.category_primers_begudes)
            "SEGONS" -> stringResource(R.string.category_segons)
            "POSTRES" -> stringResource(R.string.category_postres)
            else -> internalCategory
        }
    }

    /**
     * MANTENER función existente para retrocompatibilidad
     */
    fun translateCategoryForFirebase(category: String): String {
        return when (category.uppercase()) {
            "PRIMERS", "STARTERS", "ENTRANTES" -> "PRIMERS"
            "BEGUDES", "DRINKS", "BEBIDAS" -> "BEGUDES"
            "SEGONS", "MAIN_COURSES", "PLATOS_PRINCIPALES" -> "SEGONS"
            "POSTRES", "DESSERTS" -> "POSTRES"
            else -> category.uppercase()
        }
    }

    /**
     * NUEVA: Mapea categorías de UI a categorías de Firebase para consultas
     */
    fun mapCategoryForFirebaseQuery(uiCategory: String): List<String> {
        return when (uiCategory.uppercase()) {
            "PRIMERS" -> listOf("PRIMERS", "PRIMERS_BEGUDES")
            "BEGUDES" -> listOf("BEGUDES", "PRIMERS_BEGUDES")
            "SEGONS" -> listOf("SEGONS")
            "POSTRES" -> listOf("POSTRES")
            "PRIMERS_BEGUDES" -> listOf("PRIMERS", "BEGUDES", "PRIMERS_BEGUDES")
            else -> listOf(uiCategory)
        }
    }
}