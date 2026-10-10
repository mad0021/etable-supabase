package cat.dam.mamadou.etable.ui.screens.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.admin.AdminDashboardActivity
import cat.dam.mamadou.etable.ui.screens.waiter.WaiterDashboardActivity
import cat.dam.mamadou.etable.ui.theme.EtableTheme
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.supabase.awaitSupabaseReady
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    private val firebaseManager by lazy { FirebaseManager() }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EtableTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Simplificar la pantalla de splash
                    UltraOptimizedSplashScreen {
                        checkSession()
                    }
                }
            }
        }
    }

    private fun checkSession() {
        try {
            val isSessionActive = firebaseManager.authManager.checkAuthState(this)
            
            if (isSessionActive) {
                val userRole = firebaseManager.authManager.getUserRole()
                Log.d("SplashActivity", "Sesión activa, rol: $userRole")
                
                when (userRole.lowercase()) {
                    "admin" -> {
                        startActivity(Intent(this, AdminDashboardActivity::class.java))
                        finish()
                    }
                    "cambrer" -> {
                        startActivity(Intent(this, WaiterDashboardActivity::class.java))
                        finish()
                    }
                    else -> {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                }
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "Error verificando sesión", e)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UltraOptimizedSplashScreen(onTimeout: () -> Unit) {
    // Reducir al mínimo las animaciones y estados
    var isVisible by remember { mutableStateOf(false) }
    
    // Una sola animación simple para todo
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
        label = "fade"
    )

    // Ejecutar una sola vez con menor tiempo
    LaunchedEffect(Unit) {
        isVisible = true
        delay(800) // Splash más rápido - solo 800ms
        awaitSupabaseReady() // la sesión guardada debe estar cargada antes de comprobarla
        onTimeout()
    }

    // Fondo ultra simple sin Canvas decorativo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8)), // Color sólido en lugar de gradiente
        contentAlignment = Alignment.Center
    ) {
        // Contenido mínimo centrado
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo sin escalado ni efectos
            Image(
                painter = painterResource(id = R.drawable.ic_launcherplaystore),
                contentDescription = null, // Reducir trabajo de accesibilidad en splash
                modifier = Modifier
                    .size(160.dp) // Tamaño más pequeño
                    .alpha(alpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Texto simple sin efectos
            Text(
                text = "eTable",
                color = Color(0xFF4CAF50),
                fontSize = 32.sp, // Tamaño reducido
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(alpha)
            )

            Spacer(modifier = Modifier.height(24.dp))

            LoadingIndicator(color = Color(0xFF4CAF50))
        }
    }
}