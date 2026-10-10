// Crear un archivo nuevo: utils/OrientationUtils.kt
package cat.dam.mamadou.etable.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration

@Composable
fun rememberIsLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return remember(configuration.orientation) {
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}

@Composable
fun rememberScreenSize(): Pair<Int, Int> {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        Pair(configuration.screenWidthDp, configuration.screenHeightDp)
    }
}