package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem

@Composable
fun MenuItemsGrid(
    items: List<MenuItem>,
    onEditItem: (MenuItem) -> Unit,
    onDeleteItem: (MenuItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 220.dp),
        // Espacio inferior extra para que el FAB no tape la última fila
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.id }) { menuItem ->
            OptimizedMenuItemCard(
                menuItem = menuItem,
                onClick = { onEditItem(menuItem) },
                onDelete = { onDeleteItem(menuItem) },
                modifier = adminAnimateItem()
            )
        }
    }
}
