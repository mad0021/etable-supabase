package cat.dam.mamadou.etable.ui.screens.waiter.components.config

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.theme.AppTheme

@Composable
fun ThemeOption(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Obtener el nombre del tema usando una traducción directa en lugar del método
    val themeName = when (theme) {
        AppTheme.LIGHT -> stringResource(R.string.theme_light)
        AppTheme.DARK -> stringResource(R.string.theme_dark)
        AppTheme.OCEAN -> stringResource(R.string.theme_ocean)
        AppTheme.SUNSET -> stringResource(R.string.theme_sunset)
        AppTheme.NATURE -> stringResource(R.string.theme_nature)
        AppTheme.PURPLE -> stringResource(R.string.theme_purple)
        AppTheme.MONOCHROME -> stringResource(R.string.theme_monochrome)
        AppTheme.AUTUMN -> stringResource(R.string.theme_autumn)
        AppTheme.VINTAGE -> stringResource(R.string.theme_vintage)
        AppTheme.FIRE -> stringResource(R.string.theme_fire)
        AppTheme.MIDNIGHT -> stringResource(R.string.theme_midnight)
        AppTheme.FOREST -> stringResource(R.string.theme_forest)
    }
    
    // Determinar el color según el tema
    val themeColor = when (theme) {
        AppTheme.LIGHT -> Color(0xFF4CAF50)
        AppTheme.DARK -> Color(0xFF388E3C)
        AppTheme.OCEAN -> Color(0xFF0277BD)
        AppTheme.SUNSET -> Color(0xFFE64A19)
        AppTheme.NATURE -> Color(0xFF689F38)
        AppTheme.PURPLE -> Color(0xFF7B1FA2)
        AppTheme.MONOCHROME -> Color(0xFF424242)
        AppTheme.AUTUMN -> Color(0xFFE65100)
        AppTheme.VINTAGE -> Color(0xFF795548)
        AppTheme.FIRE -> Color(0xFFD50000)
        AppTheme.MIDNIGHT -> Color(0xFF1A237E)
        AppTheme.FOREST -> Color(0xFF1B5E20)
    }

    // Determinar el icono según el tema
    val icon = when (theme) {
        AppTheme.LIGHT -> Icons.Default.LightMode
        AppTheme.DARK -> Icons.Default.DarkMode
        AppTheme.OCEAN -> Icons.Default.Water
        AppTheme.SUNSET -> Icons.Default.WbTwilight
        AppTheme.NATURE -> Icons.Default.Landscape
        AppTheme.PURPLE -> Icons.Default.ColorLens
        AppTheme.MONOCHROME -> Icons.Default.FilterBAndW
        AppTheme.AUTUMN -> Icons.Default.FilterVintage
        AppTheme.VINTAGE -> Icons.Default.History
        AppTheme.FIRE -> Icons.Default.LocalFireDepartment
        AppTheme.MIDNIGHT -> Icons.Default.NightlightRound
        AppTheme.FOREST -> Icons.Default.Forest
    }
    
    // Descripción para accesibilidad
    val accessibilityDescription = if (selected) {
        stringResource(R.string.theme_option_selected, themeName)
    } else {
        stringResource(R.string.theme_option_not_selected, themeName)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics { 
                contentDescription = accessibilityDescription 
            },
        color = if (selected) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
            else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo de color para el tema con icono
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(themeColor)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (theme == AppTheme.DARK) Color.White else Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = themeName,  // Usar el nombre traducido
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected_icon_description),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}