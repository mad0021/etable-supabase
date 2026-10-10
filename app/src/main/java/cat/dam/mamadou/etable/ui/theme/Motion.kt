package cat.dam.mamadou.etable.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * true si el usuario activó "Quitar animaciones" (ANIMATOR_DURATION_SCALE == 0).
 * Con reducción: sin rebote (el tema ya usa MotionScheme.standard()) y entradas sin desplazamiento.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    // shortcut: se lee al componer, no observa cambios en caliente; basta con reabrir la pantalla
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
