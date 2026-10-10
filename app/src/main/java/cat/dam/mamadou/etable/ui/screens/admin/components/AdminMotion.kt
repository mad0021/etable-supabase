@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.Box
import cat.dam.mamadou.etable.ui.theme.rememberReduceMotion
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

/** true si el usuario ha desactivado las animaciones del sistema (escala de animador = 0). */
@Composable
fun rememberAdminReduceMotion(): Boolean = rememberReduceMotion()

/**
 * Entrada/salida/recolocación de un elemento de lista cuando cambian los datos (p. ej. Realtime).
 * Solo fade + placement; con reducción de movimiento, solo fade (sin desplazamiento ni rebote).
 */
@Composable
fun LazyItemScope.adminAnimateItem(): Modifier {
    val motion = MaterialTheme.motionScheme
    val reduceMotion = rememberAdminReduceMotion()
    return Modifier.animateItem(
        fadeInSpec = motion.defaultEffectsSpec(),
        placementSpec = if (reduceMotion) null else motion.defaultSpatialSpec(),
        fadeOutSpec = motion.fastEffectsSpec(),
    )
}

/** Igual que [adminAnimateItem] para elementos de LazyVerticalGrid. */
@Composable
fun LazyGridItemScope.adminAnimateItem(): Modifier {
    val motion = MaterialTheme.motionScheme
    val reduceMotion = rememberAdminReduceMotion()
    return Modifier.animateItem(
        fadeInSpec = motion.defaultEffectsSpec(),
        placementSpec = if (reduceMotion) null else motion.defaultSpatialSpec(),
        fadeOutSpec = motion.fastEffectsSpec(),
    )
}

/** Indicador de carga Expressive centrado. */
@Composable
fun AdminLoading(modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LoadingIndicator()
    }
}
