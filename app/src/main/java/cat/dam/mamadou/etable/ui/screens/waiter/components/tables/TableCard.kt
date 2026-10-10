@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components.tables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import cat.dam.mamadou.etable.ui.screens.waiter.components.waiterPressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import cat.dam.mamadou.etable.ui.theme.TableStatusColors
import kotlin.math.min
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import cat.dam.mamadou.etable.utils.TTSManager

@Composable
fun TarjetaMesa(
    mesa: Mesa,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val ttsManager = remember { TTSManager.getInstance(context) }

    // Obtener los valores internos de los estados desde constantes, no desde recursos
    val reservedState = TableStates.RESERVED
    val freeState = TableStates.FREE
    val occupiedState = TableStates.OCCUPIED

    // Obtener los textos traducidos para mostrar
    val reservedStateDisplay = stringResource(R.string.state_reserved)
    val freeStateDisplay = stringResource(R.string.state_free)
    val occupiedStateDisplay = stringResource(R.string.state_occupied)

    // Colores de fondo según estado (contraste AA con texto blanco)
    val colorFondo by remember(mesa.clientes, mesa.estado, isSelected) {
        derivedStateOf {
            when {
                isSelected -> TableStatusColors.Selected
                mesa.estado == reservedState -> TableStatusColors.Reserved
                mesa.clientes > 0 -> TableStatusColors.Occupied
                mesa.estado == freeState -> TableStatusColors.Free
                else -> TableStatusColors.Other
            }
        }
    }

    // Cambio de estado: color animado con spec de efectos (sin rebote)
    val animatedFondo by animateColorAsState(
        targetValue = colorFondo,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "tableStatusColor"
    )
    // Selección: transición corta de forma (radio de esquina)
    val cornerRadius by animateDpAsState(
        targetValue = if (isSelected) 40.dp else 28.dp,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "tableCorner"
    )
    val interactionSource = remember { MutableInteractionSource() }

    // Optimizar número de puntos a mostrar
    val dotsToShow by remember(mesa.clientes) {
        derivedStateOf { minOf(mesa.clientes, 8) }
    }

    // Preparar el texto de la mesa para TTS antes de entrar al clickable
    val tableText = if (mesa.id > 0) stringResource(id = R.string.table_prefix, mesa.id) else mesa.nombre

    Card(
        onClick = {
            onClick()

            // TTS al seleccionar mesa
            if (ttsManager.isEnabled()) {
                val tableNumber = if (mesa.id > 0) mesa.id.toString() else mesa.nombre.ifEmpty { "Mesa" }
                ttsManager.speakTableStatus(
                    tableNumber = tableNumber,
                    status = mesa.estado,
                    clientCount = mesa.clientes
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .aspectRatio(1f)
            .heightIn(min = 180.dp)
            .waiterPressScale(interactionSource),
        shape = RoundedCornerShape(cornerRadius),
        interactionSource = interactionSource,
        colors = CardDefaults.cardColors(
            containerColor = animatedFondo,
            contentColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        border = if (isSelected)
            BorderStroke(4.dp, MaterialTheme.colorScheme.onSurface)
        else
            BorderStroke(2.dp, Color.Black.copy(alpha = 0.2f))
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Tamaños proporcionales al ancho de la tarjeta
            val numberSize = min(82f, maxWidth.value * 0.4f).sp
            val labelSize = min(24f, maxWidth.value * 0.12f).sp
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Nombre/número de mesa
                Text(
                    text = if (!mesa.nombre.isNullOrEmpty()) mesa.nombre else mesa.id.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = numberSize,
                    lineHeight = numberSize,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            
                // Mostrar clientes solo si hay
                if (mesa.clientes > 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        // Texto de clientes - USANDO RECURSOS
                        Text(
                            text = stringResource(R.string.table_clients_count, mesa.clientes),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = labelSize
                        )

                        // Iconos de clientes
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            // Mostrar iconos de clientes de forma optimizada
                            if (dotsToShow <= 4) {
                                // Para 4 o menos clientes - una sola fila
                                repeat(dotsToShow) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(horizontal = 4.dp)
                                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                                    )
                                }
                            } else {
                                // Para más clientes - mostrar algunos puntos y un contador
                                repeat(4) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(horizontal = 4.dp)
                                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                                    )
                                }
                                // Texto del contador - USANDO RECURSOS
                                Text(
                                    text = stringResource(R.string.table_clients_more, mesa.clientes - 4),
                                            fontSize = labelSize * 0.9f,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                } else if (mesa.estado == reservedState) {
                    // Indicador de mesa reservada - USANDO RECURSOS
                    Text(
                        text = reservedStateDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = labelSize,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                } else if (mesa.estado == freeState) {
                    // Indicador de mesa libre - USANDO RECURSOS
                    Text(
                        text = freeStateDisplay,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = labelSize,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}
