package cat.dam.mamadou.etable.utils.firebase

import android.util.Log
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuCategories
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import cat.dam.mamadou.etable.supabase.ListenerRegistration
import cat.dam.mamadou.etable.supabase.DocumentSnapshot
import kotlinx.coroutines.Dispatchers
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext

/**
 * Manager class for menu-related operations in Firestore
 */
class FirebaseMenuManager(private val firestore: FirebaseFirestore) {
    
    private val TAG = "FirebaseMenuManager"
    private val COLLECTION_NAME = "menu_items"
    
    // Función para convertir String a MenuCategories
    private fun stringToCategory(categoryStr: String): MenuCategories {
        return try {
            MenuCategories.valueOf(categoryStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting category: $categoryStr", e)
            MenuCategories.PRIMERS // Valor por defecto
        }
    }
    
    // En la función de conversión de Firebase a MenuItem
    private fun parseFirebaseMenuItem(doc: DocumentSnapshot): MenuItem? {
        return try {
            val imageUrl = doc.getString("imageUrl")
            val categoryStr = doc.getString("category") ?: "PRIMERS"
            
            MenuItem(
                id = doc.id,
                name = doc.getString("name") ?: "",
                category = stringToCategory(categoryStr),
                imageUrl = imageUrl,
                isAvailable = doc.getBoolean("isAvailable") ?: true,
                requiresCookingPoint = doc.getBoolean("requiresCookingPoint") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error mapeando elemento del menú: ${e.message}", e)
            null
        }
    }

    /**
     * Loads menu items for orders
     * @param onComplete Callback with the list of menu items
     */
    fun loadMenuItemsForOrders(onComplete: (List<MenuItem>) -> Unit) {
        // Only load available menu items for orders
        firestore.collection(COLLECTION_NAME)
            .whereEqualTo("isAvailable", true)
            .get()
            .addOnSuccessListener { result ->
                val items = result.documents.mapNotNull { doc ->
                    parseFirebaseMenuItem(doc)
                }
                Log.d(TAG, "Loaded ${items.size} order menu items")
                onComplete(items)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading order menu items: ${e.message}", e)
                onComplete(emptyList())
            }
    }

    /**
     * Escucha cambios en los elementos del menú en tiempo real
     * @param onMenuItemsUpdated Callback que se llama cuando hay cambios en el menú
     * @return ListenerRegistration para detener la escucha cuando sea necesario
     */
    fun listenToMenuItems(onMenuItemsUpdated: (List<MenuItem>) -> Unit): ListenerRegistration {
        return firestore.collection(COLLECTION_NAME)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando elementos del menú: ${error.message}", error)
                    onMenuItemsUpdated(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { doc ->
                        parseFirebaseMenuItem(doc)
                    }
                    Log.d(TAG, "Actualizados ${items.size} elementos del menú")
                    onMenuItemsUpdated(items)
                }
            }
    }
    
    /**
     * Añade un nuevo elemento al menú
     * @param menuItem Elemento del menú a añadir
     * @param onComplete Callback con el resultado de la operación (éxito o fracaso)
     */
    fun addMenuItem(menuItem: MenuItem, onComplete: (Boolean) -> Unit) {
        val menuItemData = FirebaseModelConverter.menuItemToMap(menuItem)
        
        firestore.collection(COLLECTION_NAME)
            .add(menuItemData)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Elemento añadido con ID: ${documentReference.id}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error añadiendo elemento: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Actualiza un elemento existente del menú
     * @param menuItem Elemento del menú a actualizar
     * @param onComplete Callback con el resultado de la operación (éxito o fracaso)
     */
    fun updateMenuItem(menuItem: MenuItem, onComplete: (Boolean) -> Unit) {
        val menuItemData = FirebaseModelConverter.menuItemToMap(menuItem)
        
        firestore.collection(COLLECTION_NAME)
            .document(menuItem.id)
            .update(menuItemData)
            .addOnSuccessListener {
                Log.d(TAG, "Elemento de menú actualizado: ${menuItem.id}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar elemento de menú: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Elimina un elemento del menú
     * @param menuItemId ID del elemento a eliminar
     * @param onComplete Callback con el resultado de la operación (éxito o fracaso)
     */
    fun deleteMenuItem(menuItemId: String, onComplete: (Boolean) -> Unit) {
        firestore.collection(COLLECTION_NAME)
            .document(menuItemId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Elemento de menú eliminado: $menuItemId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al eliminar elemento de menú: ${e.message}", e)
                onComplete(false)
            }
    }
    
    /**
     * Actualiza la disponibilidad de un elemento del menú
     * @param menuItemId ID del elemento a actualizar
     * @param isAvailable Nueva disponibilidad
     * @param onComplete Callback con el resultado de la operación (éxito o fracaso)
     */
    fun updateMenuItemAvailability(menuItemId: String, isAvailable: Boolean, onComplete: (Boolean) -> Unit) {
        firestore.collection(COLLECTION_NAME)
            .document(menuItemId)
            .update("isAvailable", isAvailable)
            .addOnSuccessListener {
                Log.d(TAG, "Disponibilidad actualizada para elemento $menuItemId: $isAvailable")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar disponibilidad: ${e.message}", e)
                onComplete(false)
            }
    }
}