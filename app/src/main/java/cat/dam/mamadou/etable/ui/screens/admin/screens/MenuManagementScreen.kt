package cat.dam.mamadou.etable.ui.screens.admin.screens

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cat.dam.mamadou.etable.ui.screens.admin.components.*
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuCategories
import cat.dam.mamadou.etable.ui.screens.admin.models.TableStates
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.MenuViewModel
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.MenuImageUploader
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.TableStateUtils
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Context
import android.content.Intent
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.components.PermissionDialog
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.MenuViewModelFactory
import cat.dam.mamadou.etable.utils.PermissionsManager

@Composable
private fun UseBreakSoundOnlyListener(firebaseManager: FirebaseManager) {
    DisposableEffect(Unit) {
        BreakNotificationManager.disableModalNotifications()
        onDispose { }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MenuManagementScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = LocalContext.current as? Activity
    val firebaseManager = remember { FirebaseManager(context) }
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    
    val menuViewModel: MenuViewModel = viewModel(factory = MenuViewModelFactory(context))
    
    // Variables relacionadas con el menú
    val menuItems by menuViewModel.menuItems.collectAsState()
    val isLoadingMenu by menuViewModel.isLoading.collectAsState()
    val menuError by menuViewModel.error.collectAsState()
    
    var selectedCategory by remember { mutableStateOf(MenuCategories.PRIMERS) }
    var showMenuDialog by remember { mutableStateOf(false) }
    var editingMenuItem by remember { mutableStateOf<MenuItem?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    
    // Variables relacionadas con las mesas
    var tables by remember { mutableStateOf<List<Mesa>>(emptyList()) }
    var isLoadingTables by remember { mutableStateOf(true) }
    var showTableDialog by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<Mesa?>(null) }
    var selectedTableState by remember { mutableStateOf(TableStates.LLIURE) }
    var selectedTab by remember { mutableStateOf(0) }
    
    // IMPORTANTE: Variables para manejo de cámara y permisos
    var tempCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var shouldTakePhoto by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var isPermissionPermanentlyDeclined by remember { mutableStateOf(false) }

    // Launcher para selección de imágenes de galería
    val menuImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            editingMenuItem = editingMenuItem?.copy(imageUri = it)
        }
    }
    
    // Launcher para captura de imágenes desde cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraImageUri?.let { uri ->
                editingMenuItem = editingMenuItem?.copy(imageUri = uri)
            }
        }
    }
    
    // Launcher para solicitud de permiso de cámara
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted == true) {
            // Marcar que debemos tomar una foto cuando tengamos el permiso
            shouldTakePhoto = true
        } else {
            isPermissionPermanentlyDeclined = !(activity?.let {
                PermissionsManager.shouldShowRequestPermissionRationale(
                    it,
                    PermissionsManager.PermissionType.CAMERA
                )
            } ?: false)
            
            if (!isPermissionPermanentlyDeclined) {
                showCameraPermissionDialog = false
                Toast.makeText(context, context.getString(R.string.camera_permission_rationale), Toast.LENGTH_SHORT).show()
            } else {
                showCameraPermissionDialog = true
            }
        }
    }
    
    // Función para crear un archivo de imagen temporal
    fun createTempImageFile(): Uri? {
        return try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            
            val image = java.io.File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
            
            // Convertir el archivo a Uri con FileProvider
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                image
            )
        } catch (e: Exception) {
            Log.e("MenuManagementScreen", "Error creando archivo temporal: ${e.message}", e)
            null
        }
    }
    
    // Usar LaunchedEffect para manejar la toma de fotos
    LaunchedEffect(shouldTakePhoto) {
        if (shouldTakePhoto) {
            val uri = createTempImageFile()
            tempCameraImageUri = uri
            
            if (uri != null) {
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, context.getString(R.string.menu_management_camera_error), Toast.LENGTH_SHORT).show()
            }
            
            shouldTakePhoto = false
        }
    }
    
    // Función para manejar la solicitud de toma de fotos
    fun handleTakePhoto() {
        if (activity != null) {
            if (PermissionsManager.isPermissionGranted(context, PermissionsManager.PermissionType.CAMERA)) {
                // Activar la bandera para tomar foto
                shouldTakePhoto = true
            } else {
                if (PermissionsManager.shouldShowRequestPermissionRationale(
                        activity,
                        PermissionsManager.PermissionType.CAMERA
                    )
                ) {
                    showCameraPermissionDialog = true
                } else {
                    PermissionsManager.requestPermission(
                        PermissionsManager.PermissionType.CAMERA,
                        cameraPermissionLauncher
                    )
                }
            }
        }
    }
    
    // Diálogo para solicitar permiso de cámara
    if (showCameraPermissionDialog) {
        PermissionDialog(
            permissionTextProvider = PermissionsManager.getRationaleMessage(
                context,
                PermissionsManager.PermissionType.CAMERA
            ),
            isPermanentlyDeclined = isPermissionPermanentlyDeclined,
            onDismiss = { showCameraPermissionDialog = false },
            onOkClick = {
                showCameraPermissionDialog = false
                activity?.let {
                    PermissionsManager.requestPermission(
                        PermissionsManager.PermissionType.CAMERA,
                        cameraPermissionLauncher
                    )
                }
            },
            onGoToAppSettingsClick = {
                showCameraPermissionDialog = false
                PermissionsManager.openAppSettings(context)
            }
        )
    }

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        menuViewModel.loadMenuItems(firebaseManager)
    }

    // Un único listener de mesas; Realtime ya lo refresca tras cada alta, edición o borrado
    DisposableEffect(Unit) {
        val tablesListener = firebaseManager.tableManager.listenToTables { tablesList ->
            tables = tablesList
            isLoadingTables = false
        }
        onDispose { tablesListener.remove() }
    }

    LaunchedEffect(menuError) {
        menuError?.let {
            Toast.makeText(context, context.getString(R.string.menu_management_save_error, it), Toast.LENGTH_SHORT).show()
            Log.e("MenuManagementScreen", "Error: $it")
        }
    }

    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    
    Scaffold(
        floatingActionButton = {
            MediumFloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        editingMenuItem = MenuItem(
                            id = "",
                            name = "",
                            category = selectedCategory,
                            isAvailable = true
                        )
                        showMenuDialog = true
                    } else {
                        editingTable = Mesa(
                            id = 0,
                            estado = selectedTableState.toString(),
                            clientes = 0
                        )
                        showTableDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) 
                        stringResource(R.string.menu_management_add_item) 
                    else 
                        stringResource(R.string.table_management_add_table),
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
            if (isLandscape) {
                LandscapeLayout(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    menuContent = {
                        OptimizedMenuManagementContent(
                            menuItems = menuItems,
                            isLoading = isLoadingMenu,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            onEditItem = { 
                                editingMenuItem = it
                                showMenuDialog = true
                            },
                            onDeleteItem = { menuItem ->
                                scope.launch {
                                    menuViewModel.deleteMenuItem(firebaseManager, menuItem)
                                }
                            }
                        )
                    },
                    tableContent = {
                        OptimizedTableManagementContent(
                            tables = tables,
                            isLoading = isLoadingTables,
                            selectedState = selectedTableState,
                            onStateSelected = { selectedTableState = it },
                            onEditTable = { 
                                editingTable = it
                                showTableDialog = true
                            },
                            onDeleteTable = { table ->
                                scope.launch {
                                    deleteTable(firebaseManager, table.id) { success ->
                                        if (success) {
                                            Toast.makeText(context, context.getString(R.string.table_management_deleted_success), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.table_management_delete_error), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    MenuTablesToggle(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    key(selectedTab) {
                        when (selectedTab) {
                            0 -> OptimizedMenuManagementContent(
                                menuItems = menuItems,
                                isLoading = isLoadingMenu,
                                selectedCategory = selectedCategory,
                                onCategorySelected = { selectedCategory = it },
                                onEditItem = { 
                                    editingMenuItem = it
                                    showMenuDialog = true
                                },
                                onDeleteItem = { menuItem ->
                                    scope.launch {
                                        menuViewModel.deleteMenuItem(firebaseManager, menuItem)
                                    }
                                }
                            )
                            1 -> OptimizedTableManagementContent(
                                tables = tables,
                                isLoading = isLoadingTables,
                                selectedState = selectedTableState,
                                onStateSelected = { selectedTableState = it },
                                onEditTable = { 
                                    editingTable = it
                                    showTableDialog = true
                                },
                                onDeleteTable = { table ->
                                    scope.launch {
                                        deleteTable(firebaseManager, table.id) { success ->
                                            if (success) {
                                                Toast.makeText(context, context.getString(R.string.table_management_deleted_success), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, context.getString(R.string.table_management_delete_error), Toast.LENGTH_SHORT).show()
                                            }
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
    
    if (showMenuDialog) {
        OptimizedMenuItemDialog(
            item = editingMenuItem,
            onDismiss = { 
                showMenuDialog = false 
                uploadError = null
                isUploading = false
            },
            onSave = { menuItem ->
                scope.launch {
                    try {
                        isUploading = true
                        uploadError = null
                        
                        val isNewItem = menuItem.id.isEmpty()
                        
                        val itemWithImage = if (menuItem.imageUri != null) {
                            uploadMenuItemImage(
                                context = context,
                                menuItem = menuItem,
                                onError = { error -> 
                                    uploadError = error
                                    isUploading = false
                                }
                            )
                        } else {
                            menuItem
                        }
                        
                        if (itemWithImage != null) {
                            if (isNewItem) {
                                menuViewModel.addMenuItem(firebaseManager, itemWithImage) { success ->
                                    if (success) {
                                        showMenuDialog = false
                                    }
                                    isUploading = false
                                }
                            } else {
                                menuViewModel.updateMenuItem(firebaseManager, itemWithImage) { success ->
                                    if (success) {
                                        showMenuDialog = false
                                    }
                                    isUploading = false
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("MenuManagementScreen", context.getString(R.string.menu_management_save_error, ""), e)
                        uploadError = context.getString(R.string.menu_management_save_error, e.message)
                        isUploading = false
                    }
                }
            },
            onPickImage = {
                menuImagePickerLauncher.launch("image/*")
            },
            onTakePhoto = {
                handleTakePhoto()  // Usar la función única handleTakePhoto
            },
            isUploading = isUploading,
            uploadError = uploadError
        )
    }
    
    if (showTableDialog && editingTable != null) {
        OptimizedTableDialog(
            table = editingTable,
            onDismiss = { showTableDialog = false },
            onSave = { mesa ->
                scope.launch {
                    val isNewTable = mesa.id == 0
                    
                    val nextId = getNextAvailableTableId(tables)
                    val newTable = mesa.copy(id = nextId)
                    
                    if (isNewTable) {
                        addTable(firebaseManager, newTable) { success ->
                            if (success) {
                                showTableDialog = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.table_management_add_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        updateTable(firebaseManager, mesa) { success ->
                            if (success) {
                                showTableDialog = false
                            } else {
                                Toast.makeText(context, context.getString(R.string.table_management_update_error), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun LandscapeLayout(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    menuContent: @Composable () -> Unit,
    tableContent: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.table_management_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            textAlign = TextAlign.Center
        )
        
        MenuTablesToggle(
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            key(selectedTab) {
                when (selectedTab) {
                    0 -> menuContent()
                    1 -> tableContent()
                }
            }
        }
    }
}

/** Selector Carta / Mesas: ToggleButton conectados, cambio instantáneo. */
@Composable
private fun MenuTablesToggle(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AdminConnectedToggleGroup(
        labels = listOf(
            stringResource(R.string.menu_management_tab),
            stringResource(R.string.table_management_tab)
        ),
        icons = listOf(Icons.Default.Restaurant, Icons.Default.TableBar),
        selectedIndex = selectedTab,
        onSelect = onTabSelected,
        role = androidx.compose.ui.semantics.Role.Tab,
        modifier = modifier
    )
}

private fun getNextAvailableTableId(tables: List<Mesa>): Int {
    val existingIds = tables.map { it.id }.toSet()
    return (1..100).first { it !in existingIds }
}

private suspend fun uploadMenuItemImage(
    context: Context,
    menuItem: MenuItem,
    onError: (String) -> Unit
): MenuItem? {
    return try {
        val imgBBManager = MenuImageUploader(context)
        val imageUri = menuItem.imageUri ?: return menuItem.copy(imageUri = null)
        
        val result = imgBBManager.uploadImage(imageUri)
        if (result.isSuccess) {
            val imageUrl = result.getOrNull() ?: ""
            menuItem.copy(imageUrl = imageUrl, imageUri = null)
        } else {
            onError("No se pudo subir la imagen: ${result.exceptionOrNull()?.message ?: "Error desconocido"}")
            null
        }
    } catch (e: Exception) {
        Log.e("MenuManagementScreen", "Error subiendo imagen: ${e.message}", e)
        onError("Error al subir la imagen: ${e.message}")
        null
    }
}

private fun addTable(
    firebaseManager: FirebaseManager,
    table: Mesa,
    onComplete: (Boolean) -> Unit
) {
    val tableData = mapOf(
        "id" to table.id,
        "name" to (table.nombre ?: "Mesa ${table.id}"),
        "status" to table.estado,
        "customers" to table.clientes,
        "isOccupied" to (table.estado == "OCUPADA" || table.estado == "RESERVADA"),
        "orders" to (table.comandas?.map { 
            mapOf("nombre" to it.nombre, "cantidad" to it.cantidad) 
        } ?: emptyList())
    )
    
    firebaseManager.tableManager.saveTable(tableData) { result ->
        onComplete(result.isSuccess)
    }
}

private fun updateTable(
    firebaseManager: FirebaseManager,
    table: Mesa,
    onComplete: (Boolean) -> Unit
) {
    firebaseManager.tableManager.updateTable(table, onComplete)
}

private fun deleteTable(
    firebaseManager: FirebaseManager,
    tableId: Int,
    onComplete: (Boolean) -> Unit
) {
    firebaseManager.tableManager.deleteTable(tableId, onComplete)
}

@Composable
private fun ShowCameraError(error: String?) {
    val context = LocalContext.current
    if (error != null) {
        LaunchedEffect(error) {
            Toast.makeText(
                context,
                context.getString(R.string.menu_management_camera_error_detail, error),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

private fun getErrorMessageNonComposable(context: Context, errorType: String): String {
    return when(errorType) {
        "camera" -> context.getString(R.string.menu_management_camera_error)
        "upload" -> context.getString(R.string.menu_management_upload_error, "")
        "save" -> context.getString(R.string.menu_management_save_error, "")
        else -> context.getString(R.string.menu_management_unknown_error)
    }
}

private fun showErrorMessageToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private fun showSuccessMessageToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
