package cat.dam.mamadou.etable.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cat.dam.mamadou.etable.R
import cat.dam.mamadou.etable.utils.BreakNotificationManager

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BreakRequestNotification(
    visible: Boolean,
    userName: String = BreakNotificationManager.getCurrentBreakRequest()?.userName ?: "Usuario",
    fullName: String = BreakNotificationManager.getCurrentBreakRequest()?.fullName ?: "",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleState = remember { MutableTransitionState(false) }
    visibleState.targetState = visible

    ExpressiveNotificationVisibility(visibleState = visibleState, modifier = modifier) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.break_notification_description),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.break_notification_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(
                        R.string.break_notification_from, 
                        if (fullName.isNotEmpty()) fullName else userName
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Button(
                onClick = { onDismiss() },
                shapes = ButtonDefaults.shapes()
            ) {
                Text(stringResource(R.string.break_request_accept))
            }
        }
    }
    }
}
