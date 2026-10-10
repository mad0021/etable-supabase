package cat.dam.mamadou.etable.utils.tts

import android.content.Context
import cat.dam.mamadou.etable.R
import java.util.*

class TTSMessagesProvider(private val context: Context) {
    
    /**
     * Obtiene el mensaje de TTS activado según el idioma
     */
    fun getTTSActivatedMessage(locale: Locale): String {
        return when (locale.language) {
            "es" -> "Síntesis de voz activada"
            "ca" -> "Síntesi de veu activada"
            "en" -> "Text to speech activated"
            else -> "Text to speech activated"
        }
    }
    
    /**
     * Obtiene el mensaje de estado de mesa
     */
    fun getTableStatusMessage(locale: Locale, tableNumber: String, status: String, clientCount: Int): String {
        return when (locale.language) {
            "es" -> when {
                clientCount > 0 -> "Mesa $tableNumber ocupada con $clientCount clientes"
                status == "RESERVED" -> "Mesa $tableNumber reservada"
                else -> "Mesa $tableNumber libre"
            }
            "ca" -> when {
                clientCount > 0 -> "Taula $tableNumber ocupada amb $clientCount clients"
                status == "RESERVED" -> "Taula $tableNumber reservada"
                else -> "Taula $tableNumber lliure"
            }
            "en" -> when {
                clientCount > 0 -> "Table $tableNumber occupied with $clientCount clients"
                status == "RESERVED" -> "Table $tableNumber reserved"
                else -> "Table $tableNumber free"
            }
            else -> "Table $tableNumber - $clientCount clients"
        }
    }
    
    /**
     * Obtiene el mensaje de cambio de clientes en mesa
     */
    fun getTableClientsChangeMessage(locale: Locale, tableNumber: String, newCount: Int, isIncrease: Boolean): String {
        return when (locale.language) {
            "es" -> {
                val action = if (isIncrease) "aumentados" else "disminuidos"
                "Mesa $tableNumber: clientes $action a $newCount"
            }
            "ca" -> {
                val action = if (isIncrease) "augmentats" else "disminuïts"
                "Taula $tableNumber: clients $action a $newCount"
            }
            "en" -> {
                val action = if (isIncrease) "increased" else "decreased"
                "Table $tableNumber: clients $action to $newCount"
            }
            else -> "Table $tableNumber: $newCount clients"
        }
    }
    
    /**
     * Obtiene el mensaje de cambio de cantidad de platos
     */
    fun getQuantityChangeMessage(locale: Locale, dishName: String, quantity: Int, isIncrease: Boolean): String {
        return when (locale.language) {
            "es" -> {
                val action = if (isIncrease) "aumentado" else "disminuido"
                "$dishName: cantidad $action a $quantity"
            }
            "ca" -> {
                val action = if (isIncrease) "augmentat" else "disminuït"
                "$dishName: quantitat $action a $quantity"
            }
            "en" -> {
                val action = if (isIncrease) "increased" else "decreased"
                "$dishName: quantity $action to $quantity"
            }
            else -> "$dishName: $quantity"
        }
    }
    
    /**
     * Obtiene el mensaje de plato seleccionado
     */
    fun getDishSelectedMessage(locale: Locale, dishName: String): String {
        return when (locale.language) {
            "es" -> "Plato seleccionado: $dishName"
            "ca" -> "Plat seleccionat: $dishName"
            "en" -> "Dish selected: $dishName"
            else -> "Selected: $dishName"
        }
    }
    
    /**
     * Obtiene el mensaje de cambio de etapa
     */
    fun getStageChangeMessage(locale: Locale, stageName: String): String {
        return when (locale.language) {
            "es" -> "Etapa seleccionada: $stageName"
            "ca" -> "Etapa seleccionada: $stageName"
            "en" -> "Stage selected: $stageName"
            else -> "Stage: $stageName"
        }
    }
    
    /**
     * Obtiene el mensaje de cambio de categoría
     */
    fun getCategoryChangeMessage(locale: Locale, categoryName: String): String {
        return when (locale.language) {
            "es" -> "Categoría seleccionada: $categoryName"
            "ca" -> "Categoria seleccionada: $categoryName"
            "en" -> "Category selected: $categoryName"
            else -> "Category: $categoryName"
        }
    }
    
    /**
     * Obtiene el mensaje de notificación de categoría lista
     */
    fun getCategoryNotificationMessage(locale: Locale, categoryName: String): String {
        return when (locale.language) {
            "es" -> "$categoryName listos en cocina"
            "ca" -> "$categoryName llestos a la cuina"
            "en" -> "$categoryName ready in kitchen"
            else -> "$categoryName ready"
        }
    }
    
    /**
     * Obtiene el mensaje de acción de botón
     */
    fun getButtonActionMessage(locale: Locale, buttonType: String): String {
        return when (locale.language) {
            "es" -> when (buttonType) {
                "new_order" -> "Nueva comanda"
                "continue_order" -> "Continuar comanda"
                "update_table" -> "Actualizar mesa"
                "send_order" -> "Enviar comanda"
                "served" -> "Servido"
                "dismiss" -> "Descartar"
                else -> buttonType
            }
            "ca" -> when (buttonType) {
                "new_order" -> "Nova comanda"
                "continue_order" -> "Continuar comanda"
                "update_table" -> "Actualitzar taula"
                "send_order" -> "Enviar comanda"
                "served" -> "Servit"
                "dismiss" -> "Descartar"
                else -> buttonType
            }
            "en" -> when (buttonType) {
                "new_order" -> "New order"
                "continue_order" -> "Continue order"
                "update_table" -> "Update table"
                "send_order" -> "Send order"
                "served" -> "Served"
                "dismiss" -> "Dismiss"
                else -> buttonType
            }
            else -> buttonType
        }
    }
    
    /**
     * Obtiene el mensaje de navegación
     */
    fun getNavigationActionMessage(locale: Locale, destination: String): String {
        return when (locale.language) {
            "es" -> when (destination) {
                "tables" -> "Ir a mesas"
                "orders" -> "Ir a comandas"
                "config" -> "Ir a configuración"
                else -> destination
            }
            "ca" -> when (destination) {
                "tables" -> "Anar a taules"
                "orders" -> "Anar a comandes"
                "config" -> "Anar a configuració"
                else -> destination
            }
            "en" -> when (destination) {
                "tables" -> "Go to tables"
                "orders" -> "Go to orders"
                "config" -> "Go to settings"
                else -> destination
            }
            else -> destination
        }
    }
    
    /**
     * Obtiene el mensaje de estado de pedido
     */
    fun getOrderStatusMessage(locale: Locale, status: String): String {
        return when (locale.language) {
            "es" -> when (status) {
                "order_sent_success" -> "Comanda enviada correctamente"
                "order_completed" -> "Comanda completada"
                else -> status
            }
            "ca" -> when (status) {
                "order_sent_success" -> "Comanda enviada correctament"
                "order_completed" -> "Comanda completada"
                else -> status
            }
            "en" -> when (status) {
                "order_sent_success" -> "Order sent successfully"
                "order_completed" -> "Order completed"
                else -> status
            }
            else -> status
        }
    }
    
    /**
     * Obtiene mensaje de notificación genérico
     */
    fun getNotificationMessage(locale: Locale, message: String): String {
        // Si el mensaje ya está localizado, devolverlo tal como está
        // Si no, intentar traducirlo basándose en palabras clave
        return when (locale.language) {
            "es", "ca", "en" -> message // Ya localizado
            else -> message
        }
    }

    /**
     * NUEVO: Obtiene el mensaje de notificación completa usando strings de recursos
     */
    fun getNotificationMessageWithResources(context: Context, tableNumber: String, stageKey: String): String {
        val stageName = when(stageKey) {
            "PRIMERS_BEGUDES" -> context.getString(R.string.tts_stage_primers_begudes)
            "PRIMERS" -> context.getString(R.string.tts_stage_primers)
            "BEGUDES" -> context.getString(R.string.tts_stage_begudes)
            "SEGONS" -> context.getString(R.string.tts_stage_segons)
            "POSTRES" -> context.getString(R.string.tts_stage_postres)
            else -> context.getString(R.string.tts_stage_unknown)
        }
        
        return context.getString(R.string.tts_notification_full, tableNumber, stageName)
    }

    /**
     * NUEVO: Obtiene el nombre de la etapa traducido usando strings de recursos
     */
    fun getStageNameForTTSWithResources(context: Context, stageKey: String): String {
        return when(stageKey) {
            "PRIMERS_BEGUDES" -> context.getString(R.string.tts_stage_primers_begudes)
            "PRIMERS" -> context.getString(R.string.tts_stage_primers)
            "BEGUDES" -> context.getString(R.string.tts_stage_begudes)
            "SEGONS" -> context.getString(R.string.tts_stage_segons)
            "POSTRES" -> context.getString(R.string.tts_stage_postres)
            else -> context.getString(R.string.tts_stage_unknown)
        }
    }

    /**
     * NUEVO: Obtiene mensaje de acción de botón usando strings de recursos
     */
    fun getButtonActionMessageWithResources(context: Context, buttonType: String): String {
        return when (buttonType) {
            "served" -> context.getString(R.string.tts_button_served)
            "dismiss" -> context.getString(R.string.tts_button_dismiss)
            "new_order" -> context.getString(R.string.tts_button_new_order)
            "continue_order" -> context.getString(R.string.tts_button_continue_order)
            "send_order" -> context.getString(R.string.tts_button_send_order)
            "update_table" -> context.getString(R.string.tts_button_update_table)
            else -> {
                // Fallback a la función existente
                getButtonActionMessage(Locale.getDefault(), buttonType)
            }
        }
    }
}