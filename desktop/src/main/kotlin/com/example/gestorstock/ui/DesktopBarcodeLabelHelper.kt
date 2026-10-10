package com.example.gestorstock.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO

object DesktopBarcodeLabelHelper {

    // Patrones estándar Code 39 (9 barras por caracter: 1 = barra ancha, 0 = barra estrecha)
    private val CODE39_PATTERNS = mapOf(
        '0' to "000110100", '1' to "100100001", '2' to "001100001", '3' to "101100000",
        '4' to "000110001", '5' to "100110000", '6' to "001110000", '7' to "000100101",
        '8' to "100100100", '9' to "001100100", 'A' to "100001001", 'B' to "001001001",
        'C' to "101001000", 'D' to "000011001", 'E' to "100011000", 'F' to "001011000",
        'G' to "000001101", 'H' to "100001100", 'I' to "001001100", 'J' to "000011100",
        'K' to "100000011", 'L' to "001000011", 'M' to "101000010", 'N' to "000010011",
        'O' to "100010010", 'P' to "001010010", 'Q' to "000000111", 'R' to "100000110",
        'S' to "001000110", 'T' to "000010110", 'U' to "110000001", 'V' to "011000001",
        'W' to "111000000", 'X' to "010010001", 'Y' to "11000000", 'Z' to "011010000",
        '-' to "010000101", '.' to "110000100", ' ' to "011000100", '$' to "010101000",
        '/' to "010100010", '+' to "010001010", '%' to "000101010", '*' to "010010100"
    )

    fun generarBarrasBufferedImage(codigo: String, anchoDeseado: Int = 460, altoDeseado: Int = 90): BufferedImage {
        val textoLimpio = ("*" + codigo.uppercase().filter { it in CODE39_PATTERNS.keys } + "*").ifEmpty { "*ART*" }
        val img = BufferedImage(anchoDeseado, altoDeseado, BufferedImage.TYPE_INT_RGB)
        val g2d = img.createGraphics()
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, anchoDeseado, altoDeseado)

        g2d.color = Color.BLACK
        val unidadesTotales = textoLimpio.length * (3 * 2.5f + 6 * 1f + 1.2f)
        val unidadPixel = (anchoDeseado * 0.90f) / unidadesTotales
        val estrecho = unidadPixel
        val ancho = unidadPixel * 2.5f

        var cursorX = (anchoDeseado - (unidadesTotales * unidadPixel)) / 2f

        for (char in textoLimpio) {
            val patron = CODE39_PATTERNS[char] ?: CODE39_PATTERNS['-']!!
            for (i in patron.indices) {
                val esBarra = (i % 2 == 0)
                val esAncho = patron[i] == '1'
                val w = if (esAncho) ancho else estrecho

                if (esBarra) {
                    g2d.fillRect(cursorX.toInt(), 0, Math.max(1, w.toInt()), altoDeseado)
                }
                cursorX += w
            }
            cursorX += estrecho * 1.2f
        }
        g2d.dispose()
        return img
    }

    fun generarEtiquetaCompletaBufferedImage(
        nombreTienda: String,
        nombreArticulo: String,
        categoria: String?,
        codigo: String,
        precio: Double,
        unidadMedida: String
    ): BufferedImage {
        val width = 560
        val height = 360
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g2d = img.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        // Fondo blanco
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        // Borde exterior
        g2d.color = Color(200, 200, 215)
        g2d.drawRoundRect(10, 10, width - 20, height - 20, 16, 16)

        // Tienda
        g2d.color = Color(120, 40, 160)
        g2d.font = Font("Segoe UI", Font.BOLD, 20)
        val tiendaTexto = nombreTienda.ifBlank { "MI TIENDITA" }.uppercase()
        g2d.drawString(tiendaTexto, 24, 44)

        // Categoría si existe
        if (!categoria.isNullOrBlank()) {
            g2d.color = Color(100, 100, 110)
            g2d.font = Font("Segoe UI", Font.PLAIN, 15)
            val catText = categoria.uppercase()
            val catWidth = g2d.fontMetrics.stringWidth(catText)
            g2d.drawString(catText, width - 24 - catWidth, 44)
        }

        // Línea divisoria
        g2d.color = Color(225, 225, 235)
        g2d.drawLine(24, 56, width - 24, 56)

        // Nombre del artículo
        g2d.color = Color.BLACK
        g2d.font = Font("Segoe UI", Font.BOLD, 24)
        val nombreCorto = if (nombreArticulo.length > 28) nombreArticulo.take(26) + "..." else nombreArticulo
        g2d.drawString(nombreCorto, 24, 94)

        // Precio grande
        g2d.color = Color(216, 27, 96) // Rosa vibrante
        g2d.font = Font("Segoe UI", Font.BOLD, 38)
        val precioStr = "$${String.format(Locale.US, "%.2f", precio)} MXN"
        g2d.drawString(precioStr, 24, 142)

        // Unidad
        g2d.color = Color(90, 90, 100)
        g2d.font = Font("Segoe UI", Font.BOLD, 16)
        val precioWidth = g2d.fontMetrics.stringWidth(precioStr)
        g2d.drawString("/ ${unidadMedida.uppercase()}", 24 + precioWidth + 14, 138)

        // Barras
        val barcode = generarBarrasBufferedImage(codigo, anchoDeseado = width - 48, altoDeseado = 95)
        g2d.drawImage(barcode, 24, 170, null)

        // Código centrado
        g2d.color = Color(40, 40, 40)
        g2d.font = Font("Consolas", Font.BOLD, 22)
        val codeWidth = g2d.fontMetrics.stringWidth(codigo)
        g2d.drawString(codigo, (width - codeWidth) / 2, 305)

        // Pie de página
        g2d.color = Color(160, 160, 170)
        g2d.font = Font("Segoe UI", Font.PLAIN, 12)
        val footer = "PRODUCTO ORIGINAL • CONTROL DE STOCK"
        val footerWidth = g2d.fontMetrics.stringWidth(footer)
        g2d.drawString(footer, (width - footerWidth) / 2, 336)

        g2d.dispose()
        return img
    }

    fun generarEtiquetaComposeBitmap(
        nombreTienda: String,
        nombreArticulo: String,
        categoria: String?,
        codigo: String,
        precio: Double,
        unidadMedida: String
    ): ImageBitmap {
        val bImg = generarEtiquetaCompletaBufferedImage(
            nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida
        )
        return bImg.toComposeImageBitmap()
    }

    fun guardarEtiqueta(
        archivoDestino: File,
        nombreTienda: String,
        nombreArticulo: String,
        categoria: String?,
        codigo: String,
        precio: Double,
        unidadMedida: String
    ): Boolean {
        return try {
            val img = generarEtiquetaCompletaBufferedImage(
                nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida
            )
            ImageIO.write(img, "png", archivoDestino)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun imprimirEtiqueta(
        nombreTienda: String,
        nombreArticulo: String,
        categoria: String?,
        codigo: String,
        precio: Double,
        unidadMedida: String
    ): Boolean {
        return try {
            val img = generarEtiquetaCompletaBufferedImage(
                nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida
            )
            val job = PrinterJob.getPrinterJob()
            job.setJobName("Etiqueta_$nombreArticulo")
            job.setPrintable(Printable { graphics, pageFormat, pageIndex ->
                if (pageIndex > 0) return@Printable Printable.NO_SUCH_PAGE
                val g2d = graphics as Graphics2D
                g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
                val scale = Math.min(
                    pageFormat.imageableWidth / img.width.toDouble(),
                    pageFormat.imageableHeight / img.height.toDouble()
                )
                g2d.scale(scale, scale)
                g2d.drawImage(img, 0, 0, null)
                Printable.PAGE_EXISTS
            })

            if (job.printDialog()) {
                job.print()
                true
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
