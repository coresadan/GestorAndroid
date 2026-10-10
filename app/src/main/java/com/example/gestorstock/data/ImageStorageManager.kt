package com.example.gestorstock.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ImageStorageManager(private val context: Context) {

    private val articulosDir: File
        get() {
            val dir = File(context.filesDir, "articulos")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * Copia una imagen desde una Uri (content:// o file://) al almacenamiento interno permanente de la app.
     * Retorna la Uri persistente en formato "file:///data/user/0/.../files/articulos/art_...jpg"
     */
    fun guardarImagenArticulo(sourceUri: Uri): String? {
        return try {
            val sourceUriStr = sourceUri.toString()
            // Si ya es un archivo local en nuestro directorio interno, no necesitamos duplicarlo
            if (sourceUriStr.startsWith("file://") && sourceUriStr.contains("/articulos/")) {
                return sourceUriStr
            }

            val fileName = "art_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(articulosDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Rota una imagen en almacenamiento interno en 90 grados y guarda la nueva versión.
     */
    fun rotarImagenArticulo(uriString: String, grados: Float = 90f): String? {
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (bitmap == null) return null

            val matrix = Matrix().apply { postRotate(grados) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            val fileName = "art_${System.currentTimeMillis()}_rot.jpg"
            val destFile = File(articulosDir, fileName)
            FileOutputStream(destFile).use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            eliminarImagenArticulo(uriString)
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Elimina el archivo de imagen local si existe dentro del directorio de la app.
     */
    fun eliminarImagenArticulo(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                uri.path?.let { path ->
                    val file = File(path)
                    if (file.exists() && file.parentFile?.name == "articulos") {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
