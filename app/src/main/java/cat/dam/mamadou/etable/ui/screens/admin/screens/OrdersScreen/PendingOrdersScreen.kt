package cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen

import android.util.Log // AÑADIR ESTE IMPORT
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.OrderStages // AGREGAR ESTE IMPORT
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.PendingOrdersViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.OrderViewModelFactory
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager
import cat.dam.mamadou.etable.utils.useBreakNotificationListenerNoModal
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PendingOrdersScreen(
    viewModel: PendingOrdersViewModel
) {
    val orders by viewModel.orders.collectAsState() // Cambiar de pendingOrders a orders
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val itemCookingPoints by viewModel.itemCookingPoints.collectAsState()
    val itemObservations by viewModel.itemObservations.collectAsState()
    
    val firebaseManager = remember { FirebaseManager() }
    val showBreakNotification = useBreakNotificationListenerNoModal(firebaseManager)
    
    message?.let { msg ->
        LaunchedEffect(msg) {
            delay(100)
            viewModel.clearMessage()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoading) {
            LoadingIndicator()
        } else if (orders.isEmpty()) {
            EmptyStateMessage(message = stringResource(R.string.admin_orders_empty_pending))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = orders,
                    key = { order -> order.id }
                ) { order ->
                    PendingOrderItem(
                        modifier = adminAnimateItem(),
                        order = order,
                        itemCookingPoints = itemCookingPoints,
                        itemObservations = itemObservations,
                        onAccept = { viewModel.acceptOrder(order.id) },
                        onReject = { viewModel.rejectOrder(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(title: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun LoadingIndicator() {
    AdminLoading()
}

/** Chip de etapa del pedido (forma completamente redondeada). */
@Composable
fun OrderStageChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PendingOrderItem(
    order: Order,
    itemCookingPoints: Map<String, CookingPoint?>,
    itemObservations: Map<String, String?>,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Obtener el contexto FUERA del bloque remember
    val context = LocalContext.current
    
    // Usar el sistema de formato de fecha de acuerdo al locale actual
    val formattedTime = remember(order.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(order.timestamp)
    }
    
    val groupedItems = remember(order.items, itemCookingPoints, context) { // Añadir context como dependencia
        order.items.groupBy { item -> 
            val cookingPoint = itemCookingPoints["${order.id}_${item.id}"]
            "${item.name}${cookingPoint?.let { "_${it.display}" } ?: ""}"
        }.map { (key, items) -> 
            val name = key.substringBefore("_")
            // Ya NO obtenemos el contexto aquí dentro
            val cookingPointText = key.substringAfter("_", "").takeIf { it.isNotEmpty() }
            val displayText = if (cookingPointText != null) {
                // Usar el contexto que ya capturamos fuera
                val point = CookingPoint.values().find { it.display == cookingPointText }
                point?.getLocalizedString(context) ?: cookingPointText
            } else {
                null
            }
            val totalQuantity = items.sumOf { it.quantity }
            val firstItem = items.first()
            val observation = itemObservations["${order.id}_${firstItem.id}"]
            GroupedOrderItem(name, displayText, totalQuantity, observation)
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
                Text(
                    text = order.tableName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Usar formato de string para etapas
            val currentStageTranslated = remember(order.currentStage) {
                order.currentStage?.let { stage ->
                    OrderStages.getLocalizedText(context, stage)
                } ?: context.getString(R.string.admin_orders_stage_unspecified)
            }

            OrderStageChip(
                text = stringResource(R.string.admin_orders_stage, currentStageTranslated),
                modifier = Modifier.padding(vertical = 6.dp)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                groupedItems.forEach { item ->
                    // AÑADIR ESTE LOG ANTES DE MOSTRAR CADA ITEM
                    Log.d("PendingOrdersScreen", "Item: name='${item.name}', quantity=${item.quantity}")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // CAMBIAR ESTO:
                        Text(
                            text = "${item.quantity}× ${item.name}", // USAR EL MISMO FORMATO QUE ReadyOrdersScreen
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        
                        if (item.cookingPoint != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = CircleShape,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
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
                    
                    if (!item.observation.isNullOrEmpty()) {
                        Text(
                            // Usar formato de string para observaciones
                            text = stringResource(R.string.admin_orders_obs_prefix, item.observation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onAccept,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.heightIn(min = 56.dp)
                ) {
                    // Usar recurso de string para botón
                    Text(stringResource(R.string.admin_orders_accept_button))
                }
            }
        }
    }
}