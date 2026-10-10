package cat.dam.mamadou.etable.ui.screens.admin.models

import cat.dam.mamadou.etable.utils.firebase.FirebaseManager

enum class MenuCategories {
    PRIMERS,
    SEGONS,
    POSTRES,
    BEGUDES;
    
    companion object {
        fun fromString(value: String): MenuCategories {
            return try {
                valueOf(value)
            } catch (e: Exception) {
                PRIMERS // Valor por defecto
            }
        }
    }
}

// Ya existente en tu código
private fun categoryToString(category: MenuCategories): String {
    return category.name
}

// Ejemplo para guardar un MenuItem en Firebase (añadir en MenuManagementScreen.kt)
private fun saveMenuItemToFirebase(firebaseManager: FirebaseManager, menuItem: MenuItem, onComplete: (Boolean) -> Unit) {
    val menuData = mapOf(
        "id" to menuItem.id,
        "name" to menuItem.name,
        "imageUrl" to menuItem.imageUrl,
        "isAvailable" to menuItem.isAvailable
    )
    
    // Resto del código para guardar en Firebase...
}

// Y para leer de Firebase:
private fun parseMenuItemFromFirebase(data: Map<String, Any>): MenuItem {
    val categoryStr = data["category"] as? String ?: "PRIMERS"
    val category = MenuCategories.fromString(categoryStr)
    
    return MenuItem(
        id = data["id"] as? String ?: "",
        name = data["name"] as? String ?: "",
        category = category, // Usar el enum correctamente
        imageUrl = data["imageUrl"] as? String,
        isAvailable = data["isAvailable"] as? Boolean ?: true
    )
}