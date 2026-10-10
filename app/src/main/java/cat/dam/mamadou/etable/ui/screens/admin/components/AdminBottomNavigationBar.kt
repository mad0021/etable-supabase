@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.DimensionUtils

private data class AdminNavItem(val route: String, val iconRes: Int, val labelRes: Int)

private val adminNavItems = listOf(
    AdminNavItem("orders", R.drawable.archivo, R.string.admin_nav_orders),
    AdminNavItem("reservations", R.drawable.reserva, R.string.admin_nav_reservations),
    AdminNavItem("menu", R.drawable.menu, R.string.admin_nav_menu),
    AdminNavItem("break_requests", R.drawable.detener, R.string.admin_nav_break_requests),
    AdminNavItem("users", R.drawable.superpoblacion, R.string.admin_nav_users),
    AdminNavItem("config", R.drawable.mantenimiento, R.string.admin_nav_config)
)

@Composable
fun AdminBottomNavigationBar(
    currentScreen: String,
    onNavigate: (String) -> Unit,
    hasNewBreakRequests: Boolean = false
) {
    // Iconos de color: algo mayores en tablets grandes
    val iconSize = DimensionUtils.adaptiveDimen(
        phone = 26.dp,
        smallTablet = 28.dp,
        largeTablet = 30.dp
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        adminNavItems.forEach { item ->
            val label = stringResource(item.labelRes)
            val selected = currentScreen == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                // 6 secciones no caben con etiqueta en móvil: solo la activa la muestra (patrón M3)
                alwaysShowLabel = false,
                icon = {
                    val icon = @Composable {
                        Icon(
                            painter = painterResource(id = item.iconRes),
                            contentDescription = if (selected) null else label,
                            modifier = Modifier.size(iconSize),
                            tint = Color.Unspecified
                        )
                    }
                    // La ruta real es "break_requests" (antes se comparaba con "breaks" y el badge nunca salía)
                    if (item.route == "break_requests" && hasNewBreakRequests) {
                        BadgedBox(badge = { Badge() }) { icon() }
                    } else {
                        icon()
                    }
                },
                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}
