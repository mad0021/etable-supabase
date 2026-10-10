package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuItem
import cat.dam.mamadou.etable.ui.screens.admin.models.MenuCategories
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import cat.dam.mamadou.etable.R

// Función para mostrar nombres de categorías en español

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptimizedMenuItemDialog(
    item: MenuItem?,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,  // Nuevo parámetro para tomar foto
    isUploading: Boolean,
    uploadError: String?
) {
    if (item == null) return
    
    var name by remember { mutableStateOf(item.name) }
    var category by remember { mutableStateOf(item.category) }
    var isAvailable by remember { mutableStateOf(item.isAvailable) }
    var requiresCookingPoint by remember { mutableStateOf(item.requiresCookingPoint) }
    var expanded by remember { mutableStateOf(false) }
    
    // Usar AlertDialog para tablets - más grande y adaptado a pantallas táctiles
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (item.id.isEmpty()) stringResource(R.string.menu_item_add) else stringResource(R.string.menu_item_edit),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            ) 
        },
        modifier = Modifier
            .width(IntrinsicSize.Min)
            .widthIn(max = 600.dp),
        shape = MaterialTheme.shapes.extraLarge,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Campo para el nombre del plato
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.menu_item_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )
                
                // Selector de categoría
                Column {
                    // Etiqueta de categoría
                    Text(
                        text = stringResource(R.string.menu_item_category),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    FilledTonalButton(
                        onClick = { expanded = true },
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                    ) {
                        Text(getCategoryDisplayName(category))
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        MenuCategories.values().forEach { menuCategory ->
                            DropdownMenuItem(
                                text = { Text(getCategoryDisplayName(menuCategory)) },
                                onClick = {
                                    category = menuCategory
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Switch para disponibilidad
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Textos para disponibilidad
                    Text(
                        text = stringResource(R.string.menu_item_available),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    AdminSwitch(
                        checked = isAvailable,
                        onCheckedChange = { isAvailable = it }
                    )
                }
                
                // Nuevo switch para punto de cocción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Textos para punto de cocción
                        Text(
                            text = stringResource(R.string.menu_item_cooking_point),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(R.string.menu_item_cooking_point_info),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AdminSwitch(
                        checked = requiresCookingPoint,
                        onCheckedChange = { requiresCookingPoint = it }
                    )
                }
                
                // Selector de imagen - Modificado para mostrar dos botones
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Etiqueta de imagen
                    Text(
                        text = stringResource(R.string.menu_item_image),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    // Vista previa de imagen o placeholder
                    if (item.imageUri != null || item.imageUrl?.isNotEmpty() == true) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = item.imageUri ?: item.imageUrl
                            ),
                            contentDescription = stringResource(R.string.image_preview),
                            modifier = Modifier
                                .size(200.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .clickable(onClick = onPickImage),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = stringResource(R.string.image_select),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botones para seleccionar/tomar imagen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botón para seleccionar de galería
                        OutlinedButton(
                            onClick = onPickImage,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null
                                )
                                // Texto del botón de galería
                                Text(stringResource(R.string.menu_item_gallery))
                            }
                        }
                        
                        // Botón para tomar foto con cámara
                        OutlinedButton(
                            onClick = onTakePhoto,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Camera,
                                    contentDescription = null
                                )
                                // Texto del botón de cámara
                                Text(stringResource(R.string.menu_item_camera))
                            }
                        }
                    }
                }
                
                // Mostrar errores y progreso
                if (uploadError != null) {
                    Text(
                        text = uploadError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (isUploading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            item.copy(
                                name = name,
                                category = category,
                                isAvailable = isAvailable,
                                requiresCookingPoint = requiresCookingPoint
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && !isUploading,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                // Texto del botón de guardar
                Text(
                    stringResource(R.string.menu_item_save),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text(
                    // Texto del botón de cancelar
                    stringResource(R.string.menu_item_cancel),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    )
}