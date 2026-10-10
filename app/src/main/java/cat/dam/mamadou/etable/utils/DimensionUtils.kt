package cat.dam.mamadou.etable.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

object DimensionUtils {
    // Función para determinar si estamos en modo horizontal
    @Composable
    fun isLandscape(context: Context): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }

    // Función para determinar si estamos en una tablet pequeña
    @Composable
    fun isSmallTablet(): Boolean {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        return screenWidthDp >= 600 && screenWidthDp < 840
    }
    
    // Función para determinar si estamos en una tablet grande
    @Composable
    fun isLargeTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= 840
    }
    
    // Función para dimensiones adaptativas (Dp)
    @Composable
    fun adaptiveDimen(phone: Dp, smallTablet: Dp, largeTablet: Dp): Dp {
        return when {
            isLargeTablet() -> largeTablet
            isSmallTablet() -> smallTablet
            else -> phone
        }
    }
    
    // Función para tamaños de texto adaptativos (TextUnit)
    @Composable
    fun adaptiveTextSize(phone: TextUnit, smallTablet: TextUnit, largeTablet: TextUnit): TextUnit {
        return when {
            isLargeTablet() -> largeTablet
            isSmallTablet() -> smallTablet
            else -> phone
        }
    }
}
