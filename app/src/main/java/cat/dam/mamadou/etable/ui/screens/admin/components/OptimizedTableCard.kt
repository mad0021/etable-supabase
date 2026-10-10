package cat.dam.mamadou.etable.ui.screens.admin.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.constants.TableStates
import cat.dam.mamadou.etable.ui.screens.waiter.models.Mesa

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OptimizedTableCard(
    table: Mesa,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Normalizar el estado antes de usarlo
    val normalizedState = TableStates.fromLegacyValue(table.estado)

    // Cambio de estado de la mesa (dato): transición de color con el spec de efectos del tema
    val stateColor by animateColorAsState(
        targetValue = TableStates.getStateColor(normalizedState),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
    )

    // Obtener texto localizado para mostrar
    val stateText = TableStates.getLocalizedText(context, normalizedState)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Padding adecuado para tablets
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Número de mesa dentro de una forma "cookie" expresiva
            Surface(
                shape = MaterialShapes.Cookie9Sided.toShape(),
                color = stateColor.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.size(80.dp), // Tamaño grande para tablets
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${table.id}",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp, // Más grande para tablets
                            textAlign = TextAlign.Center
                        ),
                        color = stateColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estado de la mesa con chip coloreado
            Surface(
                shape = CircleShape,
                color = stateColor.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, stateColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TableBar,
                        contentDescription = null,
                        tint = stateColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stateText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = stateColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Información de clientes
            if (normalizedState == TableStates.OCCUPIED || normalizedState == TableStates.RESERVED) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.table_management_customers_count, table.clientes),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                // Botón Editar
                FilledTonalIconButton(
                    onClick = onClick,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.admin_edit)
                    )
                }

                // Botón Eliminar
                FilledTonalIconButton(
                    onClick = onDelete,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.admin_delete)
                    )
                }
            }
        }
    }
}
