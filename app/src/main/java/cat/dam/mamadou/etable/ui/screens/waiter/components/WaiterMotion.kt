@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components

import androidx.compose.animation.core.animateFloatAsState
import cat.dam.mamadou.etable.ui.theme.rememberReduceMotion
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberWaiterReduceMotion(): Boolean = rememberReduceMotion()

/** Feedback de pulsación: escala ligera a 0.96 (nunca desde 0). Sin efecto con reducción de movimiento. */
@Composable
fun Modifier.waiterPressScale(interactionSource: InteractionSource): Modifier {
    val reduceMotion = rememberWaiterReduceMotion()
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.96f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "waiterPressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
