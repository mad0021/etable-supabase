package cat.dam.mamadou.etable.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cat.dam.mamadou.etable.R

/**
 * Clase utilitaria para gestionar permisos en la aplicación
 */
object PermissionsManager {
    private const val TAG = "PermissionsManager"
    
    // Tipos de permisos comunes en la aplicación
    enum class PermissionType {
        NOTIFICATION,
        CAMERA,
        MICROPHONE,
        STORAGE
    }
    
    /**
     * Verifica si un permiso ha sido concedido
     */
    fun isPermissionGranted(context: Context, permissionType: PermissionType): Boolean {
        return when(permissionType) {
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true // En versiones anteriores a Android 13, no se necesita permiso explícito
                }
            }
            PermissionType.CAMERA -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
            PermissionType.MICROPHONE -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
            PermissionType.STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    true // A partir de Android 10, no se necesita permiso para almacenamiento interno de la app
                } else {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
    }
    
    /**
     * Obtiene el permiso específico de Android según el tipo
     */
    fun getPermissionString(permissionType: PermissionType): String {
        return when(permissionType) {
            PermissionType.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else {
                    ""
                }
            }
            PermissionType.CAMERA -> Manifest.permission.CAMERA
            PermissionType.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            PermissionType.STORAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ""
                } else {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                }
            }
        }
    }
    
    /**
     * Comprueba si es necesario mostrar una explicación sobre el permiso
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permissionType: PermissionType): Boolean {
        val permissionString = getPermissionString(permissionType)
        return if (permissionString.isNotEmpty()) {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionString)
        } else false
    }
    
    /**
     * Abre la configuración de la aplicación para que el usuario pueda habilitar permisos
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        context.startActivity(intent)
    }
    
    /**
     * Solicita el permiso usando el launcher proporcionado
     */
    fun requestPermission(
        permissionType: PermissionType,
        launcher: ActivityResultLauncher<String>
    ) {
        val permissionString = getPermissionString(permissionType)
        if (permissionString.isNotEmpty()) {
            Log.d(TAG, "Solicitando permiso: $permissionString")
            launcher.launch(permissionString)
        }
    }
    
    /**
     * Obtiene mensaje explicativo sobre por qué se necesita el permiso
     */
    fun getRationaleMessage(context: Context, permissionType: PermissionType): String {
        return when(permissionType) {
            PermissionType.NOTIFICATION -> context.getString(R.string.notification_permission_rationale)
            PermissionType.CAMERA -> context.getString(R.string.camera_permission_rationale)
            PermissionType.MICROPHONE -> context.getString(R.string.microphone_permission_rationale)
            PermissionType.STORAGE -> context.getString(R.string.storage_permission_rationale)
        }
    }
}