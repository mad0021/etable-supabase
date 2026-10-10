package cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminLoading
import cat.dam.mamadou.etable.ui.screens.admin.components.adminAnimateItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.OrderStages
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.ui.components.BreakRequestNotification
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.ReadyOrdersViewModel
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.useBreakNotificationListenerNoModal

@Composable
fun ReadyOrdersScreen(
    viewModel: ReadyOrdersViewModel
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val itemCookingPoints by viewModel.itemCookingPoints.collectAsState()
    val itemObservations by viewModel.itemObservations.collectAsState()
    val menuItems by viewModel.menuItems.collectAsState()
    
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
                    userName = BreakNotificationManager.getCurrentBreakRequest()?.userName ?: "Usuario",
                    fullName = BreakNotificationManager.getCurrentBreakRequest()?.fullName ?: "",
                    onDismiss = { BreakNotificationManager.dismissNotification() }
                )
            }

            if (isLoading) {
                AdminLoading()
            } else if (orders.isEmpty()) {
                EmptyStateMessage(message = stringResource(R.string.admin_orders_empty_ready))
            } else {
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
                        ReadyOrderCard( // CAMBIAR de OptimizedOrderCard
                            modifier = adminAnimateItem(),
                            order = order,
                            itemCookingPoints = itemCookingPoints,
                            itemObservations = itemObservations,
                            onMarkAsCompleted = { 
                                viewModel.markOrderAsCompleted(order.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReadyOrderCard(
    order: Order,
    itemCookingPoints: Map<String, CookingPoint?>,
    itemObservations: Map<String, String?>,
    onMarkAsCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // AGREGAR: Obtener etapa traducida usando OrderStages
    val currentStageTranslated = remember(order.currentStage) {
        order.currentStage?.let { stage ->
            OrderStages.getLocalizedText(context, stage)
        } ?: context.getString(R.string.admin_orders_stage_unspecified)
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(MaterialTheme.motionScheme.defaultEffectsSpec()),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header de la orden - ACTUALIZADO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.admin_orders_table_name, order.tableId.toString()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // AGREGAR: Mostrar etapa traducida
                    OrderStageChip(
                        text = stringResource(R.string.admin_orders_stage, currentStageTranslated),
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                
                Button(
                    onClick = onMarkAsCompleted,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp)
                ) {
                    Text(stringResource(R.string.admin_orders_deliver_button))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Items de la orden
            order.items.forEach { item ->
                // LOG para debugging
                Log.d("ReadyOrdersScreen", "Item: id=${item.id}, name='${item.name}', quantity=${item.quantity}")
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}× ${item.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Mostrar punto de cocción si existe
                    itemCookingPoints["${order.id}_${item.id}"]?.let { cookingPoint ->
                        Text(
                            text = cookingPoint.getLocalizedString(context),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // Mostrar observaciones si existen
                itemObservations["${order.id}_${item.id}"]?.let { observation ->
                    if (observation.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.admin_orders_observation_prefix, observation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}