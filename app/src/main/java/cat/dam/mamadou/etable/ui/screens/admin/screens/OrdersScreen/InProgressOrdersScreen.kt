package cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen

import android.icu.text.SimpleDateFormat
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminLoading
import cat.dam.mamadou.etable.ui.screens.admin.components.adminAnimateItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.OrderStages
import cat.dam.mamadou.etable.ui.components.BreakRequestNotification
import cat.dam.mamadou.etable.ui.components.CookingPointDisplay
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.InProgressOrdersViewModel
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.useBreakNotificationListenerNoModal
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

@Composable
fun InProgressOrdersScreen(
    viewModel: InProgressOrdersViewModel
) {
    val orders by viewModel.orders.collectAsState() // Cambiar de inProgressOrders a orders
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val itemCookingPoints by viewModel.itemCookingPoints.collectAsState()
    val itemObservations by viewModel.itemObservations.collectAsState()
    
    val firebaseManager = remember { FirebaseManager() }
    val showBreakNotification = useBreakNotificationListenerNoModal(firebaseManager)
    
    LaunchedEffect(message) {
        message?.let {
            delay(100)
            viewModel.clearMessage()
        }
    }
    
    val lazyListState = rememberLazyListState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (showBreakNotification) {
                BreakRequestNotification(
                    visible = true,
                    onDismiss = { BreakNotificationManager.dismissNotification() }
                )
            }

            if (isLoading) {
                AdminLoading()
            } else if (orders.isEmpty()) {
                EmptyStateMessage(message = stringResource(R.string.admin_orders_empty_in_progress))
            } else {  // AÑADIR ESTA LLAVE QUE FALTABA
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(
                        items = orders,
                        key = { order -> order.id }
                    ) { order ->
                        OptimizedInProgressOrderItem(
                            modifier = adminAnimateItem(),
                            order = order,
                            itemCookingPoints = itemCookingPoints,
                            itemObservations = itemObservations,
                            onMarkAsReady = { viewModel.markOrderAsReady(order.id) }
                        )
                    }
                }
            } // AÑADIR ESTA LLAVE QUE FALTABA
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptimizedInProgressOrderItem(
    order: Order,
    itemCookingPoints: Map<String, CookingPoint?>,
    itemObservations: Map<String, String?>,
    onMarkAsReady: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val dateFormat = remember { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, Locale.getDefault()) }
    val formattedTime = remember(order.timestamp) {
        dateFormat.format(order.timestamp)
    }
    
    // AGREGAR: Obtener etapa traducida usando OrderStages
    val currentStageTranslated = remember(order.currentStage) {
        order.currentStage?.let { stage ->
            OrderStages.getLocalizedText(context, stage)
        } ?: context.getString(R.string.admin_orders_stage_unspecified)
    }
    
    // CORREGIDO: Usar puntos de cocción localizados
    val groupedItems = remember(order.items, itemCookingPoints, itemObservations) {
        order.items.groupBy { item -> 
            val cookingPoint = itemCookingPoints["${order.id}_${item.id}"]
            // CAMBIO: Usar getLocalizedString en lugar de display
            val localizedCookingPoint = cookingPoint?.getLocalizedString(context)
            val key = "${item.name}${localizedCookingPoint?.let { "_$it" } ?: ""}"
            key
        }.map { (key, items) -> 
            val name = key.substringBefore("_")
            val cookingPoint = key.substringAfter("_", "").takeIf { it.isNotEmpty() }
            val totalQuantity = items.sumOf { it.quantity }
            val firstItem = items.first()
            val observation = itemObservations["${order.id}_${firstItem.id}"]
            
            // Debug log para verificar observaciones
            if (observation != null) {
                Log.d("InProgressOrder", "Observación para ${name}: $observation")
            }
            
            GroupedOrderItem(name, cookingPoint, totalQuantity, observation)
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = order.tableName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    )
                    
                    // AGREGAR: Mostrar etapa traducida
                    OrderStageChip(
                        text = stringResource(R.string.admin_orders_stage, currentStageTranslated),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mostrar los items con punto de cocción localizado
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                groupedItems.forEach { item ->
                    // LOG para debugging
                    Log.d("InProgressOrdersScreen", "Item: name='${item.name}', quantity=${item.quantity}")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}× ${item.name}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (item.cookingPoint != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.admin_orders_cooking_point_prefix),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        // CORREGIDO: Ya está localizado desde groupedItems
                                        text = item.cookingPoint,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // Mostrar observaciones si existen
                    if (!item.observation.isNullOrEmpty()) {
                        Text(
                            text = stringResource(R.string.admin_orders_observation_prefix, item.observation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón más grande y accesible para tablets
            Button(
                onClick = onMarkAsReady,
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 56.dp),
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    stringResource(R.string.admin_orders_mark_as_ready),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
