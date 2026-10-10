// c:\Users\AA\AndroidStudioProjects\etable\app\src\main\java\cat\dam\mamadou\etable\ui\screens\admin\screens\AdminConfigScreen.kt

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.admin.screens

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminLoading
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminSwitch
import cat.dam.mamadou.etable.ui.screens.admin.components.adminAnimateItem
import cat.dam.mamadou.etable.ui.screens.admin.components.rememberAdminReduceMotion
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.R.*
import cat.dam.mamadou.etable.ui.components.TextSizeAdjuster
import cat.dam.mamadou.etable.ui.screens.waiter.components.config.LanguageOption
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.ui.theme.AppTheme
import cat.dam.mamadou.etable.ui.theme.ThemeManager
import cat.dam.mamadou.etable.ui.theme.isSystemInDarkTheme
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.LocaleManager
import cat.dam.mamadou.etable.utils.ThemeHelper
import cat.dam.mamadou.etable.supabase.FirebaseUser
import java.util.Locale

// Lista de idiomas disponibles
private val idiomes = listOf(
    Pair("ESPAÑOL", Locale("es", "ES")),
    Pair("CATALÀ", Locale("ca", "ES")),
    Pair("ENGLISH", Locale("en", "US"))
)

// Efecto para escuchar notificaciones de descanso pero sin respuesta visual
@Composable
private fun UseBreakSoundOnlyListener(firebaseManager: FirebaseManager) {
    DisposableEffect(Unit) {
        // Asegurarse de que las notificaciones visuales están desactivadas
        BreakNotificationManager.disableModalNotifications()
        
        // El listener para solicitudes de descanso ya está configurado en AdminDashboardActivity
        // y se encarga del sonido y la vibración
        
        onDispose { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminConfigScreen(
    onTextSizeChanged: (Float) -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onLanguageChanged: (Locale) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val firebaseManager = remember { FirebaseManager() }
    val scrollState = rememberScrollState()
    
    // Usar nuestro efecto personalizado que no devuelve un estado visual
    UseBreakSoundOnlyListener(firebaseManager)

    // Estado para el tema oscuro
    var darkThemeEnabled by remember { 
        mutableStateOf(ThemeManager.isDarkMode(context))
    }

    // Obtener los strings fuera del bloque remember
    val spanishName = stringResource(R.string.language_name_spanish)
    val catalanName = stringResource(R.string.language_name_catalan)
    val englishName = stringResource(R.string.language_name_english)

    // Ahora podemos usar estos valores dentro de remember
    val idiomasTraducidos = remember(spanishName, catalanName, englishName) {
        listOf(
            Pair(spanishName, Locale("es", "ES")),
            Pair(catalanName, Locale("ca", "ES")),
            Pair(englishName, Locale("en", "US"))
        )
    }
    
    // Estado para el idioma seleccionado - usar la lista traducida
    var idiomaSeleccionat by remember { 
        val currentLocale = context.resources.configuration.locales[0]
        mutableStateOf(idiomasTraducidos.find { it.second.language == currentLocale.language } ?: idiomasTraducidos[0])
    }

    // Definir la función de cambio de idioma como función local
    fun switchLanguage(context: Context, locale: Locale, displayName: String) {
        // Actualizar la selección visual
        idiomaSeleccionat = Pair(displayName, locale)
        
        try {
            // 1. Establecer como locale predeterminado
            Locale.setDefault(locale)
            
            // 2. Guardar en preferencias
            val prefs = context.getSharedPreferences(LocaleManager.PREFS_LANGUAGE, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(LocaleManager.KEY_SELECTED_LANGUAGE, locale.language)
                putString(LocaleManager.KEY_SELECTED_COUNTRY, locale.country)
                commit()
            }
            
            Log.d("AdminConfigScreen", "🌍 Guardando idioma: ${locale.language}_${locale.country}")

            // 3. Actualizar configuración en TODOS los niveles
            // A nivel de aplicación
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
            
            // A nivel de recursos actuales
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            
            // A nivel de contexto de aplicación
            val appContext = context.applicationContext
            val appConfig = Configuration(appContext.resources.configuration)
            appConfig.setLocale(locale)
            appContext.resources.updateConfiguration(appConfig, appContext.resources.displayMetrics)
            
            // 4. Notificar al componente padre
            onLanguageChanged(locale)
            
            // 5. Recrear la actividad después de un breve retraso
            if (context is Activity) {
                val message = when(locale.language) {
                    "es" -> "Cambiando a Español..."
                    "ca" -> "Canviant a Català..."
                    "en" -> "Changing to English..."
                    else -> "Changing language..."
                }
                
                // Guardar una bandera que indique que la actividad se está recreando
                // para un cambio de idioma específico
                val flagPrefs = context.getSharedPreferences("language_change_flags", Context.MODE_PRIVATE)
                flagPrefs.edit().apply {
                    putString("pending_language", locale.language)
                    putString("pending_country", locale.country)
                    commit()
                }
                
                Handler(Looper.getMainLooper()).postDelayed({
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    // Y ahora recrear utilizando LocaleManager
                    LocaleManager.recreateActivity(context)
                }, 500)
            }
        } catch (e: Exception) {
            val errorMsg = when(locale.language) {
                "es" -> "Error cambiando a español: ${e.message}"
                "ca" -> "Error canviant a català: ${e.message}"
                "en" -> "Error changing to English: ${e.message}" 
                else -> "Error changing language: ${e.message}"
            }
            Log.e("AdminConfigScreen", errorMsg, e)
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // Estado para el tema seleccionado - inicializar con el actual
    var selectedTheme by remember { 
        mutableStateOf<AppTheme>(ThemeManager.getTheme(context))
    }

    val availableThemes = remember {
        listOf(
            AppTheme.LIGHT,
            AppTheme.DARK,
            AppTheme.OCEAN,
            AppTheme.SUNSET,
            AppTheme.NATURE,
            AppTheme.PURPLE,
            AppTheme.MONOCHROME,
            AppTheme.AUTUMN,
            AppTheme.VINTAGE,
            AppTheme.FIRE,
            AppTheme.MIDNIGHT,
            AppTheme.FOREST
        )
    }

    // Obtener el usuario actual
    val currentUser = remember { firebaseManager.authManager.getCurrentUser() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Centrar el contenido con ancho limitado
                    .align(Alignment.TopCenter)
                    .verticalScroll(scrollState)
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {
                // Perfil mejorado
                ProfileSection(currentUser)
                
                Spacer(modifier = Modifier.height(24.dp))

                // Sección de configuraciones mejorada
                ConfigCard {
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    // TEMAS
                    ConfigHeader(
                        icon = Icons.Default.Palette,
                        title = stringResource(R.string.config_theme_section)
                    )
                    
                    availableThemes.forEach { theme ->
                        ThemeOptionImproved(
                            theme = theme,
                            selected = selectedTheme == theme,
                            onClick = {
                                // Código existente para cambiar el tema
                                selectedTheme = theme
                                val isDark = when (theme) {
                                    AppTheme.DARK -> true
                                    AppTheme.LIGHT -> false
                                    AppTheme.OCEAN -> false
                                    AppTheme.SUNSET -> false
                                    AppTheme.NATURE -> false
                                    AppTheme.PURPLE -> false
                                    AppTheme.MONOCHROME -> false
                                    AppTheme.AUTUMN -> false
                                    AppTheme.VINTAGE -> false
                                    AppTheme.FIRE -> false
                                    AppTheme.MIDNIGHT -> true
                                    AppTheme.FOREST -> false
                                }
                                
                                ThemeManager.applyTheme(context, theme, isDark)
                                onThemeChanged(isDark)
                                darkThemeEnabled = isDark
                                
                                // Obtener los mensajes de retroalimentación del cambio de tema
                                val feedbackMessage = when (theme) {
                                    AppTheme.DARK -> context.getString(R.string.theme_applied_dark)
                                    AppTheme.LIGHT -> context.getString(R.string.theme_applied_light)
                                    AppTheme.OCEAN -> context.getString(R.string.theme_applied_ocean)
                                    AppTheme.SUNSET -> context.getString(R.string.theme_applied_sunset)
                                    AppTheme.NATURE -> context.getString(R.string.theme_applied_nature)
                                    AppTheme.PURPLE -> context.getString(R.string.theme_applied_purple)
                                    AppTheme.MONOCHROME -> context.getString(R.string.theme_applied_monochrome)
                                    AppTheme.AUTUMN -> context.getString(R.string.theme_applied_autumn)
                                    AppTheme.VINTAGE -> context.getString(R.string.theme_applied_vintage)
                                    AppTheme.FIRE -> context.getString(R.string.theme_applied_fire)
                                    AppTheme.MIDNIGHT -> context.getString(R.string.theme_applied_midnight)
                                    AppTheme.FOREST -> context.getString(R.string.theme_applied_forest)
                                }
                                Toast.makeText(context, feedbackMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    
                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    
                    // IDIOMA
                    ConfigHeader(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.config_language_section)
                    )

                    // Implementación unificada para el idioma español
                    LanguageOption(
                        name = stringResource(R.string.config_language_spanish),
                        selected = idiomaSeleccionat.second.language == "es",
                        onClick = { switchLanguage(context, Locale("es", "ES"), spanishName) },
                        contentDescription = stringResource(R.string.switch_language_icon_description)
                    )

                    // Implementación unificada para el idioma catalán
                    LanguageOption(
                        name = stringResource(R.string.config_language_catalan),
                        selected = idiomaSeleccionat.second.language == "ca",
                        onClick = { switchLanguage(context, Locale("ca", "ES"), catalanName) },
                        contentDescription = stringResource(R.string.switch_language_icon_description)
                    )

                    // Implementación unificada para inglés
                    LanguageOption(
                        name = stringResource(R.string.config_language_english),
                        selected = idiomaSeleccionat.second.language == "en",
                        onClick = { switchLanguage(context, Locale("en", "US"), englishName) },
                        contentDescription = stringResource(R.string.switch_language_icon_description)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Botón de cerrar sesión mejorado
                LogoutButton(onLogout = onLogout)
            }
        }
    }
}

@Composable
private fun ProfileSection(currentUser: FirebaseUser?) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar mejorado
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                            )
                        ),
                        shape = MaterialShapes.Cookie9Sided.toShape()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Nombre mejorado
                Text(
                    text = currentUser?.displayName ?: 
                          currentUser?.email?.substringBefore('@')?.replaceFirstChar { it.uppercase() } ?: 
                          "Admin",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Email mejorado
                Text(
                    text = currentUser?.email ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Etiqueta mejorada
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        // Reemplazar texto hardcodeado del rol
                        Text(
                            text = stringResource(R.string.admin_config_role_label),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun ConfigHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ThemeOptionImproved(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Usar ThemeHelper para obtener nombres y descripciones traducidos
    val themeName = ThemeHelper.getThemeName(context, theme)
    val themeDescription = ThemeHelper.getThemeDescription(context, theme)
    
    val backgroundColor = Color(theme.backgroundColor.value)
    val icon = when (theme) {
        AppTheme.LIGHT -> Icons.Default.LightMode
        AppTheme.DARK -> Icons.Default.DarkMode
        AppTheme.OCEAN -> Icons.Default.Waves
        AppTheme.SUNSET -> Icons.Default.WbSunny
        AppTheme.NATURE -> Icons.Default.Nature
        AppTheme.PURPLE -> Icons.Default.Palette
        AppTheme.MONOCHROME -> Icons.Default.Contrast
        AppTheme.AUTUMN -> Icons.Default.Park
        AppTheme.VINTAGE -> Icons.Default.Camera
        AppTheme.FIRE -> Icons.Default.Whatshot
        AppTheme.MIDNIGHT -> Icons.Default.NightsStay
        AppTheme.FOREST -> Icons.Default.Forest
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Estado seleccionado
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Icono del tema
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (theme) {
                        AppTheme.LIGHT -> Color(0xFFFFA000)
                        AppTheme.DARK -> Color(0xFF5C6BC0) 
                        AppTheme.OCEAN -> Color(0xFF0288D1)
                        AppTheme.SUNSET -> Color(0xFFD84315)
                        AppTheme.NATURE -> Color(0xFF689F38)
                        AppTheme.PURPLE -> Color(0xFF7B1FA2)
                        AppTheme.MONOCHROME -> Color(0xFF424242)
                        AppTheme.AUTUMN -> Color(0xFFE65100)
                        AppTheme.VINTAGE -> Color(0xFF795548)
                        AppTheme.FIRE -> Color(0xFFD50000)
                        AppTheme.MIDNIGHT -> Color(0xFF1A237E)
                        AppTheme.FOREST -> Color(0xFF1B5E20)
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Información del tema
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = themeName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                Text(
                    text = themeDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionImproved(
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val languageData = when (name) {
        stringResource(R.string.language_name_spanish) -> Triple(
            stringResource(R.string.language_code_spanish), 
            stringResource(R.string.language_country_spanish), 
            Icons.Default.EmojiFlags
        )
        stringResource(R.string.language_name_catalan) -> Triple(
            stringResource(R.string.language_code_catalan), 
            stringResource(R.string.language_country_catalan), 
            Icons.Default.EmojiFlags
        ) 
        stringResource(R.string.language_name_english) -> Triple(
            stringResource(R.string.language_code_english), 
            stringResource(R.string.language_country_english), 
            Icons.Default.EmojiFlags
        )
        else -> Triple("", "", Icons.Default.Language)
    }
    
    
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (selected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected)
                MaterialTheme.colorScheme.secondary
            else
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador visual
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.surface
                    )
                    .border(
                        width = 2.dp,
                        color = if (selected)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Icono/bandera del idioma
            Surface(
                shape = CircleShape,
                color = if (selected)
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = languageData.first,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        ),
                        color = if (selected)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (selected)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = languageData.second,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    ElevatedButton(
        shapes = ButtonDefaults.shapes(),
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.ExitToApp,
            contentDescription = stringResource(R.string.admin_config_logout_description),
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            stringResource(R.string.admin_config_logout),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
        )
    }
}
