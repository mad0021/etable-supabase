package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cat.dam.mamadou.etable.R

/** Filtro de estado como ToggleButton Expressive (la forma pasa de píldora a redondeada al marcarse). */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptimizedCategoryChip(
    category: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        shapes = ToggleButtonDefaults.shapes(),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Text(text = getDisplayNameForState(category))
    }
}

@Composable
private fun getDisplayNameForState(state: String): String {
    val context = LocalContext.current
    return when (state) {
        "LLIURE" -> context.getString(R.string.table_state_free)
        "OCUPADA" -> context.getString(R.string.state_occupied)
        "RESERVADA" -> context.getString(R.string.table_state_reserved)
        else -> state
    }
}
