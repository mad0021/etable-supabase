package cat.dam.mamadou.etable.utils

import cat.dam.mamadou.etable.models.CookingPoint

object DataMigrations {
    /**
     * Migra valores antiguos de punto de cocción a los nuevos
     */
    fun migrateCookingPoint(oldValue: String?): CookingPoint? {
        return when (oldValue?.lowercase()) {
            "poco hecho" -> CookingPoint.RARE
            "al punto" -> CookingPoint.MEDIUM
            "hecho" -> CookingPoint.MEDIUM_WELL
            "muy hecho" -> CookingPoint.WELL_DONE
            else -> null
        }
    }
    
    /**
     * Usa esta función al recibir datos de Firebase
     */
    fun processOrderFromFirebase(order: Map<String, Any>): Map<String, Any> {
        // Ejemplo de migración
        if (order.containsKey("cookingPoint")) {
            val oldCookingPoint = order["cookingPoint"] as? String
            val newCookingPoint = migrateCookingPoint(oldCookingPoint)
            if (newCookingPoint != null) {
                val mutableOrder = order.toMutableMap()
                mutableOrder["cookingPoint"] = newCookingPoint.name
                return mutableOrder
            }
        }
        return order
    }
}