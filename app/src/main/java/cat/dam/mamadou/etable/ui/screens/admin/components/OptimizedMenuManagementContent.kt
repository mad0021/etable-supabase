package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuCategories

@Composable
fun OptimizedMenuManagementContent(
    menuItems: List<MenuItem>,
    isLoading: Boolean,
    selectedCategory: MenuCategories,
    onCategorySelected: (MenuCategories) -> Unit,
    onEditItem: (MenuItem) -> Unit,
    onDeleteItem: (MenuItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Selector de categorías horizontal
        CategoryTabRow(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contenido filtrado por categoría
        if (isLoading) {
            AdminLoading(Modifier.fillMaxWidth().padding(32.dp))
        } else {
            // Filtrar elementos por la categoría seleccionada - Corrección aquí
            val filteredItems = menuItems.filter { it.category == selectedCategory }
            
            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.menu_category_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Mostrar los elementos de la categoría seleccionada
                MenuItemsGrid(
                    items = filteredItems,
                    onEditItem = onEditItem,
                    onDeleteItem = onDeleteItem
                )
            }
        }
    }
}

@Composable
fun CategoryTabRow(
    selectedCategory: MenuCategories,
    onCategorySelected: (MenuCategories) -> Unit
) {
    val categories = MenuCategories.values()

    // Categorías como grupo de ToggleButton conectados (filtro instantáneo, sin animación de contenido)
    AdminConnectedToggleGroup(
        labels = categories.map { getCategoryDisplayName(it) },
        selectedIndex = selectedCategory.ordinal,
        onSelect = { onCategorySelected(categories[it]) }
    )
}

// Función para obtener el nombre de categoría para mostrar
@Composable
fun getCategoryDisplayName(category: MenuCategories): String {
    return when (category) {
        MenuCategories.PRIMERS -> stringResource(R.string.menu_category_primers)
        MenuCategories.SEGONS -> stringResource(R.string.menu_category_segons)
        MenuCategories.POSTRES -> stringResource(R.string.menu_category_postres)
        MenuCategories.BEGUDES -> stringResource(R.string.menu_category_begudes)
    }
}