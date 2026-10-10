@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.DimensionUtils
import cat.dam.mamadou.etable.utils.TTSManager

@Composable
fun BottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }

    val iconSize = DimensionUtils.adaptiveDimen(
        phone = 28.dp,
        smallTablet = 32.dp,
        largeTablet = 36.dp
    )
    val textSize = DimensionUtils.adaptiveTextSize(
        phone = 12.sp,
        smallTablet = 14.sp,
        largeTablet = 16.sp
    )

    // NavigationBar estable: ShortNavigationBar (1.5.0-alpha22) solo colocaba el primer elemento.
    // Sin animaciones de entrada: cambiar de pestaña es de alta frecuencia (solo el indicador del item)
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        listOf(
            Triple("tables", R.drawable.restaurante, R.string.admin_nav_tables),
            Triple("orders", R.drawable.receta, R.string.admin_nav_orders),
            Triple("config", R.drawable.mantenimiento, R.string.admin_nav_config)
        ).forEach { (route, iconRes, labelRes) ->
            WaiterNavItem(
                selected = currentScreen == route,
                iconRes = iconRes,
                label = stringResource(labelRes),
                iconSize = iconSize,
                textSize = textSize,
                onClick = {
                    onNavigate(route)
                    if (ttsManager.isEnabled()) {
                        ttsManager.speakNavigationAction(route)
                    }
                }
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.WaiterNavItem(
    selected: Boolean,
    @DrawableRes iconRes: Int,
    label: String,
    iconSize: Dp,
    textSize: TextUnit,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            // Iconos de color: tint Unspecified conserva los colores originales del drawable
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = Color.Unspecified
            )
        },
        label = { Text(label, fontSize = textSize, maxLines = 1) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}
