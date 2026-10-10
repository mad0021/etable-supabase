package cat.dam.mamadou.etable.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.CookingPoint

/**
 * Componente que muestra el punto de cocción localizado
 */
@Composable
fun CookingPointDisplay(cookingPoint: CookingPoint?) {
    val context = LocalContext.current
    if (cookingPoint != null) {
        Text(
            text = stringResource(
                id = R.string.admin_orders_cooking_point_prefix
            ) + " " + cookingPoint.getLocalizedString(context),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 2.dp)
        )
    }
}

// Para visualizar de manera consistente en todas las pantallas
@Composable
fun CookingPointChip(cookingPointText: String?) {
    if (cookingPointText == null || cookingPointText.isEmpty()) return

    val context = LocalContext.current

    // Intentar convertir el texto a enum CookingPoint
    val point = try {
        CookingPoint.values().find {
            it.display.equals(cookingPointText, ignoreCase = true)
        } ?: CookingPoint.fromLocalizedValue(cookingPointText)
    } catch (e: Exception) {
        null
    }

    // Usar el texto localizado si se pudo convertir, o el texto original
    val displayText = point?.getLocalizedString(context) ?: cookingPointText

    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(start = 8.dp)
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}