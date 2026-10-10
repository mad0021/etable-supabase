package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.textButtonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cat.dam.mamadou.etable.R
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptimizedTableDialog(
    table: Mesa?,
    onDismiss: () -> Unit,
    onSave: (Mesa) -> Unit
) {
    val context = LocalContext.current
    
    if (table == null) return
    
    var nombre by remember { mutableStateOf(table.nombre ?: "") }
    var estado by remember { mutableStateOf(TableStates.fromLegacyValue(table.estado)) }
    var clientes by remember { mutableStateOf(table.clientes.toString()) }
    
    // Validaciones
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = if (table.id == 0) stringResource(R.string.table_management_add_table) 
                       else stringResource(R.string.table_management_edit_table, table.id),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            ) 
        },
        modifier = Modifier
            .widthIn(max = 560.dp)
            .padding(16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Mostrar error si existe
                if (hasError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Campo de nombre
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.table_management_table_name)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp), // Más alto para tablets
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )
                
                // Selector de estado
                Text(
                    text = stringResource(R.string.table_management_table_state),
                    style = MaterialTheme.typography.titleMedium
                )
                AdminConnectedToggleGroup(
                    labels = TableStates.ALL.map { TableStates.getLocalizedText(context, it) },
                    selectedIndex = TableStates.ALL.indexOf(estado),
                    onSelect = { estado = TableStates.ALL[it] }
                )
                
                // Campo de número de clientes
                OutlinedTextField(
                    value = clientes,
                    onValueChange = { 
                        // Solo permitir números
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            clientes = it
                        } 
                    },
                    label = { Text(stringResource(R.string.table_management_customers)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp), // Más alto para tablets
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val clientesInt = if (clientes.isEmpty()) 0 else clientes.toInt()
                        
                        if (clientesInt < 0) {
                            hasError = true
                            errorMessage = context.getString(R.string.table_management_validation_error)
                            return@Button
                        }
                        
                        val updatedTable = table.copy(
                            nombre = nombre,
                            estado = estado, // Ya está normalizado
                            clientes = clientesInt
                        )
                        
                        onSave(updatedTable)
                    } catch (e: Exception) {
                        hasError = true
                        errorMessage = context.getString(R.string.table_management_save_error, e.message)
                    }
                },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text(stringResource(R.string.table_management_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 56.dp),
                shapes = ButtonDefaults.shapes(),
                colors = textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.table_management_cancel))
            }
        }
    )
}