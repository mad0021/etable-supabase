package cat.dam.mamadou.etable.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R

@Composable
fun TextSizeAdjuster(
    modifier: Modifier = Modifier,
    onTextSizeChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var currentSize by remember { mutableStateOf(16f) }
    
    // Cargar el tamaño guardado en preferencias
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("text_preferences", android.content.Context.MODE_PRIVATE)
        currentSize = prefs.getFloat("text_size", 16f)
    }
    
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.text_size_sample),
                fontSize = currentSize.sp,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón disminuir
                IconButton(
                    onClick = {
                        if (currentSize > 12f) {
                            currentSize -= 1f
                            onTextSizeChanged(currentSize)
                            
                            // Guardar en preferencias
                            val prefs = context.getSharedPreferences("text_preferences", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putFloat("text_size", currentSize).apply()
                        }
                    },
                    enabled = currentSize > 12f
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.text_size_decrease)
                    )
                }
                
                // Mostrar tamaño actual
                Text(
                    text = "${currentSize.toInt()}sp",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Botón aumentar
                IconButton(
                    onClick = {
                        if (currentSize < 24f) {
                            currentSize += 1f
                            onTextSizeChanged(currentSize)
                            
                            // Guardar en preferencias
                            val prefs = context.getSharedPreferences("text_preferences", android.content.Context.MODE_PRIVATE)
                            prefs.edit().putFloat("text_size", currentSize).apply()
                        }
                    },
                    enabled = currentSize < 24f
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.text_size_increase)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Slider para control más preciso
            Slider(
                value = currentSize,
                onValueChange = { newSize ->
                    currentSize = newSize
                    onTextSizeChanged(newSize)
                    
                    // Guardar en preferencias
                    val prefs = context.getSharedPreferences("text_preferences", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putFloat("text_size", newSize).apply()
                },
                valueRange = 12f..24f,
                steps = 11,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}