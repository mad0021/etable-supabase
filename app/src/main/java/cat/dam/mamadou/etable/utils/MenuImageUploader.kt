package cat.dam.mamadou.etable.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import cat.dam.mamadou.etable.supabase.supabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * Sube las fotos de la carta al bucket público `menu-images` de Supabase Storage.
 * Sustituye a ImgBB: ya no hay ninguna clave en el código y solo un admin puede subir (política RLS del bucket).
 */
class MenuImageUploader(private val context: Context) {

    suspend fun uploadImage(imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (imageUri == Uri.EMPTY) return@withContext Result.failure(Exception("URI vacía"))
            val bytes = compress(imageUri)
            val path = "${UUID.randomUUID()}.jpg"
            val bucket = supabaseClient.storage.from(BUCKET)
            bucket.upload(path, bytes) { contentType = ContentType.Image.JPEG }
            Result.success(bucket.publicUrl(path))
        } catch (e: Exception) {
            Log.e(TAG, "Error subiendo imagen", e)
            Result.failure(Exception("Error al subir la imagen: ${e.localizedMessage ?: e}"))
        }
    }

    /** Reduce la foto de la cámara (varios MB) a un JPEG de ~1280 px antes de subirla. */
    private fun compress(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: error("No se puede abrir la imagen: $uri")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Formato de imagen no soportado")
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }

    private companion object {
        const val TAG = "MenuImageUploader"
        const val BUCKET = "menu-images"
        const val MAX_SIDE = 1280
    }
}
