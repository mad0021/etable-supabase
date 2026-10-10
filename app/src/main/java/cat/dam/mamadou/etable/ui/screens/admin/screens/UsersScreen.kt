@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.SessionManager
import kotlinx.coroutines.launch
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.UsersViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.UsersViewModelFactory
import cat.dam.mamadou.etable.models.User // USAR EL MODELO CENTRALIZADO
import java.util.Locale

// Efecto para escuchar notificaciones de descanso pero solo con sonido
@Composable
private fun UseBreakSoundOnlyListener(firebaseManager: FirebaseManager) {
    DisposableEffect(Unit) {
        // Asegurarse de que las notificaciones visuales están desactivadas
        BreakNotificationManager.disableModalNotifications()
        

        
        onDispose { }
    }
}

@Composable
fun UsersScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: UsersViewModel = viewModel(factory = UsersViewModelFactory(context))
    val scope = rememberCoroutineScope()
    
    val firebaseManager = remember { FirebaseManager() }
    UseBreakSoundOnlyListener(firebaseManager)
    
    val users = viewModel.users.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val error = viewModel.error.collectAsState().value
    
    // El superadmin es un campo del usuario que protege la base de datos (002_seguridad.sql), no un email fijo
    val sessionManager = remember { SessionManager(context) }
    var isSuperAdmin by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        firebaseManager.getFirestore().collection("user").document(sessionManager.getUserId()).get()
            .addOnSuccessListener { isSuperAdmin = it.getBoolean("superadmin") == true }
    }
    
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var userToEdit by remember { mutableStateOf<User?>(null) }
    var showUserConfigDialog by remember { mutableStateOf(false) }
    var userToConfig by remember { mutableStateOf<User?>(null) }
    
    var formError by remember { mutableStateOf<String?>(null) }
    
    // AÑADIR: Variables de estado que faltaban
    var isCreatingUser by remember { mutableStateOf(false) }
    var isEditingUser by remember { mutableStateOf(false) }
    
    // Variables para nuevo usuario
    var newUserEmail by remember(showAddUserDialog) { mutableStateOf("") }
    var newUserPassword by remember(showAddUserDialog) { mutableStateOf("") }
    var newUserFullName by remember(showAddUserDialog) { mutableStateOf("") }
    // MODIFICADO: Solo permitir crear admin si es super admin
    var newUserRole by remember(showAddUserDialog) { 
        mutableStateOf(if (isSuperAdmin) "cambrer" else "cambrer") 
    }
    
    var editUserFullName by remember(userToEdit) { mutableStateOf(userToEdit?.fullName ?: "") }
    var editUserRole by remember(userToEdit) { mutableStateOf(userToEdit?.role ?: "") }
    var editUserIsActive by remember(userToEdit) { mutableStateOf(userToEdit?.isActive ?: true) }
    
    LaunchedEffect(Unit) {
        try {
            viewModel.loadUsers()
        } catch (e: Exception) {
            Log.e("UsersScreen", "Error al cargar usuarios: ${e.message}", e)
        }
    }
    
    val usersList = remember(users) {
        users.mapNotNull { userData ->
            try {
                User.fromMap(userData["id"] as? String ?: "", userData)
            } catch (e: Exception) {
                Log.e("UsersScreen", "Error mapeando usuario: ${e.message}", e)
                null
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            MediumFloatingActionButton(
                onClick = { showAddUserDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.users_add),
                    modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Se eliminó el bloque de código que mostraba la notificación visual
                
                when {
                    isLoading -> {
                        AdminLoading()
                    }
                    usersList.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.users_empty),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 24.sp
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = usersList,
                                key = { it.id }
                            ) { user ->
                                OptimizedUserCard(
                                    modifier = adminAnimateItem(),
                                    user = user,
                                    onEdit = {
                                        userToEdit = user
                                        showEditUserDialog = true
                                    },
                                    onConfig = {
                                        userToConfig = user
                                        showUserConfigDialog = true
                                    },
                                    onDelete = { 
                                        scope.launch { 
                                            try {
                                                val result = viewModel.deleteUser(user.id)
                                                if (result) {
                                                    Toast.makeText(context, context.getString(R.string.user_deleted_success), Toast.LENGTH_SHORT).show()
                                                    viewModel.loadUsers()
                                                } else {
                                                    Toast.makeText(context, context.getString(R.string.user_delete_error), Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, context.getString(R.string.user_delete_error), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // MODIFICAR diálogo de añadir usuario para solo mostrar opción admin si es super admin
    if (showAddUserDialog) {
        OptimizedAddUserDialog(
            email = newUserEmail,
            password = newUserPassword,
            fullName = newUserFullName,
            role = newUserRole,
            formError = formError,
            isCreating = isCreatingUser,
            isSuperAdmin = isSuperAdmin, // NUEVO parámetro
            onEmailChange = { newUserEmail = it },
            onPasswordChange = { newUserPassword = it },
            onFullNameChange = { newUserFullName = it },
            onRoleChange = { newUserRole = it },
            onDismiss = {
                showAddUserDialog = false
                formError = null
                newUserEmail = ""
                newUserPassword = ""
                newUserFullName = ""
                newUserRole = "cambrer"
            },
            onCreateUser = {
                when {
                    newUserEmail.isBlank() -> {
                        formError = context.getString(R.string.validation_email_required)
                    }
                    newUserPassword.isBlank() -> {
                        formError = context.getString(R.string.validation_password_required)
                    }
                    newUserPassword.length < 6 -> {
                        formError = context.getString(R.string.validation_password_length)
                    }
                    newUserFullName.isBlank() -> {
                        formError = context.getString(R.string.validation_fullname_required)
                    }
                    // CORREGIDO: Usar string traducible
                    newUserRole == "admin" && !isSuperAdmin -> {
                        formError = context.getString(R.string.user_creation_no_admin_permission)
                    }
                    newUserRole.isBlank() || !listOf("admin", "cambrer").contains(newUserRole) -> {
                        formError = context.getString(R.string.validation_role_required)
                    }
                    else -> {
                        formError = null
                        isCreatingUser = true
                        scope.launch {
                            viewModel.createUser(
                                email = newUserEmail,
                                password = newUserPassword,
                                fullName = newUserFullName,
                                role = newUserRole
                            ) { result ->
                                isCreatingUser = false
                                if (result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.user_created_success),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showAddUserDialog = false
                                    formError = null
                                    newUserEmail = ""
                                    newUserPassword = ""
                                    newUserFullName = ""
                                    newUserRole = "cambrer"
                                } else {
                                    formError = result.exceptionOrNull()?.message 
                                        ?: "Error creando usuario"
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // MODIFICAR diálogo de editar usuario
    if (showEditUserDialog && userToEdit != null) {
        OptimizedEditUserDialog(
            user = userToEdit!!,
            fullName = editUserFullName,
            role = editUserRole,
            isActive = editUserIsActive,
            formError = formError,
            isEditing = isEditingUser,
            isSuperAdmin = isSuperAdmin, // NUEVO parámetro
            onFullNameChange = { editUserFullName = it },
            onRoleChange = { editUserRole = it },
            onIsActiveChange = { editUserIsActive = it },
            onDismiss = {
                showEditUserDialog = false
                formError = null
            },
            onUpdateUser = {
                // CORREGIDO: Usar string traducible
                if (editUserRole == "admin" && !isSuperAdmin) {
                    formError = context.getString(R.string.user_creation_no_admin_permission)
                    return@OptimizedEditUserDialog
                }
                
                when {
                    editUserFullName.isBlank() -> {
                        formError = context.getString(R.string.validation_fullname_required)
                    }
                    else -> {
                        formError = null
                        isEditingUser = true
                        scope.launch {
                            val success = viewModel.updateUser(
                                userToEdit!!.id,
                                editUserFullName,
                                editUserRole,
                                editUserIsActive
                            )
                            isEditingUser = false
                            if (success) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.user_updated_success),
                                    Toast.LENGTH_SHORT
                                ).show()
                                showEditUserDialog = false
                                formError = null
                            } else {
                                formError = "Error actualizando usuario"
                            }
                        }
                    }
                }
            }
        )
    }
    
    if (showUserConfigDialog && userToConfig != null) {
        OptimizedUserConfigDialog(
            user = userToConfig!!,
            onDismiss = { 
                showUserConfigDialog = false 
                userToConfig = null
            },
            onUpdateConfig = { updatedUser ->
                scope.launch {
                    try {
                        val configMap = mapOf(
                            "darkThemeEnabled" to updatedUser.darkThemeEnabled,
                            "textSize" to updatedUser.textSize,
                            "language" to updatedUser.language,
                            "textToSpeechEnabled" to updatedUser.textToSpeechEnabled,
                            "accessibilityEnabled" to updatedUser.accessibilityEnabled,
                            "canRequestBreak" to updatedUser.canRequestBreak,
                            "canChangeTheme" to updatedUser.canChangeTheme,
                            "canLogout" to updatedUser.canLogout,
                            "canChangeLanguage" to updatedUser.canChangeLanguage,
                            "canUseTTS" to updatedUser.canUseTTS,
                            "canChangeTextSize" to updatedUser.canChangeTextSize,
                            "allowedThemes" to updatedUser.allowedThemes,
                            "allowedLanguages" to updatedUser.allowedLanguages // CORREGIDO: Incluir allowedLanguages
                        )
                        
                        viewModel.updateUserConfig(updatedUser.id, configMap) { success ->
                            if (success) {
                                Toast.makeText(context, context.getString(R.string.user_config_updated), Toast.LENGTH_SHORT).show()
                                viewModel.loadUsers()
                            } else {
                                Toast.makeText(context, context.getString(R.string.user_config_update_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showUserConfigDialog = false
                        userToConfig = null
                    } catch (e: Exception) {
                        Log.e("UsersScreen", "Error guardando configuración: ${e.message}", e)
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun OptimizedUserCard(
    user: User,
    onEdit: () -> Unit,
    onConfig: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roleColor = remember(user.role) {
        when (user.role) {
            "admin" -> Color(0xFF3F51B5)
            else -> Color(0xFF4CAF50)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(color = roleColor, shape = MaterialShapes.Cookie9Sided.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = stringResource(
                            if (user.role == "admin") R.string.user_role_display_admin 
                            else R.string.user_role_display_waiter
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = roleColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.admin_edit),
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                OutlinedButton(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onConfig,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    shapes = ButtonDefaults.shapes(),
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.admin_delete),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedAddUserDialog(
    email: String,
    password: String,
    fullName: String,
    role: String,
    formError: String?,
    isCreating: Boolean,
    isSuperAdmin: Boolean, // NUEVO parámetro
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateUser: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { 
            Text(
                stringResource(R.string.add_user_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                formError?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                OutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(R.string.add_user_email)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text(stringResource(R.string.add_user_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text(stringResource(R.string.add_user_fullname)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = stringResource(R.string.user_role_label),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                // MODIFICADO: Condicional para mostrar opciones según permisos
                if (isSuperAdmin) {
                    // Super admin puede crear tanto camareros como administradores
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OptimizedRoleChip(
                            role = "cambrer",
                            roleDisplay = stringResource(R.string.user_role_display_waiter),
                            selected = role == "cambrer",
                            onClick = { onRoleChange("cambrer") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OptimizedRoleChip(
                            role = "admin", 
                            roleDisplay = stringResource(R.string.user_role_display_admin),
                            selected = role == "admin",
                            onClick = { onRoleChange("admin") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Admin normal solo puede crear camareros
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            OptimizedRoleChip(
                                role = "cambrer",
                                roleDisplay = stringResource(R.string.user_role_display_waiter),
                                selected = true,
                                onClick = { /* No hacer nada, solo camareros permitidos */ },
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = stringResource(R.string.user_creation_waiter_only), // CORREGIDO
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = onCreateUser,
                enabled = !isCreating,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                if (isCreating) {
                    LoadingIndicator(modifier = Modifier.size(28.dp), color = LocalContentColor.current)
                } else {
                    Text(stringResource(R.string.add_user_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text(
                    stringResource(R.string.add_user_cancel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    )
}

// ACTUALIZAR OptimizedRoleChip para incluir el modifier:
@Composable
fun OptimizedRoleChip(
    role: String,
    roleDisplay: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier // AÑADIR parámetro modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(roleDisplay) },
        modifier = modifier, // USAR el modifier
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizedEditUserDialog(
    user: User,
    fullName: String,
    role: String,
    isActive: Boolean,
    formError: String?,
    isEditing: Boolean,
    isSuperAdmin: Boolean, // NUEVO parámetro
    onFullNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onUpdateUser: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { 
            Text(
                text = stringResource(R.string.user_edit_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                formError?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                
                Text(
                    text = stringResource(R.string.user_email_value, user.email),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.user_id_value, user.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text(stringResource(R.string.add_user_fullname)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Text(
                    text = stringResource(R.string.user_role_label),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                
                // MODIFICADO: Condicional para edición de roles
                if (isSuperAdmin) {
                    // Super admin puede cambiar entre todos los roles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OptimizedRoleChip(
                            role = "cambrer",
                            roleDisplay = stringResource(R.string.user_role_display_waiter),
                            selected = role == "cambrer",
                            onClick = { onRoleChange("cambrer") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        OptimizedRoleChip(
                            role = "admin",
                            roleDisplay = stringResource(R.string.user_role_display_admin),
                            selected = role == "admin",
                            onClick = { onRoleChange("admin") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Admin normal solo puede cambiar camareros, no puede tocar otros admins
                    if (user.role == "admin") {
                        // Si está editando un admin y no es super admin, mostrar solo lectura
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.user_role_display_admin),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                                
                                Text(
                                    text = stringResource(R.string.user_edit_no_admin_permission), // CORREGIDO
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Puede editar camareros pero no cambiarlos a admin
                        OptimizedRoleChip(
                            role = "cambrer",
                            roleDisplay = stringResource(R.string.user_role_display_waiter),
                            selected = true,
                            onClick = { /* Solo camareros permitidos */ },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Solo permitir cambiar estado si tiene permisos o no es un admin
                if (isSuperAdmin || user.role != "admin") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.user_state_active),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        AdminSwitch(
                            checked = isActive,
                            onCheckedChange = onIsActiveChange
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = onUpdateUser,
                enabled = !isEditing && (isSuperAdmin || user.role != "admin"),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                if (isEditing) {
                    LoadingIndicator(modifier = Modifier.size(28.dp), color = LocalContentColor.current)
                } else {
                    Text(stringResource(R.string.admin_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text(
                    stringResource(R.string.admin_cancel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    )
}

@Composable
fun OptimizedUserConfigDialog(
    user: User,
    onDismiss: () -> Unit,
    onUpdateConfig: (User) -> Unit
) {
    var updatedUser by remember { mutableStateOf(user) }
    var isLoadingConfig by remember { mutableStateOf(false) }
    
    // CORREGIDO: Usar TODOS los temas disponibles de ThemeHelper
    val availableThemes = remember { 
        cat.dam.mamadou.etable.utils.ThemeHelper.AVAILABLE_THEMES
    }
    
    // CORREGIDO: Usar TODOS los idiomas disponibles
    val availableLanguages = remember {
        listOf("es", "ca", "en")
    }
    
    var selectedThemes by remember { 
        mutableStateOf(user.allowedThemes) 
    }
    
    var selectedLanguages by remember {
        mutableStateOf(user.allowedLanguages)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { 
            Text(
                stringResource(R.string.user_config_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            ) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isLoadingConfig) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingIndicator()
                    }
                } else {
                    // Configuraciones básicas del usuario
                    SectionTitle(title = stringResource(R.string.user_config_current_config))
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_dark_theme),
                        checked = updatedUser.darkThemeEnabled,
                        onCheckedChange = { updatedUser = updatedUser.copy(darkThemeEnabled = it) }
                    )
                    
                    Text(
                        text = stringResource(R.string.user_config_text_size, updatedUser.textSize),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = updatedUser.textSize,
                        onValueChange = { updatedUser = updatedUser.copy(textSize = it) },
                        valueRange = 12f..24f,
                        steps = 11
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Idioma actual
                    SectionTitle(title = stringResource(R.string.user_config_current_language))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OptimizedLanguageChip(
                            language = "es",
                            languageDisplay = "Español",
                            selected = updatedUser.language == "es",
                            onClick = { updatedUser = updatedUser.copy(language = "es") }
                        )
                        
                        OptimizedLanguageChip(
                            language = "ca",
                            languageDisplay = "Català",
                            selected = updatedUser.language == "ca",
                            onClick = { updatedUser = updatedUser.copy(language = "ca") }
                        )
                        
                        OptimizedLanguageChip(
                            language = "en",
                            languageDisplay = "English",
                            selected = updatedUser.language == "en",
                            onClick = { updatedUser = updatedUser.copy(language = "en") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Permisos de accesibilidad
                    SectionTitle(title = stringResource(R.string.user_config_accessibility))
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_tts_enabled),
                        checked = updatedUser.textToSpeechEnabled,
                        onCheckedChange = { updatedUser = updatedUser.copy(textToSpeechEnabled = it) }
                    )
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_accessibility_enabled),
                        checked = updatedUser.accessibilityEnabled,
                        onCheckedChange = { updatedUser = updatedUser.copy(accessibilityEnabled = it) }
                    )
                    
                    // Permisos de funcionalidades
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_allow_break_request),
                        checked = updatedUser.canRequestBreak,
                        onCheckedChange = { updatedUser = updatedUser.copy(canRequestBreak = it) }
                    )
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_allow_theme_change),
                        checked = updatedUser.canChangeTheme,
                        onCheckedChange = { updatedUser = updatedUser.copy(canChangeTheme = it) }
                    )
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_allow_language_change),
                        checked = updatedUser.canChangeLanguage,
                        onCheckedChange = { updatedUser = updatedUser.copy(canChangeLanguage = it) }
                    )
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_allow_textsize),
                        checked = updatedUser.canChangeTextSize,
                        onCheckedChange = { updatedUser = updatedUser.copy(canChangeTextSize = it) }
                    )
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_allow_tts),
                        checked = updatedUser.canUseTTS,
                        onCheckedChange = { updatedUser = updatedUser.copy(canUseTTS = it) }
                    )
                    
                    ConfigSwitchItem(
                        title = stringResource(R.string.user_config_allow_logout),
                        checked = updatedUser.canLogout,
                        onCheckedChange = { updatedUser = updatedUser.copy(canLogout = it) }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // CORREGIDO: Sección de temas permitidos con TODOS los temas
                    SectionTitle(title = stringResource(R.string.user_config_allowed_themes))
                    Text(
                        text = stringResource(R.string.user_config_select_themes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    // ALTERNATIVA: Usando LazyColumn con LazyRow para mejor rendimiento
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 400.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dividir temas en filas de 2
                            val chunkedThemes = availableThemes.chunked(2)
                            
                            items(chunkedThemes) { themeRow ->
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(themeRow) { themeString ->
                                        Box(
                                            modifier = Modifier.width(180.dp) // Ancho fijo para cada chip
                                        ) {
                                            ThemeSelectionChip(
                                                theme = themeString,
                                                selected = selectedThemes.contains(themeString),
                                                onClick = {
                                                    selectedThemes = if (selectedThemes.contains(themeString)) {
                                                        selectedThemes - themeString
                                                    } else {
                                                        selectedThemes + themeString
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // CORREGIDO: Sección de idiomas permitidos
                    SectionTitle(title = stringResource(R.string.user_config_allowed_languages))
                    Text(
                        text = stringResource(R.string.user_config_select_languages),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableLanguages.forEach { languageCode ->
                            val languageName = when (languageCode) {
                                "es" -> "Español"
                                "ca" -> "Català"
                                "en" -> "English"
                                else -> languageCode
                            }
                            
                            LanguageSelectionChip(
                                language = languageCode,
                                languageName = languageName,
                                selected = selectedLanguages.contains(languageCode),
                                onClick = {
                                    selectedLanguages = if (selectedLanguages.contains(languageCode)) {
                                        selectedLanguages - languageCode
                                    } else {
                                        selectedLanguages + languageCode
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = { 
                    // CORREGIDO: Incluir los temas e idiomas seleccionados
                    val finalUser = updatedUser.copy(
                        allowedThemes = selectedThemes,
                        allowedLanguages = selectedLanguages
                    )
                    onUpdateConfig(finalUser)
                },
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.user_config_save))
            }
        },
        dismissButton = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// CORREGIDO: Mejorar ThemeSelectionChip para usar nombres traducidos
@Composable
fun ThemeSelectionChip(
    theme: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    
    val themeColor = when (theme) {
        "LIGHT" -> Color(0xFF4CAF50)
        "DARK" -> Color(0xFF424242)
        "OCEAN" -> Color(0xFF0277BD)
        "SUNSET" -> Color(0xFFE64A19)
        "NATURE" -> Color(0xFF689F38)
        "PURPLE" -> Color(0xFF7B1FA2)
        "MONOCHROME" -> Color(0xFF9E9E9E)
        "AUTUMN" -> Color(0xFFE65100)
        "VINTAGE" -> Color(0xFF795548)
        "FIRE" -> Color(0xFFD50000)
        "MIDNIGHT" -> Color(0xFF1A237E)
        "FOREST" -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }
    
    // CORREGIDO: Usar ThemeHelper para obtener nombres traducidos
    val themeName = cat.dam.mamadou.etable.utils.ThemeHelper.getLocalizedThemeName(context, theme)
    
    val backgroundColor = if (selected) themeColor.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (selected) themeColor else Color.LightGray
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = themeColor
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Indicador visual del tema
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(themeColor.copy(alpha = 0.8f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "T",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = themeName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if(selected) themeColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Componentes auxiliares que faltaban
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ConfigSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        AdminSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun OptimizedLanguageChip(
    language: String,
    languageDisplay: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(languageDisplay) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun LanguageSelectionChip(
    language: String,
    languageName: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val languageColor = when (language) {
        "es" -> Color(0xFFE53935)
        "ca" -> Color(0xFF1976D2)
        "en" -> Color(0xFF388E3C)
        else -> MaterialTheme.colorScheme.primary
    }
    
    val backgroundColor = if (selected) languageColor.copy(alpha = 0.2f) else Color.Transparent
    val borderColor = if (selected) languageColor else Color.LightGray
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = 1.dp,
            color = borderColor
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = languageColor
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Indicador visual del idioma
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(languageColor.copy(alpha = 0.8f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = language.uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = languageName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if(selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = if(selected) languageColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
