package cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen

import android.content.Context
import android.util.Log // AÑADIR ESTE IMPORT
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminLoading
import cat.dam.mamadou.etable.ui.screens.admin.components.adminAnimateItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.ui.components.BreakRequestNotification
import cat.dam.mamadou.etable.ui.components.CookingPointDisplay
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.CombinedOrder
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.HistoryOrdersViewModel
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.useBreakNotificationListenerNoModal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.res.stringResource
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.components.CookingPointChip
import cat.dam.mamadou.etable.constants.OrderStages

// Clase de datos para agrupar items de pedido
data class GroupedOrderItem(
    val name: String,
    val cookingPoint: String?,
    val quantity: Int,
    val observation: String?
)

@Composable
fun HistoryOrdersScreen(
    viewModel: HistoryOrdersViewModel
) {
    val combinedOrders by viewModel.combinedOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val itemCookingPoints by viewModel.itemCookingPoints.collectAsState()
    val itemObservations by viewModel.itemObservations.collectAsState()
    
    val firebaseManager = remember { FirebaseManager() }
    val showBreakNotification = useBreakNotificationListenerNoModal(firebaseManager)
    val lazyListState = rememberLazyListState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Mostrar notificación cuando sea necesario
            if (showBreakNotification) {
                BreakRequestNotification(
                    visible = true,
                    onDismiss = { BreakNotificationManager.dismissNotification() },
                    userName = BreakNotificationManager.getCurrentBreakRequest()?.userName ?: ""
                )
            }

            if (isLoading) {
                AdminLoading()
            } else if (combinedOrders.isEmpty()) {
                EmptyStateMessage(message = stringResource(R.string.admin_history_empty))
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = combinedOrders,
                        key = { it.id } 
                    ) { combinedOrder ->
                        CombinedOrderCard(
                            modifier = adminAnimateItem(),
                            combinedOrder = combinedOrder,
                            itemCookingPoints = itemCookingPoints,
                            itemObservations = itemObservations
                        )
                    }
                }
            }
        }
    }
}

// Reemplazar la función getInternalStageKey con una versión mejorada
private fun getStageKey(displayText: String, context: Context): String {
    return when (displayText) {
        context.getString(R.string.stage_starters_drinks) -> OrderStages.STARTERS_DRINKS
        context.getString(R.string.stage_main_courses) -> OrderStages.MAIN_COURSES
        context.getString(R.string.stage_desserts) -> OrderStages.DESSERTS
        else -> ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedOrderCard(
    combinedOrder: CombinedOrder,
    itemCookingPoints: Map<String, CookingPoint?>,
    itemObservations: Map<String, String?>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Obtener textos localizados para las etapas
    val primerStageText = stringResource(R.string.stage_starters_drinks)
    val segonsStageText = stringResource(R.string.stage_main_courses)
    val postresStageText = stringResource(R.string.stage_desserts)
    val otrosStageText = stringResource(R.string.admin_history_stage_other)
    
    // Orden de visualización de etapas
    val stageOrder = remember { listOf(primerStageText, segonsStageText, postresStageText, otrosStageText) }

    // Agrupar pedidos por tipo de etapa usando la función mejorada
    val ordersByStageType = remember(combinedOrder, stageOrder, context) {
        combinedOrder.orders.groupBy { order ->
            val stageTypes = stageOrder.filter { stageText -> 
                val stageKey = getStageKey(stageText, context)
                
                // Verificar si algún item de la orden coincide con esta etapa
                order.items.any { item -> 
                    OrderStages.matchesStage(item.etapa, stageKey)
                }
            }
            stageTypes.firstOrNull() ?: otrosStageText
        }
    }
    
    // Formato de moneda según configuración regional actual
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val dateFormat = remember { 
        android.text.format.DateFormat.getDateFormat(context)
    }
    val timeFormat = remember { 
        android.text.format.DateFormat.getTimeFormat(context)
    }
    
    val formattedDate = remember(combinedOrder.timestamp) { 
        dateFormat.format(Date(combinedOrder.timestamp))
    }
    val formattedTime = remember(combinedOrder.timestamp) { 
        timeFormat.format(Date(combinedOrder.timestamp))
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Encabezado con información destacada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    // Usar recurso de string con formato
                    text = stringResource(R.string.admin_history_table_name, combinedOrder.tableName),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                )

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Chip de comanda completa
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .align(alignment = Alignment.Start)
            ) {
                Text(
                    // Usar recurso de string
                    text = stringResource(R.string.admin_history_complete_order),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Mostrar platos por cada etapa en orden
            stageOrder.forEach { stageText ->
                val ordersForStage = ordersByStageType[stageText] ?: emptyList()
                
                if (ordersForStage.isNotEmpty()) {
                    // Cabecera de etapa con estilo más distintivo
                    EtapaHeader(stageText)
                    
                    // Para cada orden de esta etapa, mostrar sus platos
                    ordersForStage.forEach { order ->
                        // Aquí necesitamos aplicar el mismo enfoque para filtrar los items
                        val stageKey = getStageKey(stageText, context)
                        
                        // Filtrar items que coincidan con esta etapa
                        val filteredItems = order.items.filter { item ->
                            OrderStages.matchesStage(item.etapa, stageKey)
                        }
                        
                        // Agrupar items por nombre y punto de cocción
                        val groupedItems = filteredItems.groupBy { item -> 
                            val cookingPoint = itemCookingPoints["${order.id}_${item.id}"]
                            "${item.name}${cookingPoint?.let { "_${it.display}" } ?: ""}"
                        }.map { (key, sameItems) -> 
                            val name = key.substringBefore("_")
                            // PROBLEMA: Se guarda directamente el texto del punto de cocción sin localizar
                            val cookingPoint = key.substringAfter("_", "").takeIf { it.isNotEmpty() }
                            val displayText = if (cookingPoint != null) {
                                // Usar directamente el punto de cocción sin traducir
                                cookingPoint
                            } else {
                                null
                            }

                            // SOLUCIÓN: Usar CookingPointHelper para localizar la visualización
                            val context = LocalContext.current
                            val cookingPointText = key.substringAfter("_", "").takeIf { it.isNotEmpty() }
                            val localizedDisplayText = if (cookingPointText != null) {
                                try {
                                    // Intentar convertir a enum y luego obtener el texto localizado
                                    val point = CookingPoint.values().find { 
                                        it.display.equals(cookingPointText, ignoreCase = true)
                                    }
                                    point?.getLocalizedString(context) ?: cookingPointText
                                } catch (e: Exception) {
                                    cookingPointText
                                }
                            } else {
                                null
                            }

                            val totalQuantity = sameItems.sumOf { it.quantity }
                            val firstItem = sameItems.first()
                            val observation = itemObservations["${order.id}_${firstItem.id}"]
                            GroupedOrderItem(name, localizedDisplayText, totalQuantity, observation)
                        }
                        
                        // Mostrar items agrupados
                        groupedItems.forEach { item ->
                            // AÑADIR ESTE LOG ANTES DE MOSTRAR CADA ITEM
                            Log.d("HistoryOrdersScreen", "Item: name='${item.name}', quantity=${item.quantity}")
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    // CAMBIAR ESTA LÍNEA:
                                    text = "${item.quantity}× ${item.name}", // USAR EL MISMO FORMATO QUE ReadyOrdersScreen
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (item.cookingPoint != null) {
                                    CookingPointChip(cookingPointText = item.cookingPoint)
                                }
                            }
                            
                            // Mostrar observaciones si existen
                            if (!item.observation.isNullOrEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .padding(start = 36.dp, top = 4.dp, end = 16.dp, bottom = 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = stringResource(R.string.admin_orders_info_icon_description),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = item.observation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Separador entre etapas
                    if (stageText != stageOrder.last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EtapaHeader(etapa: String) {
    val context = LocalContext.current
    
    // Identificar la etapa
    val stageKey = getStageKey(etapa, context)
    
    // Obtener colores basados en la etapa interna
    val (color, textColor) = when (stageKey) {
        OrderStages.STARTERS_DRINKS -> 
            MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        OrderStages.MAIN_COURSES -> 
            MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary
        OrderStages.DESSERTS -> 
            MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary
        else -> 
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    
    Surface(
        color = color,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        // Mostrar directamente el texto traducido
        Text(
            text = etapa,  // Ya está traducido cuando se pasa a esta función
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = textColor
        )
    }
}
