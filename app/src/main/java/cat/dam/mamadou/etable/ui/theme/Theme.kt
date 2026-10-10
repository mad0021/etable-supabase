package cat.dam.mamadou.etable.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.util.Log

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF388E3C),
    secondary = Color(0xFF689F38),
    tertiary = Color(0xFF4CAF50),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFEEEEEE)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF8BC34A),
    tertiary = Color(0xFF66BB6A),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF333333),
    onSurface = Color(0xFF333333)
)

/**
 * Tema raíz Material 3 Expressive. Disponible para todas las pantallas:
 * - MaterialTheme.motionScheme: expressive() (spring con rebote) o standard() si "Quitar animaciones".
 *   Usar fastSpatialSpec()/defaultSpatialSpec() para posición/escala y *EffectsSpec() para alpha/color.
 * - rememberReduceMotion() (Motion.kt): true si el sistema tiene las animaciones desactivadas.
 * - MaterialTheme.shapes = ExpressiveShapes (small 12, medium 20, large 28, extraLarge 40).
 * - MaterialTheme.typography = Typography (display/headline en Bold/ExtraBold).
 * - TableStatusColors (Color.kt): colores de estado de mesa, contraste >= 4.5:1. No cambiar.
 * - Componentes Expressive (@OptIn(ExperimentalMaterial3ExpressiveApi::class)): ButtonDefaults.shapes(),
 *   LoadingIndicator, ContainedLoadingIndicator, ButtonGroup, ToggleButton, FloatingToolbar...
 * - ui/components/ExpressiveNotificationVisibility: entrada/salida estándar para avisos importantes.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EtableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    currentTheme: AppTheme = ThemeManager.getTheme(LocalContext.current),
    content: @Composable () -> Unit
) {
    Log.d("EtableTheme", "⚡ APLICANDO TEMA EN COMPOSABLE: $currentTheme")
    
    // Simplificar la lógica eliminando SYSTEM
    val colorScheme = when (currentTheme) {
        AppTheme.DARK -> DarkColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.OCEAN -> lightColorScheme(
            primary = Color(0xFF0277BD),
            secondary = Color(0xFF4FC3F7),
            tertiary = Color(0xFF0288D1),
            background = Color(0xFFE1F5FE),
            surface = Color(0xFFE1F5FE),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF01579B),
            onSurface = Color(0xFF01579B)
        )
        AppTheme.SUNSET -> lightColorScheme(
            primary = Color(0xFFE64A19),
            secondary = Color(0xFFFFB74D),
            tertiary = Color(0xFFFF7043),
            background = Color(0xFFFBE9E7),
            surface = Color(0xFFFBE9E7),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFFBF360C),
            onSurface = Color(0xFFBF360C)
        )
        AppTheme.NATURE -> lightColorScheme(
            primary = Color(0xFF689F38),
            secondary = Color(0xFFAED581),
            tertiary = Color(0xFF8BC34A),
            background = Color(0xFFF1F8E9),
            surface = Color(0xFFF1F8E9),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF33691E),
            onSurface = Color(0xFF33691E)
        )
        AppTheme.PURPLE -> lightColorScheme(
            primary = Color(0xFF7B1FA2),
            secondary = Color(0xFFCE93D8),
            tertiary = Color(0xFF9C27B0),
            background = Color(0xFFF3E5F5),
            surface = Color(0xFFF3E5F5),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF4A148C),
            onSurface = Color(0xFF4A148C)
        )
        AppTheme.MONOCHROME -> lightColorScheme(
            primary = Color(0xFF424242),
            secondary = Color(0xFF9E9E9E),
            tertiary = Color(0xFF616161),
            background = Color(0xFFFAFAFA),
            surface = Color(0xFFFAFAFA),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF212121),
            onSurface = Color(0xFF212121)
        )
        AppTheme.AUTUMN -> lightColorScheme(
            primary = Color(0xFFE65100),
            secondary = Color(0xFFFFA726),
            tertiary = Color(0xFFD84315),
            background = Color(0xFFFFF8E1),
            surface = Color(0xFFFFF8E1),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF5D4037),
            onSurface = Color(0xFF5D4037)
        )
        AppTheme.VINTAGE -> lightColorScheme(
            primary = Color(0xFF795548),
            secondary = Color(0xFFBCAAA4),
            tertiary = Color(0xFF8D6E63),
            background = Color(0xFFF5F5DC),
            surface = Color(0xFFF5F5DC),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF4E342E),
            onSurface = Color(0xFF4E342E)
        )
        AppTheme.FIRE -> lightColorScheme(
            primary = Color(0xFFD50000),
            secondary = Color(0xFFFF5722),
            tertiary = Color(0xFFFF8A65),
            background = Color(0xFFFFF3E0),
            surface = Color(0xFFFFF3E0),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFFB71C1C),
            onSurface = Color(0xFFB71C1C)
        )
        AppTheme.MIDNIGHT -> darkColorScheme(
            primary = Color(0xFF1A237E),
            secondary = Color(0xFF3949AB),
            tertiary = Color(0xFF5C6BC0),
            background = Color(0xFF121212),
            surface = Color(0xFF121212),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFFE0E0E0),
            onSurface = Color(0xFFE0E0E0)
        )
        AppTheme.FOREST -> lightColorScheme(
            primary = Color(0xFF1B5E20),
            secondary = Color(0xFF4CAF50),
            tertiary = Color(0xFF66BB6A),
            background = Color(0xFFE8F5E9),
            surface = Color(0xFFE8F5E9),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onTertiary = Color.White,
            onBackground = Color(0xFF1B5E20),
            onSurface = Color(0xFF1B5E20)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = if (rememberReduceMotion()) MotionScheme.standard() else MotionScheme.expressive(),
        shapes = ExpressiveShapes,
        typography = Typography,
        content = content
    )
}