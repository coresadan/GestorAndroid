package com.example.gestorstock.ui

import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.VentaConDetalles
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.print.Printable
import java.awt.print.PrinterJob
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DesktopVentaItem(
    val articulo: Articulo,
    val cantidad: Int,
    val subtotal: Double
)

data class DesktopVentaResumen(
    val tienda: String,
    val items: List<DesktopVentaItem>,
    val totalArticulos: Int,
    val totalPrecio: Double,
    val metodoPago: String,
    val pagoCon: Double? = null,
    val cambio: Double? = null,
    val descuento: Double = 0.0,
    val porcentajeDescuento: Int? = null
)

object DesktopTicketHelper {

    fun generarTextoTicket(resumen: DesktopVentaResumen): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
        val fechaStr = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("🌸 *${resumen.tienda.ifBlank { "Mi Tiendita" }} - Comprobante de Compra* 🌸\n")
        sb.append("📅 Fecha: $fechaStr\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        resumen.items.forEach { item ->
            sb.append("• ${item.articulo.nombre}\n")
            sb.append("  ${item.cantidad} ${item.articulo.unidadMedida} x $${String.format(Locale.US, "%.2f", item.articulo.precio)} = *$${String.format(Locale.US, "%.2f", item.subtotal)} MXN*\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🛍️ Total de piezas: ${resumen.totalArticulos}\n")
        val metodoStr = when (resumen.metodoPago.uppercase()) {
            "TRANSFERENCIA" -> "📲 Transferencia / SPEI"
            "TARJETA" -> "💳 Tarjeta"
            else -> "💵 Efectivo"
        }
        sb.append("💳 Método de pago: $metodoStr\n")
        if (resumen.descuento > 0) {
            sb.append("🏷️ Descuento aplicado: -$${String.format(Locale.US, "%.2f", resumen.descuento)} MXN (${resumen.porcentajeDescuento ?: 0}%)\n")
        }
        sb.append("💰 TOTAL PAGADO: *$${String.format(Locale.US, "%.2f", resumen.totalPrecio)} MXN*\n")

        if (resumen.metodoPago.uppercase() == "EFECTIVO" && resumen.pagoCon != null) {
            sb.append("💵 Efectivo recibido: $${String.format(Locale.US, "%.2f", resumen.pagoCon)} MXN\n")
            if (resumen.cambio != null) {
                sb.append("🪙 Cambio devuelto: $${String.format(Locale.US, "%.2f", resumen.cambio)} MXN\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("✨ ¡Muchas gracias por tu compra y por apoyar nuestro negocio! ❤️")

        return sb.toString()
    }

    fun generarTextoTicketHistorico(ventaConDetalles: VentaConDetalles, nombreTienda: String): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
        val fechaStr = sdf.format(Date(ventaConDetalles.venta.fecha))

        val sb = StringBuilder()
        sb.append("🌸 *${nombreTienda.ifBlank { "Mi Tiendita" }} - Comprobante de Compra* 🌸\n")
        sb.append("📅 Fecha: $fechaStr\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        ventaConDetalles.lineas.forEach { linea ->
            sb.append("• ${linea.nombreArticulo}\n")
            sb.append("  ${linea.cantidad} pzs x $${String.format(Locale.US, "%.2f", linea.precioUnitario)} = *$${String.format(Locale.US, "%.2f", linea.subtotal)} MXN*\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🛍️ Total de piezas: ${ventaConDetalles.venta.totalArticulos}\n")
        val metodoHistStr = when (ventaConDetalles.venta.metodoPago.uppercase()) {
            "TRANSFERENCIA" -> "📲 Transferencia / SPEI"
            "TARJETA" -> "💳 Tarjeta"
            else -> "💵 Efectivo"
        }
        sb.append("💳 Método de pago: $metodoHistStr\n")
        sb.append("💰 TOTAL PAGADO: *$${String.format(Locale.US, "%.2f", ventaConDetalles.venta.totalPrecio)} MXN*\n")

        if (ventaConDetalles.venta.metodoPago.uppercase() == "EFECTIVO" && ventaConDetalles.venta.pagoCon != null) {
            sb.append("💵 Efectivo recibido: $${String.format(Locale.US, "%.2f", ventaConDetalles.venta.pagoCon)} MXN\n")
            if (ventaConDetalles.venta.cambio != null) {
                sb.append("🪙 Cambio devuelto: $${String.format(Locale.US, "%.2f", ventaConDetalles.venta.cambio)} MXN\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("✨ ¡Muchas gracias por tu compra y por apoyar nuestro negocio! ❤️")

        return sb.toString()
    }

    fun copiarAlPortapapeles(texto: String): Boolean {
        return try {
            val selection = StringSelection(texto)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun imprimirTicket(texto: String, titulo: String = "Ticket_Compra"): Boolean {
        return try {
            val job = PrinterJob.getPrinterJob()
            job.setJobName(titulo)
            job.setPrintable(Printable { graphics, pageFormat, pageIndex ->
                if (pageIndex > 0) return@Printable Printable.NO_SUCH_PAGE
                val g2d = graphics as Graphics2D
                g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
                g2d.font = Font("Monospaced", Font.PLAIN, 10)

                val lineas = texto.replace("*", "").lines()
                var y = 20
                for (linea in lineas) {
                    g2d.drawString(linea, 10, y)
                    y += 14
                }
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
