package cat.dam.mamadou.etable.ui.screens.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.admin.AdminDashboardActivity
import cat.dam.mamadou.etable.ui.screens.waiter.WaiterDashboardActivity
import cat.dam.mamadou.etable.ui.theme.EtableTheme
import cat.dam.mamadou.etable.utils.SessionManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseAuthManager
import cat.dam.mamadou.etable.supabase.FirebaseAuth

class LoginActivity : ComponentActivity() {
    private lateinit var authManager: FirebaseAuthManager
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar lazy para mejor rendimiento
        authManager = FirebaseAuthManager(FirebaseAuth.getInstance())
        authManager.initSessionManager(applicationContext)
        sessionManager = SessionManager(applicationContext)

        setContent {
            EtableTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UltraOptimizedRoleSelectionScreen { email, password, role, onError ->
                        performLogin(email, password, role, onError)
                    }
                }
            }
        }
    }

    // onError recibe el mensaje de error (o null) para mostrarlo en el diálogo
    private fun performLogin(email: String, password: String, role: String, onError: (String?) -> Unit) {
        Log.d("LoginActivity", "Login: $email, rol: $role")

        authManager.signInWithEmailAndPassword(
            email = email,
            password = password,
            role = role
        ) { authResult, error ->
            if (error == null && authResult != null) {
                val user = authResult.user
                val uid = user?.uid ?: run {
                    onError(null)
                    return@signInWithEmailAndPassword
                }

                Log.d("LoginActivity", "Auth exitosa. UID: $uid")
                navigateBasedOnRole(role, uid)
            } else {
                Log.e("LoginActivity", "Error: ${error?.message}")
                onError(error?.message)
            }
        }
    }

    private fun navigateBasedOnRole(selectedRole: String, userId: String) {
        val intent = when (selectedRole.trim().lowercase()) {
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            "cambrer" -> Intent(this, WaiterDashboardActivity::class.java)
            else -> return
        }
        
        intent.putExtra("USER_ID", userId)
        intent.putExtra("USER_ROLE", selectedRole)
        startActivity(intent)
        finish()
    }

    @Composable
    fun UltraOptimizedRoleSelectionScreen(onLogin: (String, String, String, (String?) -> Unit) -> Unit) {
        var showAdminDialog by remember { mutableStateOf(false) }
        var showWaiterDialog by remember { mutableStateOf(false) }

        // Fondo simple sin efectos decorativos
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Contenido principal simplificado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                // Título simple
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color(0xFF4CAF50)
                )

                Spacer(modifier = Modifier.height(80.dp))

                // Tarjetas optimizadas lado a lado
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tarjeta CAMBRER
                    OptimizedRoleCard(
                        title = stringResource(R.string.role_waiter),
                        imageRes = R.drawable.mesero,
                        onClick = { showWaiterDialog = true },
                        color = Color(0xFF3F51B5),
                        modifier = Modifier.weight(1f)
                    )

                    // Tarjeta ADMIN
                    OptimizedRoleCard(
                        title = stringResource(R.string.role_admin),
                        imageRes = R.drawable.gastronomo,
                        onClick = { showAdminDialog = true },
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Diálogos optimizados
            if (showAdminDialog) {
                OptimizedLoginDialog(
                    role = "admin",
                    onDismiss = { showAdminDialog = false },
                    onLogin = onLogin
                )
            }
            
            if (showWaiterDialog) {
                OptimizedLoginDialog(
                    role = "cambrer",
                    onDismiss = { showWaiterDialog = false },
                    onLogin = onLogin
                )
            }
        }
    }

    @Composable
    fun OptimizedRoleCard(
        title: String, 
        imageRes: Int, 
        onClick: () -> Unit, 
        color: Color,
        modifier: Modifier = Modifier
    ) {
        // Respuesta táctil: escala ligera al pulsar con el spring rápido del motionScheme
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (pressed) 0.96f else 1f,
            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
            label = "roleCardPress"
        )
        Card(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier = modifier
                .aspectRatio(1f) // Mantener proporción cuadrada
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // La imagen cede espacio para que el título siempre quepa (en móvil la tarjeta mide ~150dp)
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .sizeIn(maxWidth = 96.dp, maxHeight = 96.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Título
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun OptimizedLoginDialog(
        role: String,
        onDismiss: () -> Unit,
        onLogin: (String, String, String, (String?) -> Unit) -> Unit
    ) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val genericError = stringResource(R.string.login_error_generic)
        val credentialsError = stringResource(R.string.login_error_credentials)
        val roleError = stringResource(R.string.login_error_role)
        val inactiveError = stringResource(R.string.login_error_inactive)
        // Mantiene el diálogo abierto y muestra el error traducido si el login falla
        val submit = {
            isLoading = true
            errorMessage = null
            onLogin(email, password, role) { rawError ->
                isLoading = false
                errorMessage = when {
                    rawError == null -> genericError
                    "contraseña" in rawError -> credentialsError
                    "rol seleccionado" in rawError -> roleError
                    "desactivado" in rawError -> inactiveError
                    else -> rawError
                }
            }
        }
        var isPasswordVisible by remember { mutableStateOf(false) }
        val context = LocalContext.current
        
        // Pre-calcular strings para evitar recomposiciones
        val roleColor = remember {
            when (role.lowercase()) {
                "admin" -> Color(0xFF4CAF50)
                "cambrer" -> Color(0xFF3F51B5)
                else -> Color(0xFF4CAF50)
            }
        }
        
        val imageRes = remember {
            when (role.lowercase()) {
                "admin" -> R.drawable.gastronomo
                "cambrer" -> R.drawable.mesero
                else -> R.drawable.gastronomo
            }
        }

        Dialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Icono de rol simplificado
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Título
                    Text(
                        text = stringResource(
                            R.string.login_as,
                            stringResource(if (role.lowercase() == "admin") R.string.role_admin else R.string.role_waiter)
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = roleColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.enter_credentials),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Campo de correo optimizado
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.email_label)) },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = roleColor)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = roleColor,
                            focusedLabelColor = roleColor,
                            cursorColor = roleColor
                        ),
                        shape = MaterialTheme.shapes.small,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo de contraseña optimizado
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.password_label)) },
                        visualTransformation = if (isPasswordVisible) 
                            VisualTransformation.None 
                        else 
                            PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = roleColor)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) 
                                        Icons.Filled.VisibilityOff 
                                    else 
                                        Icons.Filled.Visibility,
                                    contentDescription = if (isPasswordVisible) 
                                        stringResource(R.string.hide_password) 
                                    else 
                                        stringResource(R.string.show_password),
                                    tint = roleColor
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = roleColor,
                            focusedLabelColor = roleColor,
                            cursorColor = roleColor
                        ),
                        shape = MaterialTheme.shapes.small,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (email.isNotEmpty() && password.isNotEmpty() && !isLoading) {
                                    submit()
                                }
                            }
                        )
                    )

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón de login
                    Button(
                        onClick = {
                            if (email.isNotBlank() && password.isNotBlank()) {
                                submit()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.enter_email_password),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shapes = ButtonDefaults.shapes(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = roleColor)
                    ) {
                        if (isLoading) {
                            ContainedLoadingIndicator(
                                modifier = Modifier.size(40.dp),
                                containerColor = roleColor,
                                indicatorColor = Color.White
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.login_button_text),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón cancelar
                    TextButton(
                        onClick = onDismiss,
                        shapes = ButtonDefaults.shapes(),
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.cancel_button),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
