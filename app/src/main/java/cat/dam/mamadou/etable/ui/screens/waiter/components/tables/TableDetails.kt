@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components.tables

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.models.OrderStatus
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import cat.dam.mamadou.etable.utils.TTSManager
import cat.dam.mamadou.etable.utils.firebase.FirebaseOrderManager
import cat.dam.mamadou.etable.supabase.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import cat.dam.mamadou.etable.supabase.await
import kotlinx.coroutines.withContext
import cat.dam.mamadou.etable.ui.theme.TableStatusColors

@Composable
fun DetallesMesa(
    mesa: Mesa,
    onClientesChanged: (Int) -> Unit,
    onNuevaComanda: (String?) -> Unit
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }
    
    var hayOrdenesPendientes by remember { mutableStateOf(false) }
    var etapaActual by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val stagePrimersBegudes = stringResource(R.string.stage_primers_begudes)
    val stageSegons = stringResource(R.string.stage_segons)
    val stagePostres = stringResource(R.string.stage_postres)
    val stageFinalizada = stringResource(R.string.stage_finalizada)

    val textoNuevaOrden = stringResource(R.string.table_new_order)
    val textoContinuarOrden = stringResource(R.string.table_continue_order)
    val textoActualizar = stringResource(R.string.table_update)

    // Añadir estas dos líneas para pre-cargar los textos de accesibilidad
    val decreaseClientDescription = stringResource(R.string.table_decrease_clients)
    val increaseClientDescription = stringResource(R.string.table_increase_clients)

    // Obtener los valores internos de los estados desde constantes
    val reservedState = TableStates.RESERVED
    val freeState = TableStates.FREE
    val occupiedState = TableStates.OCCUPIED

    // Obtener los textos traducidos para mostrar
    val reservedStateDisplay = stringResource(R.string.state_reserved)
    val freeStateDisplay = stringResource(R.string.state_free)
    val occupiedStateDisplay = stringResource(R.string.state_occupied)

    // Función para obtener el texto traducido del estado
    val getStateDisplay = { estadoInterno: String ->
        when (estadoInterno) {
            reservedState -> reservedStateDisplay
            freeState -> freeStateDisplay
            occupiedState -> occupiedStateDisplay
            else -> estadoInterno // Fallback para valores desconocidos
        }
    }

    LaunchedEffect(mesa.id) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                val firestore = FirebaseFirestore.getInstance()

                val snapshot = firestore.collection("orders")
                    .whereEqualTo("tableId", mesa.id)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()

                val ordenesExisten = !snapshot.isEmpty
                etapaActual = snapshot.documents.firstOrNull()?.getString("currentStage")

                val sharedPrefs = firestore.app.applicationContext
                    .getSharedPreferences("ActiveOrders", Context.MODE_PRIVATE)
                val savedStage = sharedPrefs.getString("table_${mesa.id}", null)

                // LÓGICA CORREGIDA: 
                // 1. Si hay órdenes pendientes en Firebase → hay comanda activa
                // 2. Si NO hay órdenes pero SÍ hay savedStage (excepto FINALIZADA) → hay comanda activa
                // 3. Solo si no hay órdenes Y savedStage es FINALIZADA o null → no hay comanda
                
                hayOrdenesPendientes = when {
                    ordenesExisten -> {
                        Log.d("TableDetails", "✅ Mesa ${mesa.id}: órdenes pendientes en Firebase")
                        true
                    }
                    savedStage != null && savedStage != "FINALIZADA" -> {
                        Log.d("TableDetails", "✅ Mesa ${mesa.id}: etapa guardada activa: $savedStage")
                        true
                    }
                    else -> {
                        Log.d("TableDetails", "❌ Mesa ${mesa.id}: sin órdenes activas")
                        false
                    }
                }

                // Si no hay etapa actual de Firebase pero sí savedStage, usar savedStage
                if (etapaActual == null && savedStage != null && savedStage != "FINALIZADA") {
                    etapaActual = savedStage
                    Log.d("TableDetails", "🔄 Mesa ${mesa.id}: usando savedStage como etapaActual: $savedStage")
                }

                Log.d("TableDetails", "📊 Mesa ${mesa.id}: órdenes pendientes: $hayOrdenesPendientes, etapa: $etapaActual, savedStage: $savedStage")
            }
        } catch (e: Exception) {
            Log.e("TableDetails", "Error verificando órdenes: ${e.message}", e)
        } finally {
            isLoading = false
        }
    }

    val comandaButtonColor by remember(hayOrdenesPendientes, etapaActual) {
        derivedStateOf {
            when {
                !hayOrdenesPendientes -> TableStatusColors.Free
                etapaActual == stagePostres -> TableStatusColors.Free
                else -> TableStatusColors.Reserved
            }
        }
    }

    val comandaButtonText by remember(hayOrdenesPendientes, etapaActual) {
        derivedStateOf {
            when {
                hayOrdenesPendientes -> {
                    Log.d("TableDetails", "🔄 Mesa ${mesa.id}: Mostrando CONTINUAR (hay órdenes pendientes)")
                    textoContinuarOrden
                }
                etapaActual == "FINALIZADA" -> {
                    Log.d("TableDetails", "🆕 Mesa ${mesa.id}: Mostrando NUEVA (etapa finalizada)")
                    textoNuevaOrden
                }
                else -> {
                    Log.d("TableDetails", "🆕 Mesa ${mesa.id}: Mostrando NUEVA (sin órdenes)")
                    textoNuevaOrden
                }
            }
        }
    }

    // Cambio de estado del botón (nueva/continuar): color con spec de efectos
    val animatedComandaColor by animateColorAsState(
        targetValue = comandaButtonColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "comandaButtonColor"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = if (!mesa.nombre.isNullOrEmpty()) mesa.nombre
                else stringResource(R.string.table_prefix, mesa.id),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    when (mesa.estado) {
                                        reservedState -> TableStatusColors.Reserved
                                        freeState -> TableStatusColors.Free
                                        occupiedState -> TableStatusColors.Occupied
                                        else -> TableStatusColors.Other
                                    },
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = getStateDisplay(mesa.estado),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.table_clients, mesa.clientes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }

            // CONTROLES DE CLIENTES CON TTS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        if (mesa.clientes > 0) {
                            val newCount = mesa.clientes - 1
                            onClientesChanged(newCount)
                            
                            // TTS para cambio de clientes
                            if (ttsManager.isEnabled()) {
                                val tableNumber = if (mesa.id > 0) mesa.id.toString() else mesa.nombre.ifEmpty { "Mesa" }
                                ttsManager.speakTableClientsChange(
                                    tableNumber = tableNumber,
                                    newCount = newCount,
                                    isIncrease = false
                                )
                            }
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription = decreaseClientDescription
                        },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TableStatusColors.Occupied,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                }

                Text(
                    text = "${mesa.clientes}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledIconButton(
                    onClick = {
                        val newCount = mesa.clientes + 1
                        onClientesChanged(newCount)
                        
                        // TTS para cambio de clientes
                        if (ttsManager.isEnabled()) {
                            val tableNumber = if (mesa.id > 0) mesa.id.toString() else mesa.nombre.ifEmpty { "Mesa" }
                            ttsManager.speakTableClientsChange(
                                tableNumber = tableNumber,
                                newCount = newCount,
                                isIncrease = true
                            )
                        }
                    },
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier
                        .size(56.dp)
                        .semantics {
                            contentDescription = increaseClientDescription
                        },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = TableStatusColors.Free,
                        contentColor = Color.White
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                }
            }

            if (mesa.comandas.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.table_orders),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val comandasToShow = mesa.comandas.take(5)
                comandasToShow.forEach { comanda ->
                    Text(
                        text = "• ${comanda.cantidad}× ${comanda.nombre}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }

                if (mesa.comandas.size > 5) {
                    Text(
                        text = stringResource(R.string.table_more_orders, mesa.comandas.size - 5),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // BOTONES CON TTS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // BOTÓN DE COMANDA CON TTS
                Button(
                    onClick = {
                        // TTS para acción del botón
                        if (ttsManager.isEnabled()) {
                            val buttonType = if (comandaButtonText == textoNuevaOrden) "new_order" else "continue_order"
                            ttsManager.speakButtonAction(buttonType)
                        }
                        
                        val etapaSiguiente = when {
                            comandaButtonText == textoNuevaOrden -> {
                                Log.d("TableDetails", "🆕 Mesa ${mesa.id}: Iniciando nueva comanda → PRIMERS_BEGUDES")
                                "PRIMERS_BEGUDES"
                            }
                            else -> {
                                // CONTINUANDO comanda existente - avanzar a la siguiente etapa
                                val nextStage = when (etapaActual) {
                                    "PRIMERS_BEGUDES" -> "SEGONS"
                                    "SEGONS" -> "POSTRES" 
                                    "POSTRES" -> "FINALIZADA"
                                    null -> {
                                        // Si no hay etapa actual, empezar desde el principio
                                        Log.d("TableDetails", "⚠️ Mesa ${mesa.id}: etapaActual es null, empezando desde PRIMERS_BEGUDES")
                                        "PRIMERS_BEGUDES"
                                    }
                                    else -> {
                                        Log.d("TableDetails", "⚠️ Mesa ${mesa.id}: etapa desconocida '$etapaActual', empezando desde PRIMERS_BEGUDES")
                                        "PRIMERS_BEGUDES"
                                    }
                                }
                                Log.d("TableDetails", "🔄 Mesa ${mesa.id}: Continuando comanda: $etapaActual → $nextStage")
                                nextStage
                            }
                        }

                        Log.d("TableDetails", "🚀 Mesa ${mesa.id}: Navegando con etapa: $etapaSiguiente (Botón: $comandaButtonText)")
                        onNuevaComanda(etapaSiguiente)
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .weight(2f)
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = animatedComandaColor
                    )
                ) {
                    Text(
                        text = comandaButtonText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}