@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import cat.dam.mamadou.etable.ui.screens.waiter.components.rememberWaiterReduceMotion
import cat.dam.mamadou.etable.ui.theme.TableStatusColors
import androidx.compose.material.icons.filled.Close
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuCategories
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import cat.dam.mamadou.etable.ui.screens.waiter.components.orders.CategorySelector
import cat.dam.mamadou.etable.ui.screens.waiter.components.orders.ConfirmationDialog
import cat.dam.mamadou.etable.ui.screens.waiter.components.orders.OrderItem as OrderItemComponent
import cat.dam.mamadou.etable.ui.screens.waiter.components.orders.StageSelector
import cat.dam.mamadou.etable.ui.screens.waiter.models.*
import cat.dam.mamadou.etable.utils.firebase.FirebaseMenuManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseTableManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext
import java.util.UUID
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.ModelOrderItem as ModelOrderItem
import cat.dam.mamadou.etable.models.OrderItem
import cat.dam.mamadou.etable.utils.OrderReadyEventBus
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import cat.dam.mamadou.etable.R
import kotlinx.coroutines.CoroutineScope
import java.util.Locale
import cat.dam.mamadou.etable.utils.CategoryTranslator
import cat.dam.mamadou.etable.utils.PermissionsManager
import cat.dam.mamadou.etable.utils.PermissionsManager.PermissionType
import cat.dam.mamadou.etable.ui.components.PermissionDialog
import cat.dam.mamadou.etable.utils.TTSManager

// Lista de categorías optimizada como objeto inmutable
private val CATEGORIAS = listOf("PRIMERS", "SEGONS", "POSTRES", "BEGUDES")

// Función para convertir MenuCategories a String
private fun categoryToString(category: MenuCategories): String {
    return category.name
}

// Función de conversión
private fun ModelOrderItem.toOrderItem(): OrderItem {
    return OrderItem(
        id = this.id,
        name = this.name,
        price = this.price,
        quantity = this.quantity,
        etapa = this.etapa
    )
}

// Añadir esta función para avanzar automáticamente a la siguiente etapa
private fun getNextStage(currentStage: ComandaStage): ComandaStage {
    return when (currentStage) {
        ComandaStage.PRIMERS_BEGUDES -> ComandaStage.SEGONS
        ComandaStage.SEGONS -> ComandaStage.POSTRES
        ComandaStage.POSTRES -> ComandaStage.FINALIZADA
        else -> currentStage
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    mesaId: Int? = null, 
    etapaInicial: String? = null,
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as? Activity
    // CAMBIAR a lazy initialization
    val ttsManager by lazy { TTSManager.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // Managers inicializados una sola vez y de manera eficiente
    val managers = remember {
        val firestore = cat.dam.mamadou.etable.supabase.FirebaseFirestore.getInstance()
        Triple(
            FirebaseOrderManager(firestore),
            FirebaseMenuManager(firestore),
            FirebaseTableManager(firestore)
        )
    }
    val (firebaseOrderManager, firebaseMenuManager, firebaseTableManager) = managers

    // Estados básicos
    var nombreMesa by remember { mutableStateOf("") }
    val platosSeleccionados = remember { mutableStateMapOf<String, Int>() }
    val cookingPoints = remember { mutableStateMapOf<String, CookingPoint?>() }
    val observations = remember { mutableStateMapOf<String, String>() }
    var platosDisponibles by remember { mutableStateOf<Map<String, List<Plato>>>(emptyMap()) }
    var etapaComanda by remember { 
        mutableStateOf(
            when (etapaInicial) {
                "PRIMERS_BEGUDES" -> ComandaStage.PRIMERS_BEGUDES
                "SEGONS" -> ComandaStage.SEGONS
                "POSTRES" -> ComandaStage.POSTRES
                "FINALIZADA" -> ComandaStage.FINALIZADA
                else -> ComandaStage.PRIMERS_BEGUDES
            }
        ) 
    }
    var cargarOrdenExistente by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    val sentItems = remember { mutableStateMapOf<String, Boolean>() }
    
    // Estados de UI
    var categoriaSeleccionada by remember { mutableStateOf(CATEGORIAS[0]) }
    var mostrarDialogoConfirmacion by remember { mutableStateOf(false) }
    var mostrarMensajeExito by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoSeleccion by remember { mutableStateOf(false) }
    var mostrarImagenAmpliada by remember { mutableStateOf(false) }
    var platoSeleccionado by remember { mutableStateOf<Plato?>(null) }
    var platoParaVoz by remember { mutableStateOf<String?>(null) }
    var existingOrderId by remember { mutableStateOf<String?>(null) }

    // Extrae los strings que necesitas al inicio de la función @Composable
    val noPlatesSelectedText = stringResource(R.string.no_dishes_selected)
    val errorSendingOrderText = stringResource(R.string.error_sending_order)

    // Listener de Firebase optimizado
    val tableListener = remember { mutableStateOf<cat.dam.mamadou.etable.supabase.ListenerRegistration?>(null) }
    
    // Launcher para el reconocimiento de voz
    val speechRecognizer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Procesar el resultado del reconocimiento de voz
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0) ?: ""
            platoParaVoz?.let { nombrePlato ->
                // Actualizar el campo de observaciones con el texto reconocido
                observations[nombrePlato] = spokenText
                platoParaVoz = null
            }
        }
    }
    
    // Función para iniciar el reconocimiento de voz
    fun startSpeechToText(nombrePlato: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag()) // Usar idioma del sistema
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.dictate_observations, nombrePlato))
        }
        speechRecognizer.launch(intent)
    }
    
    // Estado para controlar diálogo de permisos
    var showMicrophonePermissionDialog by remember { mutableStateOf(false) }
    var isPermissionPermanentlyDeclined by remember { mutableStateOf(false) }
    
    // Solicitar permiso de micrófono
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted && platoParaVoz != null) {
            // Iniciar reconocimiento de voz si se concede el permiso
            startSpeechToText(platoParaVoz!!)
        } else {
            // Verificar si el permiso fue denegado permanentemente
            if (activity != null) {
                isPermissionPermanentlyDeclined = !PermissionsManager.shouldShowRequestPermissionRationale(
                    activity,
                    PermissionsManager.PermissionType.MICROPHONE
                )
            } else {
                isPermissionPermanentlyDeclined = false
            }
            
            if (!isPermissionPermanentlyDeclined) {
                showMicrophonePermissionDialog = false
                mensajeError = context.getString(R.string.mic_permission_required)
            } else {
                showMicrophonePermissionDialog = true
            }
        }
    }
    
    // Función para manejar clic en botón de micrófono
    fun handleSpeechToTextClick(nombrePlato: String) {
        if (PermissionsManager.isPermissionGranted(context, PermissionType.MICROPHONE)) {
            startSpeechToText(nombrePlato)
        } else {
            // AQUÍ es donde debe pedirlo
            PermissionsManager.requestPermission(
                PermissionType.MICROPHONE,
                requestPermissionLauncher
            )
        }
    }
    
    // Diálogo para solicitar permiso de micrófono si es necesario
    if (showMicrophonePermissionDialog) {
        PermissionDialog(
            permissionTextProvider = PermissionsManager.getRationaleMessage(
                context,
                PermissionType.MICROPHONE
            ),
            isPermanentlyDeclined = isPermissionPermanentlyDeclined,
            onDismiss = { showMicrophonePermissionDialog = false },
            onOkClick = {
                showMicrophonePermissionDialog = false
                if (activity != null) {
                    PermissionsManager.requestPermission(
                        PermissionType.MICROPHONE,
                        requestPermissionLauncher
                    )
                }
            },
            onGoToAppSettingsClick = {
                showMicrophonePermissionDialog = false
                PermissionsManager.openAppSettings(context)
            }
        )
    }
    
    // Categorías visibles basadas en la etapa actual (calculadas una sola vez por etapa)
    val categoriasVisibles by remember(etapaComanda) {
        derivedStateOf {
            when (etapaComanda) {
                ComandaStage.PRIMERS_BEGUDES -> listOf("PRIMERS", "BEGUDES")
                ComandaStage.SEGONS -> listOf("SEGONS")
                ComandaStage.POSTRES -> listOf("POSTRES")
                else -> emptyList()
            }
        }
    }

    val categoriasVisiblesTraducidas by remember(categoriasVisibles) {
        derivedStateOf {
            categoriasVisibles.map { category ->
                CategoryTranslator.getTranslatedCategory(context, category)
            }
        }
    }

    val categoriaSeleccionadaTraducida by remember(categoriaSeleccionada) {
        derivedStateOf {
            CategoryTranslator.getTranslatedCategory(context, categoriaSeleccionada)
        }
    }
    
    // Asegurar que la categoría seleccionada esté entre las visibles
    LaunchedEffect(categoriasVisibles) {
        if (categoriasVisibles.isNotEmpty() && categoriaSeleccionada !in categoriasVisibles) {
            categoriaSeleccionada = categoriasVisibles[0]
        }
    }
    
    // Platos de la categoría actual (calculados una sola vez por cambio)
    val platosCategoria by remember(categoriaSeleccionada, platosDisponibles) {
        derivedStateOf {
            platosDisponibles[categoriaSeleccionada]?.filter { it.disponible } ?: emptyList()
        }
    }

    // Cargar información de la mesa de manera eficiente
    LaunchedEffect(mesaId) {
        if (mesaId == null) return@LaunchedEffect
        
        withContext(Dispatchers.IO) {
            firebaseTableManager.getTableById(mesaId) { table ->
                if (table != null) {
                    nombreMesa = table["name"] as? String ?: table["nombre"] as? String 
                        ?: "Mesa $mesaId"
                } else {
                    nombreMesa = "Mesa $mesaId"
                }
            }
        }
    }
    
    // Verificar órdenes existentes de manera optimizada
    LaunchedEffect(mesaId) {
        if (mesaId == null) return@LaunchedEffect
        isLoading = true

        try {
            withContext(Dispatchers.IO) {
                val snapshot = cat.dam.mamadou.etable.supabase.FirebaseFirestore.getInstance()
                    .collection("orders")
                    .whereEqualTo("tableId", mesaId)
                    .whereEqualTo("status", OrderStatus.PENDING.toString())
                    .get()
                    .await()
                
                withContext(Dispatchers.Main) {
                    if (!snapshot.isEmpty) {
                        mostrarDialogoSeleccion = true
                        
                        // Configurar etapa basada en orden existente o etapaInicial
                        val document = snapshot.documents.firstOrNull()
                        existingOrderId = document?.id
                        
                        // Usar etapaInicial si está disponible, de lo contrario usar la etapa en Firebase
                        if (etapaInicial == null) {
                            etapaComanda = when (document?.getString("currentStage")) {
                                "SEGONS" -> ComandaStage.SEGONS
                                "POSTRES" -> ComandaStage.POSTRES
                                else -> ComandaStage.PRIMERS_BEGUDES
                            }
                        }
                    } else {
                        cargarOrdenExistente = false
                        platosSeleccionados.clear()
                        
                        // Si no hay órdenes existentes, mantener etapaInicial si está disponible
                        if (etapaInicial == null) {
                            etapaComanda = ComandaStage.PRIMERS_BEGUDES
                        }
                    }
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("OrdersScreen", "Error cargando órdenes: ${e.message}")
                cargarOrdenExistente = false
                platosSeleccionados.clear()
                isLoading = false
            }
        }
    }

    // Escuchar cambios en órdenes de manera eficiente
    DisposableEffect(mesaId, cargarOrdenExistente) {
        if (mesaId != null && cargarOrdenExistente) {
            // Implementar una consulta optimizada que sólo escucha cambios relevantes
            val listener = cat.dam.mamadou.etable.supabase.FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("tableId", mesaId)
                .whereEqualTo("status", OrderStatus.PENDING.toString())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("OrdersScreen", "Error en listener: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot?.isEmpty != false) return@addSnapshotListener
                    
                    // Actualizar con un único batch en lugar de elementos individuales
                    val newPlatosSeleccionados = mutableMapOf<String, Int>()
                    
                    try {
                        snapshot.documents.forEach { document ->
                            val itemsData = document.get("items") as? List<Map<String, Any>> ?: emptyList()
                            
                            itemsData.forEach { itemData ->
                                val nombre = itemData["name"] as? String ?: ""
                                val cantidad = (itemData["quantity"] as? Number)?.toInt() ?: 0
                                
                                if (nombre.isNotEmpty() && cantidad > 0) {
                                    newPlatosSeleccionados[nombre] = 
                                        (newPlatosSeleccionados[nombre] ?: 0) + cantidad
                                }
                            }
                        }
                        
                        // Actualizar estados solo si hay cambios
                        if (platosSeleccionados != newPlatosSeleccionados) {
                            platosSeleccionados.clear()
                            platosSeleccionados.putAll(newPlatosSeleccionados)
                        }
                    } catch (e: Exception) {
                        Log.e("OrdersScreen", "Error procesando datos: ${e.message}")
                    }
                }

            tableListener.value = listener
        } else {
            platosSeleccionados.clear()
            tableListener.value?.remove()
            tableListener.value = null
        }

        onDispose {
            tableListener.value?.remove()
            tableListener.value = null
        }
    }

    // Función optimizada para obtener elementos de la etapa current
    val itemsForCurrentStage by remember(etapaComanda, platosSeleccionados, platosDisponibles) {
        derivedStateOf {
            // Especificar explícitamente el tipo para evitar inferencias incorrectas
            val currentCategories: List<String> = when (etapaComanda) {
                ComandaStage.PRIMERS_BEGUDES -> listOf("PRIMERS", "BEGUDES")
                ComandaStage.SEGONS -> listOf("SEGONS")
                ComandaStage.POSTRES -> listOf("POSTRES")
                else -> emptyList()
            }
            
            val currentPlatos = platosDisponibles
                .filterKeys { it in currentCategories }
                .values
                .flatten()
                .map { it.nombre }
                .toSet()
                
            platosSeleccionados.filter { (nombre, cantidad) ->
                cantidad > 0 && nombre in currentPlatos
            }
        }
    }

    // Función optimizada para avanzar etapas
    fun avanzarEtapaComanda() {
        val currentItems = itemsForCurrentStage
        
        if (currentItems.isNotEmpty()) {
            mostrarDialogoConfirmacion = true
        } else {
            // Avanzar a la siguiente etapa sin preguntar si no hay items nuevos
            when (etapaComanda) {
                ComandaStage.PRIMERS_BEGUDES -> etapaComanda = ComandaStage.SEGONS
                ComandaStage.SEGONS -> etapaComanda = ComandaStage.POSTRES
                ComandaStage.POSTRES -> {
                    etapaComanda = ComandaStage.FINALIZADA
                    
                    // Usar la función compartida para actualizar la mesa
                    if (mesaId != null) {
                        actualizarMesaFinalizada(mesaId, context, firebaseTableManager, scope)
                    }
                    
                    mostrarMensajeExito = true
                    scope.launch {
                        delay(1500)
                        platosSeleccionados.clear()
                        sentItems.clear()
                    }
                }
                else -> {}
            }
        }
    }

    // Cargar platos de menú una sola vez con caché
    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            firebaseMenuManager.loadMenuItemsForOrders { menuItems ->
                val platosPorCategoria = menuItems
                    .asSequence()
                    .groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.map { menuItem ->
                            Plato(
                                id = menuItem.id,
                                nombre = menuItem.name,
                                categoria = categoryToString(menuItem.category), // Convertir de MenuCategories a String
                                imagenUrl = menuItem.imageUrl,
                                disponible = menuItem.isAvailable,
                                requiresCookingPoint = menuItem.requiresCookingPoint // Añadir esta propiedad
                            )
                        }
                    }
                
                if (platosPorCategoria.isNotEmpty()) {
                    // Convertir las claves de MenuCategories a String
                    platosDisponibles = platosPorCategoria.map { (category, platos) ->
                        categoryToString(category) to platos
                    }.toMap()
                }
                isLoading = false
            }
        }
    }

    // Añadir este LaunchedEffect para avanzar automáticamente cuando se carga con etapa específica
    LaunchedEffect(mesaId, etapaInicial) {
        if (mesaId != null && etapaInicial != null && etapaInicial != "FINALIZADA") {
            // Pequeño retraso para permitir que la UI se cargue primero
            delay(500)
            
            // Determinar la etapa actual en formato tag
            val stageTag = when (etapaComanda) {
                ComandaStage.PRIMERS_BEGUDES -> "PRIMERS_BEGUDES"
                ComandaStage.SEGONS -> "SEGONS"
                ComandaStage.POSTRES -> "POSTRES"
                ComandaStage.FINALIZADA -> "FINALIZADA"
                else -> null
            }
            
            if (stageTag != null) {
                try {
                    // Marcar la etapa actual como completada
                    isLoading = true
                    firebaseOrderManager.markStageAsCompleted(
                        orderId = existingOrderId,
                        tableId = mesaId,
                        stageTag = stageTag
                    ) { success ->
                        if (success) {
                            Log.d("OrdersScreen", "Avance automático: Etapa $stageTag marcada como completada")
                            
                            // Avanzar a la siguiente etapa en la UI
                            etapaComanda = when (etapaComanda) {
                                ComandaStage.PRIMERS_BEGUDES -> ComandaStage.SEGONS
                                ComandaStage.SEGONS -> ComandaStage.POSTRES
                                ComandaStage.POSTRES -> ComandaStage.FINALIZADA
                                else -> etapaComanda
                            }
                            
                            if (etapaComanda == ComandaStage.FINALIZADA) {
                                mostrarMensajeExito = true
                            }
                        }
                        isLoading = false
                    }
                } catch (e: Exception) {
                    Log.e("OrdersScreen", "Error en avance automático: ${e.message}")
                    isLoading = false
                }
            }
        }
    }

    // Reemplazar este bloque de LaunchedEffect
    LaunchedEffect(Unit) {
        // Verificar si hay etapa guardada en preferencias
        if (mesaId != null) {
            val sharedPrefs = context.getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
            val savedStage = sharedPrefs.getString("table_$mesaId", null)
            
            if (savedStage != null && savedStage != "FINALIZADA") {
                // NO avanzar automáticamente, usar la etapa guardada directamente
                etapaComanda = when (savedStage) {
                    "PRIMERS_BEGUDES" -> ComandaStage.PRIMERS_BEGUDES
                    "SEGONS" -> ComandaStage.SEGONS
                    "POSTRES" -> ComandaStage.POSTRES
                    "FINALIZADA" -> ComandaStage.FINALIZADA
                    else -> etapaComanda
                }
                
                // Si avanzamos a FINALIZADA, actualizar la mesa
                if (etapaComanda == ComandaStage.FINALIZADA) {
                    actualizarMesaFinalizada(mesaId, context, firebaseTableManager, scope)
                }
            }
        }
    }

    // Añadir este bloque después de establecer la etapa inicial

    // Guardar la etapa actual al inicio
    LaunchedEffect(etapaComanda) {
        if (mesaId != null) {
            val etapaTag = when (etapaComanda) {
                ComandaStage.PRIMERS_BEGUDES -> "PRIMERS_BEGUDES"
                ComandaStage.SEGONS -> "SEGONS"
                ComandaStage.POSTRES -> "POSTRES" 
                ComandaStage.FINALIZADA -> "FINALIZADA"
            }
            
            // Guardar la etapa ACTUAL en SharedPreferences
            val sharedPrefs = context.getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("table_$mesaId", etapaTag).apply()
            Log.d("OrdersScreen", "Guardando etapa actual: $etapaTag para mesa $mesaId")
        }
    }

    // Función mejorada para enviar comanda
    suspend fun enviarComanda() {
        if (platosSeleccionados.isEmpty()) {
            mensajeError = noPlatesSelectedText
            return
        }
        
        isLoading = true
        
        val stageTag = when (etapaComanda) {
            ComandaStage.PRIMERS_BEGUDES -> "PRIMERS_BEGUDES"
            ComandaStage.SEGONS -> "SEGONS"
            ComandaStage.POSTRES -> "POSTRES"
            ComandaStage.FINALIZADA -> "FINALIZADA"
        }
        
        // Convertir platosSeleccionados a OrderItems
        val items = platosSeleccionados.mapNotNull { (platoNombre, cantidad) ->
            try {
                // Buscar el plato por nombre en lugar de por ID
                val plato = platosDisponibles.values.flatten().find { it.nombre == platoNombre }
                    ?: throw IllegalStateException("Plato no encontrado: $platoNombre")
                
                // Obtener el punto de cocción para este plato
                val cookingPoint = cookingPoints[platoNombre]
                
                // Obtener observaciones para este plato
                val observation = observations[platoNombre]
                
                // Construir la etiqueta de etapa con punto de cocción y observaciones
                var etapaCompleta = stageTag
                
                // Agregar punto de cocción si existe
                if (cookingPoint != null) {
                    etapaCompleta += "_PUNTO:${cookingPoint.name}"
                }
                
                // Agregar observaciones si existen
                if (!observation.isNullOrBlank()) {
                    etapaCompleta += "_OBS:$observation"
                }
                
                OrderItem(
                    id = plato.id,
                    name = plato.nombre,
                    quantity = cantidad,
                    price = plato.precio,
                    etapa = etapaCompleta
                )
            } catch (e: Exception) {
                Log.e("OrdersScreen", "Error con plato: $platoNombre - ${e.message}")
                mensajeError = "Error: ${e.message}"
                null
            }
        }
        
        // Si hay algún error con los platos, detener el proceso
        if (items.size != platosSeleccionados.size) {
            isLoading = false
            return
        }
        
        try {
            managers.first.saveOrder(
                tableId = mesaId ?: 0,
                tableName = nombreMesa,
                items = items,
                stageTag = stageTag,
                completedStages = emptyList()
            ) { orderId, success ->
                if (success && orderId != null) {
                    // Guardar el ID de la orden para futuras actualizaciones
                    existingOrderId = orderId
                    
                    // Marcar todos los elementos como enviados
                    platosSeleccionados.keys.forEach { platoId ->
                        sentItems[platoId] = true
                    }
                    
                    // TTS ÚNICO para confirmación de envío
                    ttsManager.speakOrderStatus("order_sent_success")
                    
                    mostrarMensajeExito = true
                    // ...existing code...
                } else {
                    mensajeError = errorSendingOrderText
                }
                isLoading = false
            }
        } catch (e: Exception) {
            mensajeError = "Error: ${e.message}"
            isLoading = false
        }
    }

    // UI optimizada
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (nombreMesa.isNotEmpty()) 
                            stringResource(R.string.order_title, nombreMesa)
                        else 
                            stringResource(R.string.order_table_title, mesaId ?: 0),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                actions = {
                    if (mesaId != null) {
                        TextButton(
                            onClick = { mostrarDialogoSeleccion = true }
                        ) {
                            Text(
                                stringResource(R.string.new_order),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (etapaComanda != ComandaStage.FINALIZADA && itemsForCurrentStage.isNotEmpty()) {
                // Extended FAB: acción principal con etiqueta; LoadingIndicator durante el envío existente
                ExtendedFloatingActionButton(
                    text = {
                        Text(
                            text = stringResource(R.string.send_order),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    icon = {
                        if (isLoading) {
                            LoadingIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    onClick = {
                        // TTS ÚNICO para botón enviar
                        ttsManager.speakButtonAction("send_order")

                        scope.launch {
                            enviarComanda()
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    containerColor = when {
                        platosSeleccionados.isEmpty() -> Color(0xFFBDBDBD)
                        etapaComanda == ComandaStage.POSTRES -> Color(0xFF4CAF50)
                        else -> Color(0xFFFFA000)
                    },
                    contentColor = Color.White
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
                modifier = Modifier.fillMaxSize()
            ) {
                // Selector de etapa - vista optimizada
                StageSelector(
                    currentStage = etapaComanda,
                    onStageSelected = { etapaComanda = it }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Content area
                if (etapaComanda != ComandaStage.FINALIZADA) {
                    if (categoriasVisibles.isNotEmpty()) {
                        // Selector de categoría - optimizado para reducir recomposiciones
                        CategorySelector(
                            categories = categoriasVisiblesTraducidas,
                            selectedCategory = categoriaSeleccionadaTraducida,
                            onCategorySelected = { categoriaTraducida ->
                                // Encontrar la categoría original correspondiente
                                val categoriaOriginal = categoriasVisibles.find { category ->
                                    CategoryTranslator.getTranslatedCategory(context, category) == categoriaTraducida
                                } ?: categoriaSeleccionada

                                // Actualizar la categoría seleccionada con el valor original
                                categoriaSeleccionada = categoriaOriginal
                            }
                        )

                        // Lista de platos - optimizada con keys y lazy loading
                        if (platosCategoria.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentPadding = PaddingValues(
                                    start = 16.dp, 
                                    end = 16.dp, 
                                    top = 8.dp,
                                    bottom = 80.dp
                                )
                            ) {
                                itemsIndexed(
                                    items = platosCategoria,
                                    key = { _, plato -> plato.id } // Usar ID como clave estable
                                ) { index, plato ->
                                    val cantidad = platosSeleccionados[plato.nombre] ?: 0
                                    
                                    OrderItemComponent(
                                        plato = plato,
                                        cantidad = cantidad,
                                        onCantidadChanged = { nuevaCantidad ->
                                            // Manejar TTS UNA SOLA VEZ aquí
                                            if (nuevaCantidad > cantidad) {
                                                ttsManager.speakQuantityChange(plato.nombre, nuevaCantidad, true)
                                            } else if (nuevaCantidad < cantidad) {
                                                if (nuevaCantidad == 0) {
                                                    // Usar una función diferente para elementos eliminados
                                                    ttsManager.speakDishSelected("${plato.nombre} eliminado")
                                                } else {
                                                    ttsManager.speakQuantityChange(plato.nombre, nuevaCantidad, false)
                                                }
                                            }
                                            
                                            if (nuevaCantidad > 0) {
                                                platosSeleccionados[plato.nombre] = nuevaCantidad
                                            } else {
                                                platosSeleccionados.remove(plato.nombre)
                                                cookingPoints.remove(plato.nombre)
                                                observations.remove(plato.nombre)
                                            }
                                        },
                                        isSent = sentItems[plato.nombre] == true,
                                        onImageClick = {
                                            // AGREGAR TTS para clic en imagen:
                                            ttsManager.speakDishSelected(plato.nombre)
                                            
                                            // Mostrar imagen ampliada
                                            platoSeleccionado = plato
                                            mostrarImagenAmpliada = true
                                        },
                                        cookingPoint = cookingPoints[plato.nombre],
                                        onCookingPointChanged = { punto ->
                                            cookingPoints[plato.nombre] = punto
                                            // TTS para cambio de punto de cocción
                                            if (punto != null) {
                                                // CAMBIAR esta línea para usar una función que existe:
                                                val mensajePuntoCoccion = "${plato.nombre}: ${punto.getLocalizedString(context)}"
                                                ttsManager.speakDishSelected(mensajePuntoCoccion)
                                            }
                                        },
                                        observations = observations[plato.nombre] ?: "",
                                        onObservationsChanged = { nuevasObservaciones ->
                                            observations[plato.nombre] = nuevasObservaciones
                                        },
                                        onSpeechToTextClick = { 
                                            handleSpeechToTextClick(plato.nombre) 
                                        }
                                    )
                                }
                            }
                        } else {
                            // Mensaje de categoría vacía
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_dishes_available),
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Botón de avanzar etapa - optimizado para claridad
                        EtapaActionButton(
                            etapaComanda = etapaComanda,
                            onClick = { avanzarEtapaComanda() }
                        )
                    }
                } else {
                    // Pantalla de comanda finalizada - simplificada
                    ComandaFinalizadaScreen(
                        onVolverClick = { navController.popBackStack() }
                    )
                }
            }
            
            // Dialogs optimizados - solo se renderizan cuando son necesarios
            if (mostrarDialogoSeleccion) {
                AlertDialog(
                    onDismissRequest = { mostrarDialogoSeleccion = false },
                    title = { Text(stringResource(R.string.existing_order_title)) },
                    text = { Text(stringResource(R.string.existing_order_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                cargarOrdenExistente = true
                                mostrarDialogoSeleccion = false
                            }
                        ) {
                            Text(stringResource(R.string.continue_order))
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                cargarOrdenExistente = false
                                platosSeleccionados.clear()
                                sentItems.clear()
                                etapaComanda = ComandaStage.PRIMERS_BEGUDES
                                mostrarDialogoSeleccion = false
                            }
                        ) {
                            Text(stringResource(R.string.new_option))
                        }
                    }
                )
            }

            // Diálogo de confirmación - renderizado condicional
            if (mostrarDialogoConfirmacion) {
                ConfirmationDialog(
                    title = stringResource(R.string.confirm_send_title),
                    message = stringResource(R.string.confirm_send_message),
                    onConfirm = {
                        mostrarDialogoConfirmacion = false
                        scope.launch {
                            enviarComanda()
                        }
                    },
                    onDismiss = { mostrarDialogoConfirmacion = false }
                )
            }

            // Diálogo de imagen ampliada - optimizado
            if (mostrarImagenAmpliada && platoSeleccionado != null) {
                Dialog(
                    onDismissRequest = { mostrarImagenAmpliada = false },
                    properties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Box(modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(platoSeleccionado?.imagenUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = platoSeleccionado?.nombre,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth(0.8f)
                                .aspectRatio(1f)
                        )

                        // Botón cerrar
                        IconButton(
                            onClick = { mostrarImagenAmpliada = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close_image),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Mensaje de error - usando Snackbar en lugar de Dialog
            if (mensajeError != null) {
                LaunchedEffect(mensajeError) {
                    delay(3000)
                    mensajeError = null
                }
                
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    containerColor = Color(0xFFD32F2F)
                ) {
                    Text(text = mensajeError ?: "", color = Color.White)
                }
            }

            // Mensaje de éxito: entrada expresiva (fade + escala 0.9→1 con spring), salida sutil (fade)
            if (mostrarMensajeExito) {
                LaunchedEffect(Unit) {
                    delay(1500)
                    mostrarMensajeExito = false
                }
            }
            val reduceMotion = rememberWaiterReduceMotion()
            val motionScheme = MaterialTheme.motionScheme
            AnimatedVisibility(
                visible = mostrarMensajeExito,
                modifier = Modifier.fillMaxSize(),
                enter = fadeIn(motionScheme.fastEffectsSpec()),
                exit = fadeOut(motionScheme.fastEffectsSpec())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .padding(24.dp)
                            .animateEnterExit(
                                enter = if (reduceMotion) EnterTransition.None
                                else scaleIn(motionScheme.defaultSpatialSpec(), initialScale = 0.9f),
                                exit = ExitTransition.None
                            ),
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = TableStatusColors.Free,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = stringResource(R.string.order_sent_success),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }

            // Indicador de carga - simplificado
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }
    }
}

// Función para actualizar la mesa cuando se finaliza la comanda
private fun actualizarMesaFinalizada(
    mesaId: Int,
    context: Context,
    firebaseTableManager: FirebaseTableManager,
    scope: CoroutineScope
) {
    // Guardar estado de comanda finalizada en preferencias
    val sharedPrefs = context.getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
    sharedPrefs.edit().putString("table_$mesaId", "FINALIZADA").apply()
    
    // Actualizar estado de la mesa
    scope.launch {
        firebaseTableManager.getTableById(mesaId) { tableData ->
            if (tableData != null) {
                val isReserved = tableData["estado"] as? String == "RESERVADA" || 
                               tableData["status"] as? String == "RESERVADA"
                
                if (isReserved) {
                    firebaseTableManager.updateTableStatus(
                        tableId = mesaId,
                        estado = context.getString(R.string.state_reserved_internal),
                        clientes = 0
                    ) { _ -> }
                } else {
                    firebaseTableManager.updateTableStatus(
                        tableId = mesaId,
                        estado = context.getString(R.string.state_free_internal),
                        clientes = 0
                    ) { _ -> }
                }
            }
        }
    }
}

// Componentes extraídos para mejor rendimiento
@Composable
fun EtapaActionButton(etapaComanda: ComandaStage, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp),
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(
            containerColor = when(etapaComanda) {
                ComandaStage.POSTRES -> Color(0xFF4CAF50)
                else -> Color(0xFFFFA000)
            }
        )
    ) {
        Text(
            text = when(etapaComanda) {
                ComandaStage.PRIMERS_BEGUDES -> stringResource(R.string.continue_to_main)
                ComandaStage.SEGONS -> stringResource(R.string.continue_to_dessert)
                ComandaStage.POSTRES -> stringResource(R.string.finish_order)
                else -> stringResource(R.string.next)
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ComandaFinalizadaScreen(onVolverClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.order_completed),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onVolverClick,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text(
                    text = stringResource(R.string.back_to_tables),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}