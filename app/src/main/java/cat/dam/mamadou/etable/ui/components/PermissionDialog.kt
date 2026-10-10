package cat.dam.mamadou.etable.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cat.dam.mamadou.etable.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PermissionDialog(
    permissionTextProvider: String,
    isPermanentlyDeclined: Boolean,
    onDismiss: () -> Unit,
    onOkClick: () -> Unit,
    onGoToAppSettingsClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(text = stringResource(id = R.string.permission_required))
        },
        text = {
            Text(text = permissionTextProvider)
        },
        confirmButton = {
            Button(
                shapes = ButtonDefaults.shapes(),
                onClick = {
                    if (isPermanentlyDeclined) {
                        onGoToAppSettingsClick()
                    } else {
                        onOkClick()
                    }
                }
            ) {
                Text(
                    text = if (isPermanentlyDeclined) {
                        stringResource(id = R.string.open_settings)
                    } else {
                        stringResource(id = R.string.grant_permission)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                shapes = ButtonDefaults.shapes(),
                onClick = onDismiss
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}