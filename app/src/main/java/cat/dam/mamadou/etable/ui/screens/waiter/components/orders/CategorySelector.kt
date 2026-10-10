@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components.orders

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import cat.dam.mamadou.etable.utils.CategoryTranslator
import cat.dam.mamadou.etable.utils.TTSManager

/**
 * Grupo de botones conectados (Material 3 Expressive). Selección única:
 * pulsar la categoría ya seleccionada no hace nada (misma lógica que antes).
 */
@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val context = LocalContext.current
    val ttsManager by lazy { TTSManager.getInstance(context) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        categories.forEachIndexed { index, category ->
            val isSelected = category == selectedCategory

            ToggleButton(
                checked = isSelected,
                onCheckedChange = {
                    if (category != selectedCategory) {
                        onCategorySelected(category)

                        val translatedCategory = CategoryTranslator.getTranslatedCategory(context, category)
                        ttsManager.speakCategoryChange(translatedCategory)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .semantics { role = Role.RadioButton },
                shapes = connectedShapes(index, categories.size)
            ) {
                Text(
                    text = CategoryTranslator.getTranslatedCategory(context, category),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Formas del grupo conectado: extremos redondeados, centro recto; morph a píldora al marcar. */
@Composable
internal fun connectedShapes(index: Int, count: Int): ToggleButtonShapes = when {
    count <= 1 -> ToggleButtonDefaults.shapes()
    index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
    index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
}

// MANTENER función existente para retrocompatibilidad
fun translateCategory(context: Context, category: String): String {
    return CategoryTranslator.getTranslatedCategory(context, category)
}
