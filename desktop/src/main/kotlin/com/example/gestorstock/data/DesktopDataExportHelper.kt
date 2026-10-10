package com.example.gestorstock.data

import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

data class DesktopBackupContenido(
    val categorias: List<Categoria>,
    val articulos: List<Articulo>,
    val ventas: List<VentaConDetalles>
)

object DesktopDataExportHelper {

    private fun sanitizarCsv(texto: String?): String {
        if (texto.isNullOrEmpty()) return ""
        val limpio = texto.replace("\"", "\"\"").replace("\n", " ").replace("\r", " ")
        return if (limpio.contains(";") || limpio.contains("\"") || limpio.contains(",")) {
            "\"$limpio\""
        } else {
            limpio
        }
    }

    fun exportarInventarioCsv(
        destino: File,
        articulos: List<Articulo>,
        categorias: List<Categoria>
    ) {
        val catMap = categorias.associateBy { it.id }
        val sdfFechaFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        FileOutputStream(destino).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // BOM UTF-8 para apertura directa en Microsoft Excel
                writer.write("\uFEFF")
                writer.write("ID;Nombre;Descripción;Categoría;Stock;Stock Mínimo;Unidad;Precio Costo;Precio Venta;Margen Beneficio %;Código de Barras;Caducidad;Fecha Alta\n")

                for (art in articulos) {
                    val catNombre = catMap[art.categoriaId]?.nombre ?: "Sin categoría"
                    val margen = if (art.precioCosto > 0) {
                        String.format(Locale.US, "%.1f", ((art.precio - art.precioCosto) / art.precioCosto) * 100)
                    } else "100.0"

                    val fechaCad = art.fechaCaducidad?.let { sdfFechaFormat.format(Date(it)) } ?: "N/A"
                    val fechaAlta = sdfFechaFormat.format(Date(art.fechaCreacion))

                    val linea = listOf(
                        art.id.toString(),
                        sanitizarCsv(art.nombre),
                        sanitizarCsv(art.descripcion),
                        sanitizarCsv(catNombre),
                        art.stock.toString(),
                        art.stockMinimo.toString(),
                        sanitizarCsv(art.unidadMedida),
                        String.format(Locale.US, "%.2f", art.precioCosto),
                        String.format(Locale.US, "%.2f", art.precio),
                        margen,
                        sanitizarCsv(art.codigoBarras ?: ""),
                        fechaCad,
                        fechaAlta
                    ).joinToString(";")

                    writer.write(linea)
                    writer.write("\n")
                }
            }
        }
    }

    fun exportarVentasCsv(
        destino: File,
        ventas: List<VentaConDetalles>
    ) {
        val sdfFechaVenta = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfHoraVenta = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        FileOutputStream(destino).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                writer.write("\uFEFF")
                writer.write("ID Venta;Fecha;Hora;Total Artículos;Importe Total;Método de Pago;Pago Con;Cambio;Estado;Notas;Artículos Desglosados\n")

                for (vd in ventas) {
                    val v = vd.venta
                    val fecha = sdfFechaVenta.format(Date(v.fecha))
                    val hora = sdfHoraVenta.format(Date(v.fecha))

                    val desglose = vd.lineas.joinToString(" | ") { linea ->
                        "${linea.cantidad}x ${linea.nombreArticulo} (${String.format(Locale.US, "%.2f", linea.subtotal)})"
                    }

                    val linea = listOf(
                        v.id.toString(),
                        fecha,
                        hora,
                        v.totalArticulos.toString(),
                        String.format(Locale.US, "%.2f", v.totalPrecio),
                        sanitizarCsv(v.metodoPago),
                        v.pagoCon?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        v.cambio?.let { String.format(Locale.US, "%.2f", it) } ?: "",
                        sanitizarCsv(v.estado),
                        sanitizarCsv(v.notas),
                        sanitizarCsv(desglose)
                    ).joinToString(";")

                    writer.write(linea)
                    writer.write("\n")
                }
            }
        }
    }

    fun obtenerFotoBase64(fotoPath: String?): String? {
        if (fotoPath.isNullOrBlank()) return null
        return try {
            val file = File(fotoPath)
            if (file.exists()) {
                Base64.getEncoder().encodeToString(file.readBytes())
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restaurarFotoDesdeBase64(base64Str: String?, artId: Int): String? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val bytes = Base64.getDecoder().decode(base64Str)
            val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
            val dir = File(appData, "GestorStock\\articulos").apply { if (!exists()) mkdirs() }
            val fileName = "art_${System.currentTimeMillis()}_res_${artId}.jpg"
            val destFile = File(dir, fileName)
            destFile.writeBytes(bytes)
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportarBackupCompletoJson(
        destino: File,
        articulos: List<Articulo>,
        categorias: List<Categoria>,
        ventas: List<VentaConDetalles>
    ) {
        val root = JSONObject().apply {
            put("app", "GestorStock")
            put("version", 2)
            put("fechaExportacion", System.currentTimeMillis())

            // Categorías
            val catArray = JSONArray()
            for (c in categorias) {
                catArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("nombre", c.nombre)
                    put("colorHex", c.colorHex)
                })
            }
            put("categorias", catArray)

            // Artículos
            val artArray = JSONArray()
            for (a in articulos) {
                artArray.put(JSONObject().apply {
                    put("id", a.id)
                    put("nombre", a.nombre)
                    put("descripcion", a.descripcion)
                    put("precio", a.precio)
                    put("precioCosto", a.precioCosto)
                    put("stock", a.stock)
                    put("stockMinimo", a.stockMinimo)
                    put("unidadMedida", a.unidadMedida)
                    if (a.fechaCaducidad != null) put("fechaCaducidad", a.fechaCaducidad)
                    if (a.codigoBarras != null) put("codigoBarras", a.codigoBarras)
                    put("categoriaId", a.categoriaId ?: JSONObject.NULL)
                    put("fechaCreacion", a.fechaCreacion)

                    val fotoBase64 = obtenerFotoBase64(a.fotoUri)
                    if (fotoBase64 != null) {
                        put("fotoBase64", fotoBase64)
                    } else if (a.fotoUri != null) {
                        put("fotoUri", a.fotoUri)
                    }
                })
            }
            put("articulos", artArray)

            // Ventas
            val vArray = JSONArray()
            for (vd in ventas) {
                val v = vd.venta
                val vObj = JSONObject().apply {
                    put("id", v.id)
                    put("fecha", v.fecha)
                    put("totalArticulos", v.totalArticulos)
                    put("totalPrecio", v.totalPrecio)
                    if (v.pagoCon != null) put("pagoCon", v.pagoCon)
                    if (v.cambio != null) put("cambio", v.cambio)
                    put("metodoPago", v.metodoPago)
                    put("notas", v.notas)
                    put("estado", v.estado)

                    val lineasArray = JSONArray()
                    for (l in vd.lineas) {
                        lineasArray.put(JSONObject().apply {
                            put("articuloId", l.articuloId ?: JSONObject.NULL)
                            put("nombreArticulo", l.nombreArticulo)
                            put("precioUnitario", l.precioUnitario)
                            put("cantidad", l.cantidad)
                            put("subtotal", l.subtotal)
                            put("categoriaNombre", l.categoriaNombre ?: JSONObject.NULL)
                        })
                    }
                    put("lineas", lineasArray)
                }
                vArray.put(vObj)
            }
            put("ventas", vArray)
        }

        FileOutputStream(destino).use { fos ->
            fos.write(root.toString(2).toByteArray(StandardCharsets.UTF_8))
        }
    }

    fun parsearBackupJson(jsonString: String): DesktopBackupContenido? {
        return try {
            val root = JSONObject(jsonString)
            val categorias = mutableListOf<Categoria>()
            val articulos = mutableListOf<Articulo>()
            val ventas = mutableListOf<VentaConDetalles>()

            if (root.has("categorias")) {
                val catArray = root.getJSONArray("categorias")
                for (i in 0 until catArray.length()) {
                    val c = catArray.getJSONObject(i)
                    categorias.add(
                        Categoria(
                            id = c.optInt("id", 0),
                            nombre = c.getString("nombre"),
                            colorHex = c.optString("colorHex", "#E91E63")
                        )
                    )
                }
            }

            if (root.has("articulos")) {
                val artArray = root.getJSONArray("articulos")
                for (i in 0 until artArray.length()) {
                    val a = artArray.getJSONObject(i)
                    val fotoBase64 = if (a.has("fotoBase64") && !a.isNull("fotoBase64")) a.getString("fotoBase64") else null
                    val fotoRestaurada = if (fotoBase64 != null) {
                        restaurarFotoDesdeBase64(fotoBase64, a.optInt("id", 0))
                    } else if (a.has("fotoUri") && !a.isNull("fotoUri")) {
                        a.getString("fotoUri")
                    } else null

                    articulos.add(
                        Articulo(
                            id = a.optInt("id", 0),
                            nombre = a.getString("nombre"),
                            descripcion = a.optString("descripcion", ""),
                            precio = a.optDouble("precio", 0.0),
                            precioCosto = a.optDouble("precioCosto", 0.0),
                            stock = a.optInt("stock", 0),
                            stockMinimo = a.optInt("stockMinimo", 2),
                            unidadMedida = a.optString("unidadMedida", "pza"),
                            fechaCaducidad = if (a.has("fechaCaducidad") && !a.isNull("fechaCaducidad")) a.getLong("fechaCaducidad") else null,
                            codigoBarras = if (a.has("codigoBarras") && !a.isNull("codigoBarras")) a.getString("codigoBarras") else null,
                            categoriaId = if (a.has("categoriaId") && !a.isNull("categoriaId")) a.getInt("categoriaId") else null,
                            fechaCreacion = a.optLong("fechaCreacion", System.currentTimeMillis()),
                            fotoUri = fotoRestaurada
                        )
                    )
                }
            }

            if (root.has("ventas")) {
                val vArray = root.getJSONArray("ventas")
                for (i in 0 until vArray.length()) {
                    val vObj = vArray.getJSONObject(i)
                    val venta = Venta(
                        id = vObj.optLong("id", 0),
                        fecha = vObj.optLong("fecha", System.currentTimeMillis()),
                        totalArticulos = vObj.optInt("totalArticulos", 0),
                        totalPrecio = vObj.optDouble("totalPrecio", 0.0),
                        pagoCon = if (vObj.has("pagoCon") && !vObj.isNull("pagoCon")) vObj.getDouble("pagoCon") else null,
                        cambio = if (vObj.has("cambio") && !vObj.isNull("cambio")) vObj.getDouble("cambio") else null,
                        metodoPago = vObj.optString("metodoPago", "Efectivo"),
                        notas = vObj.optString("notas", ""),
                        estado = vObj.optString("estado", "COMPLETADA")
                    )

                    val lineas = mutableListOf<LineaVenta>()
                    if (vObj.has("lineas")) {
                        val lineasArray = vObj.getJSONArray("lineas")
                        for (j in 0 until lineasArray.length()) {
                            val lObj = lineasArray.getJSONObject(j)
                            lineas.add(
                                LineaVenta(
                                    id = 0,
                                    ventaId = venta.id,
                                    articuloId = if (lObj.has("articuloId") && !lObj.isNull("articuloId")) lObj.getInt("articuloId") else null,
                                    nombreArticulo = lObj.getString("nombreArticulo"),
                                    precioUnitario = lObj.optDouble("precioUnitario", 0.0),
                                    cantidad = lObj.optInt("cantidad", 1),
                                    subtotal = lObj.optDouble("subtotal", 0.0),
                                    categoriaNombre = if (lObj.has("categoriaNombre") && !lObj.isNull("categoriaNombre")) lObj.getString("categoriaNombre") else null
                                )
                            )
                        }
                    }
                    ventas.add(VentaConDetalles(venta, lineas))
                }
            }

            DesktopBackupContenido(categorias, articulos, ventas)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
