package com.example.gestorstock.data

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.util.UUID
import javax.imageio.ImageIO

class DesktopImageManager {

    private val articulosDir: File

    init {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val appDir = File(appData, "GestorStock").apply { if (!exists()) mkdirs() }
        articulosDir = File(appDir, "articulos").apply { if (!exists()) mkdirs() }
    }

    fun guardarImagenArticulo(origen: File): String? {
        return try {
            val fileName = "art_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
            val destFile = File(articulosDir, fileName)
            origen.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun rotarImagenArticulo(path: String, grados: Double = 90.0): String? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            val original = ImageIO.read(file) ?: return null
            val rads = Math.toRadians(grados)
            val sin = Math.abs(Math.sin(rads))
            val cos = Math.abs(Math.cos(rads))
            val w = original.width
            val h = original.height
            val newWidth = Math.floor(w * cos + h * sin).toInt()
            val newHeight = Math.floor(h * cos + w * sin).toInt()

            val rotated = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
            val g2d = rotated.createGraphics()
            val at = AffineTransform()
            at.translate((newWidth - w) / 2.0, (newHeight - h) / 2.0)
            at.rotate(rads, w / 2.0, h / 2.0)
            g2d.drawRenderedImage(original, at)
            g2d.dispose()

            val newFileName = "art_${System.currentTimeMillis()}_rot.jpg"
            val newDest = File(articulosDir, newFileName)
            ImageIO.write(rotated, "jpg", newDest)

            eliminarImagenArticulo(path)
            newDest.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun eliminarImagenArticulo(path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val file = File(path)
            if (file.exists() && file.parentFile?.absolutePath == articulosDir.absolutePath) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
