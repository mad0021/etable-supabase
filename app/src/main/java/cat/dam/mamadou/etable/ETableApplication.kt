package cat.dam.mamadou.etable

import android.app.Application
import android.content.Context
import android.util.Log
import cat.dam.mamadou.etable.ui.theme.AppTheme
import cat.dam.mamadou.etable.ui.theme.ThemeManager
import cat.dam.mamadou.etable.utils.SessionManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseAuthManager
import cat.dam.mamadou.etable.supabase.FirebaseAuth
import cat.dam.mamadou.etable.supabase.initSupabase
import cat.dam.mamadou.etable.ui.theme.ThemeController
import cat.dam.mamadou.etable.utils.LocaleManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import android.content.res.Configuration
import java.util.Locale

class ETableApplication : Application() {
    
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAuthManager: FirebaseAuthManager
    
    companion object {
        @Volatile private lateinit var instance: ETableApplication
        
        fun getInstance(): ETableApplication = instance
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Configurar idioma de la aplicación
        val savedLanguage = LocaleManager.getPersistedLocale(this)
        val config = Configuration(resources.configuration)
        config.setLocale(savedLanguage)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Limpieza de notificaciones al inicio de la aplicación
        val prefs = getSharedPreferences("NotificationState", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("blockNotificationRestore", true).apply()
        
        // Inicializar Supabase (recupera la sesión guardada)
        initSupabase(this)

        // Inicializar SessionManager y FirebaseAuthManager
        sessionManager = SessionManager(this)
        firebaseAuthManager = FirebaseAuthManager(FirebaseAuth.getInstance())
        
        // Configurar el tema predeterminado si no hay uno guardado
        val savedTheme = ThemeManager.getTheme(this)
        if (savedTheme == null) {
            Log.w("ETableApplication", "Tema corrupto o nulo, reseteando a LIGHT")
            ThemeManager.saveTheme(this, AppTheme.LIGHT, false)
        }
        
        // CREAR CANAL DE NOTIFICACIONES CON TU .MP3
        BreakNotificationManager.createNotificationChannel(this)
        
        // Inicializar canales de notificación
        setupNotificationChannels()
        
        // Inicializar el controlador de temas
        ThemeController.initialize(this)
        Log.d("ETableApplication", "ThemeController inicializado con tema: ${ThemeController.themeState.value}")
        
        // Instancia para acceso global
        instance = this
        
        // Restaurar sesión de forma proactiva
        if (sessionManager.isLoggedIn()) {
            val userId = sessionManager.getUserId()
            val email = sessionManager.getUserEmail()
            val role = sessionManager.getUserRole()
            
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null && userId.isNotEmpty() && role.isNotEmpty()) {
                Log.w("ETableApplication", "Usuario en SessionManager pero no en FirebaseAuth")
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
    }
    
    override fun attachBaseContext(base: Context) {
        val locale = LocaleManager.getPersistedLocale(base)
        val context = LocaleManager.updateResources(base, locale)
        super.attachBaseContext(context)
    }
    
    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Canal para notificaciones de órdenes listas
            val orderReadyChannel = NotificationChannel(
                "ORDER_READY_CHANNEL",
                getString(R.string.notification_channel_orders),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_orders_description)
            }
            
            // Canal para notificaciones de categorías listas
            val categoryReadyChannel = NotificationChannel(
                "CATEGORY_READY_CHANNEL", 
                getString(R.string.notification_channel_categories),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_categories_description)
            }
            
            notificationManager.createNotificationChannel(orderReadyChannel)
            notificationManager.createNotificationChannel(categoryReadyChannel)
        }
    }
}