package cat.dam.mamadou.etable.ui.theme

import android.app.Activity
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Un wrapper para EtableTheme que observa automáticamente los cambios de tema global
 */
@Composable
fun ETableThemeWrapper(content: @Composable () -> Unit) {
    val context = LocalContext.current
    
    // Estado para el tema actual y modo oscuro
    var currentTheme by remember { mutableStateOf(ThemeManager.getTheme(context)) }
    var isDarkMode by remember { mutableStateOf(ThemeManager.isDarkMode(context)) }
    
    // Observar cambios de tema
    LaunchedEffect(Unit) {
        ThemeManager.themeChanges.collect { pair ->
            currentTheme = pair.first
            isDarkMode = pair.second
        }
    }
    
    // Aplicar el tema actual
    Log.d("ETableThemeWrapper", "🌈 Aplicando tema: $currentTheme, isDarkMode: $isDarkMode")
    EtableTheme(darkTheme = isDarkMode, currentTheme = currentTheme) {
        content()
    }
    
    // Efecto para actualizar la barra de estado
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(currentTheme, isDarkMode) {
            val window = (context as Activity).window
            window.statusBarColor = getStatusBarColorForTheme(currentTheme).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
            
            onDispose {}
        }
    }
}

// Función auxiliar para obtener el color de la barra de estado
private fun getStatusBarColorForTheme(theme: AppTheme): androidx.compose.ui.graphics.Color {
    return when (theme) {
        AppTheme.OCEAN -> androidx.compose.ui.graphics.Color(0xFF0277BD)
        AppTheme.SUNSET -> androidx.compose.ui.graphics.Color(0xFFE64A19)
        AppTheme.NATURE -> androidx.compose.ui.graphics.Color(0xFF689F38)
        AppTheme.PURPLE -> androidx.compose.ui.graphics.Color(0xFF7B1FA2)
        AppTheme.MONOCHROME -> androidx.compose.ui.graphics.Color(0xFF424242)
        AppTheme.AUTUMN -> androidx.compose.ui.graphics.Color(0xFFE65100)
        AppTheme.VINTAGE -> androidx.compose.ui.graphics.Color(0xFF795548)
        AppTheme.FIRE -> androidx.compose.ui.graphics.Color(0xFFD50000)
        AppTheme.MIDNIGHT -> androidx.compose.ui.graphics.Color(0xFF1A237E)
        AppTheme.FOREST -> androidx.compose.ui.graphics.Color(0xFF1B5E20)
        AppTheme.DARK -> androidx.compose.ui.graphics.Color(0xFF388E3C)
        AppTheme.LIGHT -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
    }
}