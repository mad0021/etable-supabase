// c:\Users\AA\AndroidStudioProjects\etable\app\src\main\java\cat\dam\mamadou\etable\utils\WindowSizeUtils.kt

package cat.dam.mamadou.etable.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Clase que representa las diferentes clases de tamaño de ventana.
 * - COMPACT: Para dispositivos pequeños como teléfonos.
 * - MEDIUM: Para dispositivos medianos como tablets pequeñas.
 * - EXPANDED: Para dispositivos grandes como tablets grandes o pantallas de escritorio.
 */
enum class WindowSizeClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Utilidad para determinar la clase de tamaño de ventana basada en el ancho y alto.
 */
object WindowSizeUtils {

    /**
     * Obtiene la clase de tamaño de ventana basada en el ancho.
     * @return La clase de tamaño de ventana (COMPACT, MEDIUM, EXPANDED).
     */
    @Composable
    fun getWindowWidthClass(): WindowSizeClass {
        val configuration = LocalConfiguration.current
        val widthDp = configuration.screenWidthDp.dp

        return when {
            widthDp < 600.dp -> WindowSizeClass.COMPACT
            widthDp < 840.dp -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        }
    }
}