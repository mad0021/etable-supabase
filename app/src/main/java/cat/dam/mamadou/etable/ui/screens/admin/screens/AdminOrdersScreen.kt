package cat.dam.mamadou.etable.ui.screens.admin.screens

import cat.dam.mamadou.etable.ui.components.formatPrice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.ui.semantics.Role
import cat.dam.mamadou.etable.ui.screens.admin.components.AdminConnectedToggleGroup
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.models.Order
import cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen.HistoryOrdersScreen
import cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen.InProgressOrdersScreen
import cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen.PendingOrdersScreen
import cat.dam.mamadou.etable.ui.screens.admin.screens.OrdersScreen.ReadyOrdersScreen
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.HistoryOrdersViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.InProgressOrdersViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.OrderViewModelFactory
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.PendingOrdersViewModel
import cat.dam.mamadou.etable.ui.screens.admin.viewmodels.ReadyOrdersViewModel
import cat.dam.mamadou.etable.utils.BreakNotificationManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Estructura para las pestañas con iconos para mejorar la intuitividad
private data class TabItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit // QUITAR el parámetro onNavigateBack
)

// Efecto para escuchar notificaciones de descanso pero solo con sonido
@Composable
private fun UseBreakSoundOnlyListener(firebaseManager: FirebaseManager) {
    DisposableEffect(Unit) {
        // Asegurarse de que las notificaciones visuales están desactivadas
        BreakNotificationManager.disableModalNotifications()
        
        // El listener para solicitudes de descanso ya está configurado en AdminDashboardActivity
        // y se encarga del sonido y la vibración
        
        onDispose { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    mesaId: Int? = null, 
    etapaInicial: String? = null,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModelFactory = remember { OrderViewModelFactory(context) }
    val firebaseManager = remember { FirebaseManager() }
    
    // Crear ViewModels usando factory
    val pendingViewModel: PendingOrdersViewModel = viewModel(factory = viewModelFactory)
    val inProgressViewModel: InProgressOrdersViewModel = viewModel(factory = viewModelFactory)
    val readyViewModel: ReadyOrdersViewModel = viewModel(factory = viewModelFactory)
    val historyViewModel: HistoryOrdersViewModel = viewModel(factory = viewModelFactory)

    // Configurar listener solo para sonido de notificaciones de descanso
    UseBreakSoundOnlyListener(firebaseManager)

    Column(modifier = Modifier.fillMaxSize()) {
        val pagerState = rememberPagerState(pageCount = { 4 })
        val scope = rememberCoroutineScope()

        // Textos localizados para las pestañas
        val pendingTitle = stringResource(R.string.admin_orders_tab_pending) // CAMBIAR de admin_orders_pending
        val inProgressTitle = stringResource(R.string.admin_orders_tab_in_progress) // CAMBIAR de admin_orders_in_progress
        val readyTitle = stringResource(R.string.admin_orders_tab_ready) // CAMBIAR de admin_orders_ready
        val historyTitle = stringResource(R.string.admin_orders_tab_history) // CAMBIAR de admin_orders_history

        val tabs = listOf(
            TabItem(
                title = pendingTitle,
                icon = Icons.Default.Pending,
                screen = { PendingOrdersScreen(pendingViewModel) } // SIN parámetros
            ),
            TabItem(
                title = inProgressTitle,
                icon = Icons.Default.Restaurant,
                screen = { InProgressOrdersScreen(inProgressViewModel) } // SIN parámetros
            ),
            TabItem(
                title = readyTitle,
                icon = Icons.Default.Done,
                screen = { ReadyOrdersScreen(readyViewModel) } // SIN parámetros
            ),
            TabItem(
                title = historyTitle,
                icon = Icons.Default.History,
                screen = { HistoryOrdersScreen(historyViewModel) } // SIN parámetros
            )
        )

        // Pestañas de estado: grupo de ToggleButton conectados (cambio instantáneo, sin animar el pager)
        AdminConnectedToggleGroup(
            labels = tabs.map { it.title },
            icons = tabs.map { it.icon },
            selectedIndex = pagerState.currentPage,
            onSelect = { index -> scope.launch { pagerState.scrollToPage(index) } },
            role = Role.Tab,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
        ) { page ->
            tabs[page].screen() // SIN parámetros
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    // Memoizar el formato de fecha para evitar recreaciones
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    // Calcular y memorizar fecha formateada
    val formattedDate = remember(order.timestamp) { 
        dateFormat.format(order.timestamp) 
    }
    
    // Pre-calcular total para evitar cálculos repetidos
    val totalAmount = remember(order.items) {
        order.items.sumOf { it.price * it.quantity }
    }
    
    // Agrupar items iguales para reducir elementos renderizados
    val groupedItems = remember(order.items) {
        order.items.groupBy { it.name }
            .map { (name, items) ->
                val quantity = items.sumOf { it.quantity }
                val price = items.first().price
                Triple(name, quantity, price * quantity)
            }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.admin_orders_table_name, order.tableId.toString()), // CORREGIR conversión a Any
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Contenedor para fecha más destacado
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mostrar los items agrupados
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedItems.forEach { (name, quantity, price) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$quantity× $name",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                        )
                        Text(
                            text = formatPrice(price),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                        )
                    }
                }
            }
            
            // Línea separadora y total
            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.admin_orders_total),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                Text(
                    text = formatPrice(totalAmount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Botones adaptados para tablets - más grandes y accesibles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 2.dp,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.admin_orders_accept_description) // CAMBIAR de admin_orders_reject_description
                    )
                }
                
                Text(
                    text = stringResource(R.string.admin_orders_accept) // CAMBIAR de admin_orders_reject
                )

                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.admin_orders_accept_description) // CAMBIAR de admin_orders_reject_description
                    )
                }
                
                Text(
                    text = stringResource(R.string.admin_orders_accept) // CAMBIAR de admin_orders_reject
                )
            }
        }
    }
}
