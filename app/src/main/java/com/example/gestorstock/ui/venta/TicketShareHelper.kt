package com.example.gestorstock.ui.venta

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.gestorstock.data.model.VentaConDetalles
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TicketShareHelper {

    fun generarTextoTicket(resumen: VentaResumen): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
        val fechaStr = sdf.format(Date())

        val sb = StringBuilder()
        sb.append("🌸 *${resumen.tienda.ifBlank { "Mi Tiendita" }} - Comprobante de Compra* 🌸\n")
        sb.append("📅 Fecha: $fechaStr\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        resumen.items.forEach { item ->
            sb.append("• ${item.articulo.nombre}\n")
            sb.append("  ${item.cantidad} pzs x $${String.format(Locale.US, "%.0f", item.articulo.precio)} = *$${String.format(Locale.US, "%.2f", item.subtotal)} MXN*\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🛍️ *Total de piezas:* ${resumen.totalArticulos}\n")
        val metodoStr = when(resumen.metodoPago) {
            "TRANSFERENCIA" -> "📲 Transferencia / SPEI"
            "TARJETA" -> "💳 Tarjeta"
            else -> "💵 Efectivo"
        }
        sb.append("💳 *Método de pago:* $metodoStr\n")
        if (resumen.descuento > 0) {
            sb.append("🏷️ *Descuento aplicado:* -$${String.format(Locale.US, "%.2f", resumen.descuento)} MXN (${resumen.porcentajeDescuento ?: 0}%)\n")
        }
        sb.append("💰 *TOTAL PAGADO:* *$${String.format(Locale.US, "%.2f", resumen.totalPrecio)} MXN*\n")

        if (resumen.metodoPago == "EFECTIVO" && resumen.pagoCon != null) {
            sb.append("💵 *Efectivo recibido:* $${String.format(Locale.US, "%.2f", resumen.pagoCon)} MXN\n")
            if (resumen.cambio != null) {
                sb.append("🪙 *Cambio devuelto:* $${String.format(Locale.US, "%.2f", resumen.cambio)} MXN\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("✨ ¡Muchas gracias por tu compra y por apoyar el arte hecho a mano! 🧶❤️")

        return sb.toString()
    }

    fun generarTextoTicketHistorico(ventaConDetalles: VentaConDetalles, nombreTienda: String): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
        val fechaStr = sdf.format(Date(ventaConDetalles.venta.fecha))

        val sb = StringBuilder()
        sb.append("🌸 *${nombreTienda.ifBlank { "Mi Tiendita" }} - Comprobante de Compra* 🌸\n")
        sb.append("📅 Fecha: $fechaStr\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")

        ventaConDetalles.lineas.forEach { linea ->
            sb.append("• ${linea.nombreArticulo}\n")
            sb.append("  ${linea.cantidad} pzs x $${String.format(Locale.US, "%.0f", linea.precioUnitario)} = *$${String.format(Locale.US, "%.2f", linea.subtotal)} MXN*\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🛍️ *Total de piezas:* ${ventaConDetalles.venta.totalArticulos}\n")
        val metodoHistStr = when(ventaConDetalles.venta.metodoPago) {
            "TRANSFERENCIA" -> "📲 Transferencia / SPEI"
            "TARJETA" -> "💳 Tarjeta"
            else -> "💵 Efectivo"
        }
        sb.append("💳 *Método de pago:* $metodoHistStr\n")
        sb.append("💰 *TOTAL PAGADO:* *$${String.format(Locale.US, "%.2f", ventaConDetalles.venta.totalPrecio)} MXN*\n")

        if (ventaConDetalles.venta.metodoPago == "EFECTIVO" && ventaConDetalles.venta.pagoCon != null) {
            sb.append("💵 *Efectivo recibido:* $${String.format(Locale.US, "%.2f", ventaConDetalles.venta.pagoCon)} MXN\n")
            if (ventaConDetalles.venta.cambio != null) {
                sb.append("🪙 *Cambio devuelto:* $${String.format(Locale.US, "%.2f", ventaConDetalles.venta.cambio)} MXN\n")
            }
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("✨ ¡Muchas gracias por tu compra y por apoyar el arte hecho a mano! 🧶❤️")

        return sb.toString()
    }

    fun compartirTicketTexto(context: Context, texto: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        val chooser = Intent.createChooser(intent, "Compartir ticket de compra")
        context.startActivity(chooser)
    }

    fun compartirTicketWhatsApp(context: Context, texto: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Si no tiene WhatsApp instalado, abrir selector general
            compartirTicketTexto(context, texto)
        }
    }
}
