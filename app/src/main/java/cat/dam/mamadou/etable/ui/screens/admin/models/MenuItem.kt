package cat.dam.mamadou.etable.ui.screens.admin.models

import android.net.Uri

/**
 * Modelo de datos para los platos del menú
 */
data class MenuItem(
    val id: String,
    val name: String,
    val category: MenuCategories, // Usar enum aquí
    val imageUrl: String? = null,
    val imageUri: Uri? = null,
    val isAvailable: Boolean = true,
    val requiresCookingPoint: Boolean = false // Propiedad para punto de cocción
)