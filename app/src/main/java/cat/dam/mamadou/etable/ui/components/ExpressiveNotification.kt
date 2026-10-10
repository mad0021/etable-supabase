package cat.dam.mamadou.etable.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import cat.dam.mamadou.etable.ui.theme.rememberReduceMotion
import kotlinx.coroutines.flow.first

/**
 * Estado de un aviso que entra al componerse. Poner targetState = false para cerrarlo:
 * onDismissed se llama una sola vez, cuando termina la animación de salida.
 */
@Composable
fun rememberNotificationState(onDismissed: () -> Unit): MutableTransitionState<Boolean> {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    val latest by rememberUpdatedState(onDismissed)
    LaunchedEffect(state) {
        snapshotFlow { !state.targetState && state.isIdle }.first { it }
        latest()
    }
    return state
}

/**
 * Entrada expresiva (spring del motionScheme: fade + escala desde 0.9 + leve caída) y salida más sutil.
 * Para eventos poco frecuentes e importantes (pedido listo, categoría lista). Sin bucles.
 * Con rememberReduceMotion() la entrada no se desplaza.
 */
@Composable
fun ExpressiveNotificationVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val motion = MaterialTheme.motionScheme
    val reduceMotion = rememberReduceMotion()
    val enter = fadeIn(motion.defaultEffectsSpec()) +
        scaleIn(motion.defaultSpatialSpec(), initialScale = 0.9f) +
        if (reduceMotion) EnterTransition.None else slideInVertically(motion.defaultSpatialSpec()) { -it / 4 }
    val exit = fadeOut(motion.fastEffectsSpec()) + scaleOut(motion.fastEffectsSpec(), targetScale = 0.96f)
    AnimatedVisibility(visibleState = visibleState, modifier = modifier, enter = enter, exit = exit, content = content)
}
