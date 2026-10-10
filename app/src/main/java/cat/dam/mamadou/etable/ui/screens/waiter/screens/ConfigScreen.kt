@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import cat.dam.mamadou.etable.ui.screens.waiter.components.orders.connectedShapes

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.components.TextSizeAdjuster
import cat.dam.mamadou.etable.ui.theme.AppTheme
import cat.dam.mamadou.etable.ui.theme.ThemeManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.LocaleManager
import cat.dam.mamadou.etable.utils.SessionManager
import cat.dam.mamadou.etable.utils.TTSManager
import cat.dam.mamadou.etable.utils.UserConfigManager
import cat.dam.mamadou.etable.BuildConfig
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    navController: NavController,
    onTextSizeChanged: (Float) -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    onLanguageChanged: (Locale) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }
    
    // Estados existentes...
    var ttsEnabled by remember { mutableStateOf(ttsManager.isEnabled()) }

    val scope = rememberCoroutineScope()
    val firebaseManager = remember { FirebaseManager() }
    val sessionManager = remember { SessionManager(context) }
    val scrollState = rememberScrollState()

    // NUEVO: Estados para permisos del usuario
    var userPermissions by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var isLoadingPermissions by remember { mutableStateOf(true) }

    // NUEVO: Escuchar cambios en tiempo real de permisos
    val permissionsUpdated by UserConfigManager.permissionsUpdated.collectAsStateWithLifecycle()

    // Cargar permisos del usuario al iniciar
    LaunchedEffect(Unit) {
        val userId = sessionManager.getUserId()
        if (userId.isNotEmpty()) {
            // Iniciar escucha en tiempo real
            UserConfigManager.startListening(userId)
            
            // Cargar permisos iniciales
            firebaseManager.userManager.getUserConfig(userId).fold(
                onSuccess = { config ->
                    userPermissions = config
                    isLoadingPermissions = false
                    Log.d("ConfigScreen", "✅ Permisos iniciales cargados: $config")
                },
                onFailure = { error ->
                    Log.e("ConfigScreen", "❌ Error cargando permisos: ${error.message}")
                    isLoadingPermissions = false
                }
            )
        }
    }

    // NUEVO: Reaccionar a cambios de permisos
    LaunchedEffect(permissionsUpdated) {
        if (permissionsUpdated) {
            Log.d("ConfigScreen", "🔄 Permisos actualizados detectados")
            val updatedPermissions = UserConfigManager.lastPermissions
            userPermissions = updatedPermissions
            UserConfigManager.resetUpdatedFlag()
            Log.d("ConfigScreen", "📱 Permisos actualizados aplicados: $updatedPermissions")
        }
    }

    // Limpiar listener al salir
    DisposableEffect(Unit) {
        onDispose {
            UserConfigManager.stopListening()
        }
    }

    // Funciones helper para verificar permisos
    fun hasPermission(permission: String, default: Boolean = true): Boolean {
        val result = userPermissions[permission] as? Boolean ?: default
        Log.d("ConfigScreen", "🔍 Verificando permiso '$permission': $result (default: $default)")
        return result
    }

    fun getAllowedThemes(): List<String> {
        val themes = userPermissions["allowedThemes"] as? List<*>
        val result = themes?.filterIsInstance<String>() ?: emptyList()
        Log.d("ConfigScreen", "🎨 Temas permitidos: $result")
        return result
    }

    // CORREGIDO: Función para obtener idiomas permitidos
    fun getAllowedLanguages(): List<String> {
        val languages = userPermissions["allowedLanguages"] as? List<*>
        val result = languages?.filterIsInstance<String>() ?: emptyList()
        Log.d("ConfigScreen", "🌍 Idiomas permitidos desde Firebase: $result")
        return result
    }

    // Estados de configuración
    var darkThemeEnabled by remember { 
        mutableStateOf(ThemeManager.isDarkMode(context))
    }

    // Obtener los strings fuera del bloque remember
    val spanishName = stringResource(R.string.language_name_spanish)
    val catalanName = stringResource(R.string.language_name_catalan)
    val englishName = stringResource(R.string.language_name_english)

    // Lista de idiomas traducidos
    val idiomasTraducidos = remember(spanishName, catalanName, englishName) {
        listOf(
            Pair(spanishName, Locale("es", "ES")),
            Pair(catalanName, Locale("ca", "ES")),
            Pair(englishName, Locale("en", "US"))
        )
    }
    
    // Estado para el idioma seleccionado
    var idiomaSeleccionat by remember { 
        val currentLocale = context.resources.configuration.locales[0]
        mutableStateOf(idiomasTraducidos.find { it.second.language == currentLocale.language } ?: idiomasTraducidos[0])
    }

    // Estado para el tema seleccionado
    var selectedTheme by remember { 
        mutableStateOf<AppTheme>(ThemeManager.getTheme(context))
    }

    // Estados para solicitud de descanso
    var isBreakDialogVisible by remember { mutableStateOf(false) }
    var isSendingBreakRequest by remember { mutableStateOf(false) }

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

    // CORREGIDO: Filtrar temas disponibles según permisos
    val filteredThemes = remember(userPermissions) {
        val allowedThemes = getAllowedThemes()
        Log.d("ConfigScreen", "🎨 Procesando temas permitidos: $allowedThemes")
        
        if (allowedThemes.isNotEmpty()) {
            val filtered = availableThemes.filter { theme ->
                val isAllowed = allowedThemes.contains(theme.name)
                Log.d("ConfigScreen", "🔍 Tema ${theme.name}: $isAllowed")
                isAllowed
            }
            Log.d("ConfigScreen", "✅ Temas filtrados finales: ${filtered.map { it.name }}")
            filtered
        } else {
            // CAMBIO: Si no hay temas específicos permitidos, mostrar SOLO los básicos
            Log.d("ConfigScreen", "⚠️ No hay temas específicos permitidos, mostrando básicos")
            listOf(AppTheme.LIGHT, AppTheme.DARK)
        }
    }

    // CORREGIDO: Filtrar idiomas disponibles según permisos
    val filteredLanguages = remember(userPermissions) {
        val allowedLanguages = getAllowedLanguages()
        Log.d("ConfigScreen", "🌍 Procesando idiomas permitidos: $allowedLanguages")
        
        if (allowedLanguages.isNotEmpty()) {
            val filtered = idiomasTraducidos.filter { (_, locale) ->
                val isAllowed = allowedLanguages.contains(locale.language)
                Log.d("ConfigScreen", "🔍 Idioma ${locale.language}: $isAllowed")
                isAllowed
            }
            Log.d("ConfigScreen", "✅ Idiomas filtrados finales: ${filtered.map { it.second.language }}")
            filtered
        } else {
            // CAMBIO: Si no hay idiomas específicos permitidos, mostrar SOLO español
            Log.d("ConfigScreen", "⚠️ No hay idiomas específicos permitidos, mostrando solo español")
            listOf(Pair(spanishName, Locale("es", "ES")))
        }
    }

    // Función para cambiar idioma (igual que en AdminConfigScreen)
    fun switchLanguage(context: Context, locale: Locale, displayName: String) {
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
            
            Log.d("ConfigScreen", "🌍 Guardando idioma: ${locale.language}_${locale.country}")

            // 3. NUEVO: Sincronizar TTS antes de cambiar UI
            ttsManager.updateLanguage(locale)
            
            // 4. Actualizar configuración de la aplicación
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
            
            // 5. Mensaje de confirmación localizado
            val message = when(locale.language) {
                "es" -> "Cambiando a español..."
                "ca" -> "Canviant a català..."
                "en" -> "Switching to English..."
                else -> "Changing language..."
            }
            
            // 6. TTS del cambio de idioma en el nuevo idioma
            if (ttsManager.isEnabled()) {
                ttsManager.speakNavigationAction("config")
            }
            
            // 7. Recrear actividad con delay
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                LocaleManager.recreateActivity(context)
            }, 500)
            
        } catch (e: Exception) {
            val errorMsg = when(locale.language) {
                "es" -> "Error cambiando a español: ${e.message}"
                "ca" -> "Error canviant a català: ${e.message}"
                "en" -> "Error changing to English: ${e.message}" 
                else -> "Error changing language: ${e.message}"
            }
            Log.e("ConfigScreen", errorMsg, e)
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // Función para enviar solicitud de descanso
    fun sendBreakRequest(reason: String) {
        scope.launch {
            isSendingBreakRequest = true
            
            try {
                val userId = sessionManager.getUserId()
                val userName = sessionManager.getUserEmail()?.substringBefore("@") ?: "Usuario"
                
                val breakRequest = cat.dam.mamadou.etable.utils.firebase.BreakRequest(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    userName = userName,
                    fullName = userName,
                    content = reason,
                    reason = reason,
                    timestamp = Date(),
                    viewed = false,
                    status = "pending"
                )
                
                val result = firebaseManager.breakManager.sendBreakRequest(breakRequest)
                
                if (result.isSuccess) {
                    Toast.makeText(context, context.getString(R.string.break_request_sent_success), Toast.LENGTH_SHORT).show()
                    isBreakDialogVisible = false
                } else {
                    Toast.makeText(context, context.getString(R.string.break_request_sent_error), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ConfigScreen", "Error enviando solicitud de descanso: ${e.message}", e)
                Toast.makeText(context, context.getString(R.string.break_request_sent_error), Toast.LENGTH_SHORT).show()
            } finally {
                isSendingBreakRequest = false
            }
        }
    }

    // Obtener el usuario actual
    val currentUser = remember { firebaseManager.authManager.getCurrentUser() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.config_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        if (isLoadingPermissions) {
            // Mostrar indicador de carga mientras se obtienen los permisos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LoadingIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.loading_user_permissions),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.TopCenter)
                        .verticalScroll(scrollState)
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                    // Perfil del camarero
                    WaiterProfileSection(currentUser)
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón de solicitud de descanso (solo si tiene permiso)
                    if (hasPermission("canRequestBreak", true)) {
                        BreakRequestButton(
                            onClick = { isBreakDialogVisible = true },
                            isLoading = isSendingBreakRequest
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // DEBUG: Mostrar información de permisos (añadir esta sección antes de ConfigCard)
                    if (BuildConfig.DEBUG) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Yellow.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("🐛 DEBUG - Información completa:", fontWeight = FontWeight.Bold)
                                Text("👤 Usuario ID: ${sessionManager.getUserId()}")
                                Text("🔒 allowedThemes en Firebase: ${getAllowedThemes()}")
                                Text("🎨 filteredThemes calculados: ${filteredThemes.map { it.name }}")
                                Text("🌍 allowedLanguages en Firebase: ${getAllowedLanguages()}")
                                Text("🗣️ filteredLanguages calculados: ${filteredLanguages.map { it.second.language }}")
                                Text("✅ canChangeTheme: ${hasPermission("canChangeTheme", true)}")
                                Text("🗣️ canChangeLanguage: ${hasPermission("canChangeLanguage", true)}")
                                Text("📄 Todos los permisos: $userPermissions")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sección de configuraciones
                    ConfigCard {
                        var hasAnyOption = false

                        // TEMAS (solo si puede cambiar tema Y hay temas disponibles)
                        if (hasPermission("canChangeTheme", true) && filteredThemes.isNotEmpty()) {
                            hasAnyOption = true
                            
                            ConfigHeader(
                                icon = Icons.Default.Palette,
                                title = stringResource(R.string.config_theme_section)
                            )
                            
                            filteredThemes.forEach { theme ->
                                ThemeOptionImproved(
                                    theme = theme,
                                    selected = selectedTheme == theme,
                                    onClick = {
                                        selectedTheme = theme
                                        val isDark = when (theme) {
                                            AppTheme.DARK -> true
                                            AppTheme.MIDNIGHT -> true
                                            else -> false
                                        }
                                        ThemeManager.saveTheme(context, theme, isDark)
                                        onThemeChanged(isDark)
                                    }
                                )
                            }
                        }
                        
                        // IDIOMA (solo si puede cambiar idioma Y hay idiomas permitidos)
                        if (hasPermission("canChangeLanguage", true) && filteredLanguages.isNotEmpty()) {
                            if (hasAnyOption) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                            hasAnyOption = true
                            
                            ConfigHeader(
                                icon = Icons.Default.Language,
                                title = stringResource(R.string.config_language_section)
                            )

                            // Idiomas como grupo de ToggleButton conectados (selección única)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                filteredLanguages.forEachIndexed { index, (displayName, locale) ->
                                    val selected = idiomaSeleccionat.second.language == locale.language
                                    ToggleButton(
                                        checked = selected,
                                        onCheckedChange = { switchLanguage(context, locale, displayName) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                            .semantics { role = Role.RadioButton },
                                        shapes = connectedShapes(index, filteredLanguages.size)
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(
                                            text = displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        
                        // TAMAÑO DE TEXTO (solo si puede cambiar tamaño)
                        if (hasPermission("canChangeTextSize", true)) {
                            if (hasAnyOption) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                            hasAnyOption = true
                            
                            ConfigHeader(
                                icon = Icons.Default.FormatSize,
                                title = stringResource(R.string.config_text_size_section)
                            )
                            
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                TextSizeAdjuster(
                                    onTextSizeChanged = onTextSizeChanged
                                )
                            }
                        }

                        // TTS Configuration - Solo mostrar si tiene permiso
                        if (hasPermission("canUseTTS", true)) {
                            if (hasAnyOption) {
                                Divider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                            hasAnyOption = true

                            ConfigHeader(
                                icon = Icons.Default.RecordVoiceOver,
                                title = stringResource(R.string.config_tts_title)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.config_tts_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = stringResource(R.string.config_tts_description),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Switch(
                                    checked = ttsEnabled,
                                    onCheckedChange = { enabled ->
                                        ttsEnabled = enabled
                                        ttsManager.setEnabled(enabled)
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (ttsEnabled) Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            }
                        }
                        
                        // Si no hay ninguna opción disponible, mostrar mensaje
                        if (!hasAnyOption) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = stringResource(R.string.config_no_permissions_available),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Text(
                                        text = stringResource(R.string.config_contact_admin_for_access),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Botón de cerrar sesión (solo si tiene permiso)
                    if (hasPermission("canLogout", true)) {
                        LogoutButton(onLogout = onLogout)
                    }
                }
            }
        }
    }
    
    // Dialog para solicitud de descanso
    if (isBreakDialogVisible) {
        BreakRequestDialog(
            onDismiss = { isBreakDialogVisible = false },
            onConfirm = { reason -> sendBreakRequest(reason) },
            isLoading = isSendingBreakRequest
        )
    }
}

@Composable
private fun WaiterProfileSection(currentUser: cat.dam.mamadou.etable.supabase.FirebaseUser?) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del camarero
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    )
                    .border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person, // Icono diferente para camarero
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentUser?.displayName ?: 
                          currentUser?.email?.substringBefore("@") ?: 
                          stringResource(R.string.user_role_waiter),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = currentUser?.email ?: "",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.user_role_waiter),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakRequestButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Coffee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.break_request_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = stringResource(R.string.break_request_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BreakRequestDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean
) {
    var reason by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.ifBlank { "" }) },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(R.string.break_request_send))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = {
            Text(stringResource(R.string.break_request_dialog_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.break_request_dialog_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.break_request_reason)) },
                    placeholder = { Text(stringResource(R.string.break_request_reason_placeholder)) },
                    maxLines = 3,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

// Reutilizar los mismos componentes que AdminConfigScreen pero sin cambios
@Composable
private fun ConfigCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
    val backgroundColor = when (theme) {
        AppTheme.LIGHT -> Color(0xFFF8F8F8)
        AppTheme.DARK -> Color(0xFF202020)
        AppTheme.OCEAN -> Color(0xFFE1F5FE)
        AppTheme.SUNSET -> Color(0xFFFBE9E7)
        AppTheme.NATURE -> Color(0xFFF1F8E9)
        AppTheme.PURPLE -> Color(0xFFF3E5F5)
        AppTheme.MONOCHROME -> Color(0xFFFAFAFA)
        AppTheme.AUTUMN -> Color(0xFFFFF8E1)
        AppTheme.VINTAGE -> Color(0xFFF5F5DC)
        AppTheme.FIRE -> Color(0xFFFFF3E0)
        AppTheme.MIDNIGHT -> Color(0xFF121212)
        AppTheme.FOREST -> Color(0xFFE8F5E9)
    }
    
    val name = when(theme) {
        AppTheme.LIGHT -> stringResource(R.string.theme_name_light)
        AppTheme.DARK -> stringResource(R.string.theme_name_dark)
        AppTheme.OCEAN -> stringResource(R.string.theme_name_ocean)
        AppTheme.SUNSET -> stringResource(R.string.theme_name_sunset)
        AppTheme.NATURE -> stringResource(R.string.theme_name_nature)
        AppTheme.PURPLE -> stringResource(R.string.theme_name_purple)
        AppTheme.MONOCHROME -> stringResource(R.string.theme_name_monochrome)
        AppTheme.AUTUMN -> stringResource(R.string.theme_name_autumn)
        AppTheme.VINTAGE -> stringResource(R.string.theme_name_vintage)
        AppTheme.FIRE -> stringResource(R.string.theme_name_fire)
        AppTheme.MIDNIGHT -> stringResource(R.string.theme_name_midnight)
        AppTheme.FOREST -> stringResource(R.string.theme_name_forest)
    }
    
    val icon = when(theme) {
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
    
    // Selección: solo cambio de color (sin rebote ni animación de sombra)
    val containerColor by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "themeOptionColor"
    )

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
                        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
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
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when (theme) {
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
                        AppTheme.FOREST -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = when (theme) {
                        AppTheme.LIGHT -> stringResource(R.string.theme_desc_light)
                        AppTheme.DARK -> stringResource(R.string.theme_desc_dark)
                        AppTheme.OCEAN -> stringResource(R.string.theme_desc_ocean)
                        AppTheme.SUNSET -> stringResource(R.string.theme_desc_sunset)
                        AppTheme.NATURE -> stringResource(R.string.theme_desc_nature)
                        AppTheme.PURPLE -> stringResource(R.string.theme_desc_purple)
                        AppTheme.MONOCHROME -> stringResource(R.string.theme_desc_monochrome)
                        AppTheme.AUTUMN -> stringResource(R.string.theme_desc_autumn)
                        AppTheme.VINTAGE -> stringResource(R.string.theme_desc_vintage)
                        AppTheme.FIRE -> stringResource(R.string.theme_desc_fire)
                        AppTheme.MIDNIGHT -> stringResource(R.string.theme_desc_midnight)
                        AppTheme.FOREST -> stringResource(R.string.theme_desc_forest)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
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
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shapes = ButtonDefaults.shapes(),
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
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            stringResource(R.string.config_logout),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                letterSpacing = 1.sp
            )
        )
    }
}