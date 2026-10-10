@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components.orders

import cat.dam.mamadou.etable.ui.components.formatPrice
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.waiter.models.Plato
import cat.dam.mamadou.etable.models.CookingPoint
import cat.dam.mamadou.etable.utils.CookingPointHelper
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun OrderItem(
    plato: Plato,
    cantidad: Int,
    onCantidadChanged: (Int) -> Unit,
    isSent: Boolean = false,
    modifier: Modifier = Modifier,
    onImageClick: () -> Unit = {},
    cookingPoint: CookingPoint? = null,
    onCookingPointChanged: (CookingPoint?) -> Unit = {},
    observations: String = "",
    onObservationsChanged: (String) -> Unit = {},
    onSpeechToTextClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = if (isSent) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // PRIMERA FILA: Imagen, info y controles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen del plato - CORREGIR el TTS
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Color(0xFFF5F5F5))
                        .clickable { 
                            onImageClick()
                            
                            // AGREGAR la llamada TTS aquí:
                            // Como no tenemos acceso directo al TTSManager, lo manejaremos en el componente padre
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(plato.imagenUrl ?: plato.imagen)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .size(width = 140, height = 140)
                            .build(),
                        contentDescription = stringResource(
                            R.string.order_item_image_description, 
                            plato.nombre
                        ),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (isSent) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .background(Color(0xFF4CAF50), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(R.string.order_item_sent),
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Información del plato
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = plato.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (plato.precio > 0) {
                        Text(
                            text = formatPrice(plato.precio),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }
                }
                
                // Controles de cantidad - SIN TTS
                if (!isSent) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón de restar - SIN lógica TTS
                        if (cantidad > 0) {
                            FilledIconButton(
                                onClick = {
                                    val nuevaCantidad = cantidad - 1
                                    onCantidadChanged(nuevaCantidad)
                                    // NO TTS aquí
                                },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFFEF5350),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.order_item_decrease),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        
                        // Número en el medio
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cantidad.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Botón de sumar - SIN lógica TTS
                        FilledIconButton(
                            onClick = {
                                val nuevaCantidad = cantidad + 1
                                onCantidadChanged(nuevaCantidad)
                                // NO TTS aquí
                            },
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.order_item_increase),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            
            // SEGUNDA FILA: Área expandida COMPACTA (SOLO SI ES NECESARIA)
            if (plato.requiresCookingPoint && cantidad > 0 && !isSent) {
                Divider(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
                
                // Área compacta para punto de cocción y observaciones
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    // Selector de punto de cocción COMPACTO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.order_item_cooking_point_label),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(70.dp),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Dropdown COMPACTO
                        var expanded by remember { mutableStateOf(false) }
                        
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    // CAMBIAR ESTA LÍNEA:
                                    text = cookingPoint?.getLocalizedString(context) ?: stringResource(R.string.order_item_select_cooking_point),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.order_item_dropdown_options),
                                    modifier = Modifier
                                        .padding(start = 4.dp)
                                        .size(14.dp)
                                )
                            }
                            
                            // Dropdown menu
                            val cookingPointOptions = remember(context) { 
                                CookingPointHelper.getAllLocalizedCookingPoints(context) 
                            }
                            
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                cookingPointOptions.forEach { localizedText ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                localizedText,
                                                style = MaterialTheme.typography.bodySmall
                                            ) 
                                        },
                                        onClick = {
                                            val point = CookingPointHelper.fromLocalizedString(context, localizedText)
                                            onCookingPointChanged(point)
                                            expanded = false
                                        }
                                    )
                                }
                                
                                Divider()
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            stringResource(R.string.order_item_no_cooking_point),
                                            style = MaterialTheme.typography.bodySmall
                                        ) 
                                    },
                                    onClick = {
                                        onCookingPointChanged(null)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Campo para observaciones COMPACTO
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.order_item_observations),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(70.dp),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        OutlinedTextField(
                            value = observations,
                            onValueChange = onObservationsChanged,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            placeholder = { 
                                Text(
                                    stringResource(R.string.order_item_observations_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Botón de micrófono MÁS PEQUEÑO
                        IconButton(
                            onClick = onSpeechToTextClick
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = stringResource(R.string.order_item_voice_input),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}