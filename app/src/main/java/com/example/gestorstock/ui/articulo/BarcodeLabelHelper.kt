package com.example.gestorstock.ui.articulo

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object BarcodeLabelHelper {

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
        'W' to "111000000", 'X' to "010010001", 'Y' to "110010000", 'Z' to "011010000",
        '-' to "010000101", '.' to "110000100", ' ' to "011000100", '$' to "010101000",
        '/' to "010100010", '+' to "010001010", '%' to "000101010", '*' to "010010100"
    )

    /**
     * Dibuja las barras de un código de barras en formato Code 39
     */
    fun generarBarrasBitmap(codigo: String, anchoDeseado: Int = 460, altoDeseado: Int = 90): Bitmap {
        val textoLimpio = ("*" + codigo.uppercase().filter { it in CODE39_PATTERNS.keys } + "*")
            .ifEmpty { "*ART*" }

        val bitmap = Bitmap.createBitmap(anchoDeseado, altoDeseado, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val paintBarra = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        // Calcular ancho total de elementos
        // Cada caracter tiene 9 elementos (3 anchos, 6 estrechos) + 1 espacio inter-caracter estrecho
        val unidadesTotales = textoLimpio.length * (3 * 2.5f + 6 * 1f + 1.2f)
        val unidadPixel = (anchoDeseado * 0.90f) / unidadesTotales
        val estrecho = unidadPixel
        val ancho = unidadPixel * 2.5f

        var cursorX = (anchoDeseado - (unidadesTotales * unidadPixel)) / 2f

        for (char in textoLimpio) {
            val patron = CODE39_PATTERNS[char] ?: CODE39_PATTERNS['-']!!
            for (i in patron.indices) {
                val esBarra = (i % 2 == 0) // Índices pares son barras, impares son espacios
                val esAncho = patron[i] == '1'
                val w = if (esAncho) ancho else estrecho

                if (esBarra) {
                    canvas.drawRect(cursorX, 0f, cursorX + w, altoDeseado.toFloat(), paintBarra)
                }
                cursorX += w
            }
            cursorX += estrecho * 1.2f // Espacio inter-caracter
        }

        return bitmap
    }

    /**
     * Genera una etiqueta comercial completa (para anaquel o empaque)
     */
    fun generarEtiquetaCompleta(
        nombreTienda: String,
        nombreArticulo: String,
        categoria: String?,
        codigo: String,
        precio: Double,
        unidadMedida: String
    ): Bitmap {
        val width = 560
        val height = 360
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Fondo blanco
        canvas.drawColor(Color.WHITE)

        // Borde exterior
        val borderPaint = Paint().apply {
            color = Color.rgb(200, 200, 210)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(RectF(10f, 10f, width - 10f, height - 10f), 16f, 16f, borderPaint)

        // Cabecera: Nombre de tienda
        val headerPaint = Paint().apply {
            color = Color.rgb(120, 40, 160)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tiendaTexto = nombreTienda.ifBlank { "MI TIENDITA" }.uppercase()
        canvas.drawText(tiendaTexto, 24f, 44f, headerPaint)

        // Categoría si existe
        if (!categoria.isNullOrBlank()) {
            val catPaint = Paint().apply {
                color = Color.rgb(100, 100, 110)
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(categoria.uppercase(), (width - 24).toFloat(), 44f, catPaint)
        }

        // Línea divisoria
        val linePaint = Paint().apply {
            color = Color.rgb(225, 225, 235)
            strokeWidth = 1.5f
        }
        canvas.drawLine(24f, 56f, (width - 24).toFloat(), 56f, linePaint)

        // Nombre del artículo
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val nombreCorto = if (nombreArticulo.length > 28) nombreArticulo.take(26) + "..." else nombreArticulo
        canvas.drawText(nombreCorto, 24f, 92f, namePaint)

        // Precio Grande
        val pricePaint = Paint().apply {
            color = Color.rgb(216, 27, 96) // Rosa vibrante
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val precioStr = "$${String.format(Locale.US, "%.2f", precio)} MXN"
        canvas.drawText(precioStr, 24f, 142f, pricePaint)

        // Unidad de medida
        val unitPaint = Paint().apply {
            color = Color.rgb(90, 90, 100)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val precioWidth = pricePaint.measureText(precioStr)
        canvas.drawText("/ ${unidadMedida.uppercase()}", 24f + precioWidth + 12f, 138f, unitPaint)

        // Gráfico del Código de Barras
        val barcodeBitmap = generarBarrasBitmap(codigo, anchoDeseado = width - 48, altoDeseado = 95)
        canvas.drawBitmap(barcodeBitmap, 24f, 172f, null)

        // Texto del Código de Barras centrado abajo
        val codeTextPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 21f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(codigo, (width / 2f), 305f, codeTextPaint)

        // Pie de página discreto
        val footerPaint = Paint().apply {
            color = Color.rgb(160, 160, 170)
            textSize = 12f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("PRODUCTO ORIGINAL • CONTROL DE STOCK", (width / 2f), 336f, footerPaint)

        return bitmap
    }

    /**
     * Guarda la etiqueta temporalmente en el caché y lanza el Intent para compartir (WhatsApp, correo, etc.)
     */
    fun compartirEtiqueta(context: Context, bitmap: Bitmap, nombreArticulo: String) {
        try {
            val dir = File(context.cacheDir, "tickets").apply { if (!exists()) mkdirs() }
            val file = File(dir, "etiqueta_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "🏷️ Etiqueta de código de barras: $nombreArticulo")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir etiqueta de producto")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Envía la etiqueta a la impresora a través del servicio de impresión nativo de Android
     */
    fun imprimirEtiqueta(context: Context, bitmap: Bitmap, nombreArticulo: String) {
        try {
            val printHelper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
            }
            val jobName = "Etiqueta_${nombreArticulo.replace(" ", "_")}"
            printHelper.printBitmap(jobName, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
