@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package cat.dam.mamadou.etable.ui.screens.waiter.components.orders

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.ui.screens.waiter.models.ComandaStage
import cat.dam.mamadou.etable.ui.theme.TableStatusColors

// Objeto singleton para definir las etapas una sola vez
@Composable
private fun getStages(): List<StageData> {
    // MODIFICAR: Usar la nueva string para PRIMERS_BEGUDES
    val primersBegudesName = stringResource(R.string.category_primers_begudes_combined)
    val segonsName = stringResource(R.string.stage_segons_name)
    val postresName = stringResource(R.string.stage_postres_name)
    
    return listOf(
        StageData(
            name = primersBegudesName, // CAMBIO: Usar nueva traducción
            stage = ComandaStage.PRIMERS_BEGUDES,
            icon = Icons.Default.LocalBar,
            activeColor = TableStatusColors.Selected,
            inactiveColor = Color(0xFFE8EAF6),
            completedColor = TableStatusColors.Free
        ),
        StageData(
            name = segonsName,
            stage = ComandaStage.SEGONS,
            icon = Icons.Default.LocalDining,
            activeColor = TableStatusColors.Free,
            inactiveColor = Color(0xFFE8F5E9),
            completedColor = TableStatusColors.Free
        ),
        StageData(
            name = postresName,
            stage = ComandaStage.POSTRES,
            icon = Icons.Default.Cake,
            activeColor = TableStatusColors.Occupied,
            inactiveColor = Color(0xFFFFEBEE),
            completedColor = TableStatusColors.Free
        )
    )
}

// Clase de datos inmutable para las etapas
private data class StageData(
    val name: String,
    val stage: ComandaStage,
    val icon: ImageVector,
    val activeColor: Color,
    val inactiveColor: Color,
    val completedColor: Color
)

// Versión de la función para contextos no-composables
private object StageInfoFallback {
    val stages = listOf(
        StageData(
            name = "PRIMERS I BEGUDES",  // Valor por defecto en catalán
            stage = ComandaStage.PRIMERS_BEGUDES,
            icon = Icons.Default.LocalBar,
            activeColor = TableStatusColors.Selected,
            inactiveColor = Color(0xFFE8EAF6),
            completedColor = TableStatusColors.Free
        ),
        StageData(
            name = "SEGONS",  // Valor por defecto en catalán
            stage = ComandaStage.SEGONS,
            icon = Icons.Default.LocalDining,
            activeColor = TableStatusColors.Free,
            inactiveColor = Color(0xFFE8F5E9),
            completedColor = TableStatusColors.Free
        ),
        StageData(
            name = "POSTRES",  // Valor por defecto en catalán
            stage = ComandaStage.POSTRES,
            icon = Icons.Default.Cake,
            activeColor = TableStatusColors.Occupied,
            inactiveColor = Color(0xFFFFEBEE),
            completedColor = TableStatusColors.Free
        )
    )
}

@Composable
fun StageSelector(
    currentStage: ComandaStage,
    onStageSelected: (ComandaStage) -> Unit
) {
    // Cargar las etapas traducidas
    val stages = getStages()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Text(
            // Usar recurso de string para el título
            text = stringResource(R.string.stage_selector_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            // Usar las etapas traducidas
            stages.forEachIndexed { index, stageData ->
                StageButton(
                    stageData = stageData,
                    isSelected = currentStage == stageData.stage,
                    isCompleted = when {
                        currentStage == ComandaStage.SEGONS && stageData.stage == ComandaStage.PRIMERS_BEGUDES -> true
                        currentStage == ComandaStage.POSTRES &&
                            (stageData.stage == ComandaStage.PRIMERS_BEGUDES || stageData.stage == ComandaStage.SEGONS) -> true
                        currentStage == ComandaStage.FINALIZADA -> true
                        else -> false
                    },
                    onSelected = { onStageSelected(stageData.stage) },
                    shapes = connectedShapes(index, stages.size),
                    // La etapa activa se ensancha y muestra su nombre; las otras solo icono (no se truncan en móvil)
                    modifier = Modifier.weight(if (currentStage == stageData.stage) 2.4f else 1f)
                )
            }
        }
    }
}

// Componente extraído para reducir recomposiciones
@Composable
private fun StageButton(
    stageData: StageData,
    isSelected: Boolean,
    isCompleted: Boolean,
    onSelected: () -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier
) {
    // El resto del código permanece igual
    // Calcular colores una sola vez
    val backgroundColor = when {
        isSelected -> stageData.activeColor
        isCompleted -> stageData.completedColor
        else -> stageData.inactiveColor
    }
    
    val contentColor = if (isSelected || isCompleted) Color.White else Color.DarkGray

    // ToggleButton conectado: morph de forma al marcar/pulsar; colores de etapa intactos
    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onSelected() },
        modifier = modifier
            .heightIn(min = 64.dp)
            .semantics { role = Role.RadioButton },
        shapes = shapes,
        colors = ToggleButtonDefaults.toggleButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            checkedContainerColor = stageData.activeColor,
            checkedContentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = stageData.icon,
            contentDescription = if (isSelected) null else stageData.name,
            modifier = Modifier.size(22.dp)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stageData.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}