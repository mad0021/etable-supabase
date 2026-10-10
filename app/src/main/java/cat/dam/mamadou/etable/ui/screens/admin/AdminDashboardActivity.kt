package cat.dam.mamadou.etable.ui.screens.admin

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.admin.screens.*
import cat.dam.mamadou.etable.ui.theme.EtableTheme
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminBottomNavigationBar
import cat.dam.mamadou.etable.utils.TextUtils
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.BreakRequestViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.OrderViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.BreakRequestViewModelFactory
import cat.dam.mamadou.etable.ui.screens.auth.LoginActivity
import cat.dam.mamadou.etable.ui.screens.waiter.screens.TablesScreen
import cat.dam.mamadou.etable.ui.theme.AppTheme
import cat.dam.mamadou.etable.ui.theme.ETableThemeWrapper
import cat.dam.mamadou.etable.ui.theme.ThemeManager
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.LocaleManager
import cat.dam.mamadou.etable.utils.PermissionsManager
import java.util.*
import cat.dam.mamadou.etable.utils.firebase.BreakRequest
import cat.dam.mamadou.etable.utils.firebase.BreakRequestEventBus
import kotlinx.coroutines.launch

class AdminDashboardActivity : ComponentActivity() {
    // Inicializar Firebase
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // IMPORTANTE: Aplicar el idioma guardado ANTES de inflar el layout
        val prefs = getSharedPreferences(LocaleManager.PREFS_LANGUAGE, Context.MODE_PRIVATE)
        val language = prefs.getString(LocaleManager.KEY_SELECTED_LANGUAGE, null)
        val country = prefs.getString(LocaleManager.KEY_SELECTED_COUNTRY, null)
        
        if (language != null && country != null) {
            val locale = Locale(language, country)
            LocaleManager.setLocale(this, locale)
        }
        
        // NUEVO: Si la app se inició desde cero, limpiar notificaciones de sesión
        if (savedInstanceState == null) {
            Log.d("AdminDashboard", "🆕 Nueva instancia de AdminDashboard, limpiando sesión anterior")
            getSharedPreferences("AdminNotificationSession", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
        
        // Log inicial
        Log.d("AdminDashboard", "🌈 Iniciando AdminDashboardActivity")
        
        setContent {
            // Usar el nuevo wrapper de tema que observa automáticamente los cambios
            ETableThemeWrapper {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdminDashboardContent(
                        currentTheme = ThemeManager.getTheme(this@AdminDashboardActivity),
                        isDarkThemeInitial = ThemeManager.isDarkMode(this@AdminDashboardActivity),
                        onLogout = {
                            // Cerrar sesión y redirigir a la pantalla de login
                            firebaseManager.authManager.signOut()
                            val intent = Intent(this@AdminDashboardActivity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }

    // También sobrescribe los métodos del ciclo de vida para mantener el idioma
    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleManager.getPersistedLocale(newBase)
        val context = LocaleManager.updateResources(newBase, locale)
        super.attachBaseContext(context)
    }

    // AGREGAR override de onDestroy para limpieza completa:
    override fun onDestroy() {
        super.onDestroy()
        
        // Si la actividad se está destruyendo completamente (no por rotación)
        if (isFinishing) {
            Log.d("AdminDashboard", "🧹 AdminDashboard terminando, limpiando notificaciones de sesión")
            getSharedPreferences("AdminNotificationSession", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }
    }
}

@Composable
fun AdminDashboardContent(
    currentTheme: AppTheme = ThemeManager.getTheme(LocalContext.current),
    isDarkThemeInitial: Boolean = false,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val breakRequestViewModel: BreakRequestViewModel = viewModel(
        factory = BreakRequestViewModelFactory(context)
    )
    val orderViewModelFactory = OrderViewModelFactory(context)

    // Convertir el parámetro inmutable en estado mutable
    var isDarkTheme by remember { mutableStateOf(isDarkThemeInitial) }

    // Estados para el tamaño de texto, tema e idioma
    var textSize by remember { mutableStateOf(TextUtils.getSavedTextSize(context)) }
    var appLanguage by remember { mutableStateOf(Locale("ca", "ES")) }

    // Estado para las notificaciones de solicitudes de descanso
    var hasNewBreakRequests by remember { mutableStateOf(false) }
    var lastBreakRequest by remember { mutableStateOf<cat.dam.mamadou.etable.utils.firebase.BreakRequest?>(null) }

    // MOVER AQUÍ el launcher para permisos:
    val adminNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("AdminDashboard", "✅ Permiso de notificaciones concedido")
        } else {
            Log.w("AdminDashboard", "❌ Permiso de notificaciones denegado")
        }
    }

    // MOVER AQUÍ el LaunchedEffect para solicitar permisos:
    LaunchedEffect(Unit) {
        // Solicitar permiso de notificaciones para solicitudes de descanso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionsManager.isPermissionGranted(context, PermissionsManager.PermissionType.NOTIFICATION)) {
                Log.d("AdminDashboard", "🔔 Solicitando permiso de notificaciones para solicitudes de descanso")
                
                PermissionsManager.requestPermission(
                    PermissionsManager.PermissionType.NOTIFICATION,
                    adminNotificationPermissionLauncher
                )
            }
        }
    }

    // Referencia al MediaPlayer para reproducir sonido de notificación - CORREGIDO PARA .MP3
    val notificationSound = remember { 
        try {
            // USAR TU ARCHIVO .MP3 PERSONALIZADO
            val mediaPlayer = MediaPlayer.create(context, R.raw.notification_sound)
            if (mediaPlayer != null) {
                Log.d("AdminDashboard", "✅ notification_sound.mp3 cargado correctamente")
                mediaPlayer
            } else {
                Log.w("AdminDashboard", "⚠️ notification_sound.mp3 no encontrado, usando sonido del sistema")
                // Fallback al sonido del sistema
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val fallbackPlayer = MediaPlayer()
                fallbackPlayer.setDataSource(context, uri)
                fallbackPlayer.prepare()
                fallbackPlayer
            }
        } catch (e: Exception) {
            Log.e("AdminDashboard", "❌ Error cargando notification_sound.mp3: ${e.message}")
            try {
                // Fallback al sonido del sistema
                val uri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                val fallbackPlayer = MediaPlayer()
                fallbackPlayer.setDataSource(context, uri)
                fallbackPlayer.prepare()
                fallbackPlayer
            } catch (fallbackError: Exception) {
                Log.e("AdminDashboard", "❌ Error con sonido fallback: ${fallbackError.message}")
                null
            }
        }
    }

    // Escuchar solicitudes de descanso en tiempo real
    val firebaseManager = remember { FirebaseManager() }
    var breakRequestListener by remember { mutableStateOf<Any?>(null) }

    DisposableEffect(Unit) {
        // IMPORTANTE: Habilitar notificaciones modales en AdminDashboard
        BreakNotificationManager.enableModalNotifications()
        
        // NUEVO: Usar SharedPreferences para persistir notificaciones procesadas en esta sesión
        val sessionPrefs = context.getSharedPreferences("AdminNotificationSession", Context.MODE_PRIVATE)
        val processedInSession = sessionPrefs.getStringSet("processed_notifications", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        Log.d("AdminDashboard", "🔄 Iniciando listener. Notificaciones ya procesadas en sesión: ${processedInSession.size}")
        
        // Registrar listener para solicitudes de descanso
        breakRequestListener = firebaseManager.breakManager.listenForBreakRequests { breakRequests ->
            val pendingRequests = breakRequests.filter { !it.viewed && it.status == "pending" }
            hasNewBreakRequests = pendingRequests.isNotEmpty()

            if (pendingRequests.isNotEmpty()) {
                // Obtener la solicitud más reciente que NO haya sido procesada en esta sesión
                val latestRequest = pendingRequests
                    .filter { !processedInSession.contains(it.id) }
                    .maxByOrNull { it.timestamp }
                
                if ((latestRequest != null) && (latestRequest != lastBreakRequest)) {
                    lastBreakRequest = latestRequest
                    
                    // IMPORTANTE: Marcar como procesada INMEDIATAMENTE y persistir
                    processedInSession.add(latestRequest.id)
                    sessionPrefs.edit()
                        .putStringSet("processed_notifications", processedInSession)
                        .apply()
                    
                    Log.d("AdminDashboard", "✅ Notificación ${latestRequest.id} marcada como procesada en sesión")
                    
                    // Emitir evento a través del EventBus para que otros componentes puedan reaccionar
                    scope.launch {
                        BreakRequestEventBus.notifyNewBreakRequest(latestRequest)
                    }

                    // CORREGIR: Verificar si ya fue procesada globalmente antes de mostrar notificación
                    if (!BreakNotificationManager.isNotificationProcessed(context, latestRequest.id)) {
                        // NUEVO: Mostrar notificación del sistema
                        BreakNotificationManager.showBreakNotification(context, latestRequest)
                        
                        // MEJORAR: Reproducir sonido con múltiples fallbacks
                        try {
                            // Verificar configuración de audio
                            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                            val notificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                            val ringerMode = audioManager.ringerMode

                            Log.d("AdminDashboard", "🔊 Estado de audio:")
                            Log.d("AdminDashboard", "   - Volumen notificaciones: $notificationVolume/$maxVolume")
                            Log.d("AdminDashboard", "   - Modo de timbre: $ringerMode (0=silencio, 1=vibración, 2=normal)")

                            if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
                                Log.w("AdminDashboard", "⚠️ Teléfono en modo silencioso")
                            } else if (notificationVolume == 0) {
                                Log.w("AdminDashboard", "⚠️ Volumen de notificaciones en 0")
                            }

                            // Intentar reproducir sonido - PRIORIZAR TU ARCHIVO .MP3
                            if (notificationSound != null) {
                                try {
                                    // Verificar que el MediaPlayer esté en estado válido
                                    if (!notificationSound.isPlaying) {
                                        // Ajustar volumen al máximo para tu archivo .mp3
                                        notificationSound.setVolume(1.0f, 1.0f)
                                        notificationSound.seekTo(0) // Reiniciar desde el principio
                                        notificationSound.start()
                                        Log.d("AdminDashboard", "🔊 notification_sound.mp3 reproducido correctamente")
                                    } else {
                                        Log.d("AdminDashboard", "🔊 notification_sound.mp3 ya se está reproduciendo")
                                    }
                                } catch (e: Exception) {
                                    Log.e("AdminDashboard", "❌ Error reproduciendo notification_sound.mp3: ${e.message}")
                                    
                                    // Solo usar fallback si tu .mp3 falla
                                    try {
                                        Log.d("AdminDashboard", "🔄 Usando sonido del sistema como fallback")
                                        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                        val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                                        ringtone?.play()
                                        Log.d("AdminDashboard", "🔊 Sonido del sistema reproducido")
                                    } catch (fallbackError: Exception) {
                                        Log.e("AdminDashboard", "❌ Error total con sonido: ${fallbackError.message}")
                                    }
                                }
                            } else {
                                Log.w("AdminDashboard", "⚠️ MediaPlayer es null, intentando reproducir con sistema")
                                try {
                                    val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                    val ringtone = RingtoneManager.getRingtone(context, notificationUri)
                                    ringtone?.play()
                                    Log.d("AdminDashboard", "🔊 Sonido del sistema reproducido (MediaPlayer null)")
                                } catch (e: Exception) {
                                    Log.e("AdminDashboard", "❌ Error total reproduciendo sonido: ${e.message}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AdminDashboard", "❌ Error reproduciendo sonido: ${e.message}")
                            
                            // Opción 3: Usar ToneGenerator como último recurso
                            try {
                                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
                                if (currentVolume > 0) {
                                    // Si hay volumen, intentar reproducir con ToneGenerator
                                    val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                                    toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 500)
                                    toneGenerator.release()
                                    Log.d("AdminDashboard", "🔊 Tono de emergencia reproducido")
                                } else {
                                    Log.w("AdminDashboard", "⚠️ Volumen silenciado")
                                }
                            } catch (toneError: Exception) {
                                Log.e("AdminDashboard", "❌ Error con tono de emergencia: ${toneError.message}")
                            }
                        }

                        // Vibrar el dispositivo
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            // Patrón de vibración más distintivo
                            val pattern = longArrayOf(0, 300, 100, 300, 100, 600)
                            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 300, 100, 300, 100, 600), -1)
                        }
                        
                        Log.d("AdminDashboard", "🔔 Nueva notificación procesada: ${latestRequest.userName}")
                    } else {
                        Log.d("AdminDashboard", "🚫 Notificación ya procesada anteriormente: ${latestRequest.id}")
                    }
                } else if (latestRequest != null && processedInSession.contains(latestRequest.id)) {
                    Log.d("AdminDashboard", "🔄 Notificación ${latestRequest.id} ya procesada en esta sesión, omitiendo sonido")
                }
            }
        }

        // Limpiar el listener cuando se destruya la composición
        onDispose {
            (breakRequestListener as? cat.dam.mamadou.etable.supabase.ListenerRegistration)?.remove()
            // CORREGIR: Usar safe call operator
            notificationSound?.release()
            
            // NUEVO: Limpiar notificaciones procesadas cuando se cierre la app completamente
            // (pero no en rotaciones)
            if (context is ComponentActivity) {
                if (context.isFinishing) {
                    Log.d("AdminDashboard", "🧹 App cerrando, limpiando notificaciones de sesión")
                    context.getSharedPreferences("AdminNotificationSession", Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                }
            }
        }
    }

    // Envolver todo el contenido con el tema, pasando el valor explícito del tema
    EtableTheme(darkTheme = isDarkTheme, currentTheme = currentTheme) {
        // Estructura principal con Scaffold
        Scaffold(
            bottomBar = {
                AdminBottomNavigationBar(
                    currentScreen = navController.currentBackStackEntryAsState().value?.destination?.route ?: "dashboard",
                    onNavigate = { screen -> 
                        navController.navigate(screen) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    hasNewBreakRequests = hasNewBreakRequests
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Contenido principal con NavHost
                NavHost(
                    navController = navController,
                    startDestination = "dashboard"
                ) {
                    composable("dashboard") {
                        AdminHomeScreen()
                    }
                    composable("orders") {
                        AdminOrdersScreen(navController = navController)
                    }
                    composable("menu") {
                        MenuManagementScreen(navController)
                    }
                    composable("tables") {
                        TablesScreen(navController)
                    }
                    composable("users") {
                        UsersScreen(navController)
                    }
                    composable("reservations") {
                        ReservationsScreen(navController)
                    }
                    composable("break_requests") {
                        BreakMessagesScreen(navController)
                    }
                    // Actualizar la ruta de configuración para pasar el callback de logout
                    composable("config") {
                        AdminConfigScreen(
                            onTextSizeChanged = { newSize ->
                                textSize = newSize
                            },
                            onThemeChanged = { isDark ->
                                isDarkTheme = isDark
                                
                                // También actualizar en el ThemeManager para persistencia
                                val currentTheme = ThemeManager.getTheme(context)
                                ThemeManager.applyTheme(context, currentTheme, isDark)
                            },
                            onLanguageChanged = { locale ->
                                appLanguage = locale
                            },
                            onLogout = onLogout
                        )
                    }
                }
            }
        }
    }
}

// Actualizar la función AdminHomeScreen para usar recursos de strings
@Composable
fun AdminHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.admin_dashboard_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.admin_dashboard_welcome),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}