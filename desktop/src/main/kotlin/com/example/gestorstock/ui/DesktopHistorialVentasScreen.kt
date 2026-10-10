package com.example.gestorstock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gestorstock.data.DesktopDataExportHelper
import com.example.gestorstock.data.model.VentaConDetalles
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PeriodoVentas(val etiqueta: String) {
    HOY("Hoy"),
    SEMANA("Últimos 7 días"),
    MES("Últimos 30 días"),
    TODO("Todo el historial")
}

@Composable
fun DesktopHistorialVentasScreen(
    viewModel: DesktopStockViewModel
) {
    val ventas by viewModel.ventas.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()

    var periodoSeleccionado by remember { mutableStateOf(PeriodoVentas.TODO) }
    var busquedaHistorial by remember { mutableStateOf("") }
    var ventaParaTicket by remember { mutableStateOf<VentaConDetalles?>(null) }
    var ventaParaAnular by remember { mutableStateOf<VentaConDetalles?>(null) }

    val sdfFechaHora = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale.getDefault())

    // Filtrar por período
    val ahora = System.currentTimeMillis()
    val inicioHoy = remember(ahora) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.timeInMillis
    }
    val sieteDiasAtras = ahora - 7L * 24 * 60 * 60 * 1000
    val treintaDiasAtras = ahora - 30L * 24 * 60 * 60 * 1000

    val ventasFiltradas = remember(ventas, periodoSeleccionado, busquedaHistorial) {
        ventas.filter { vd ->
            val cumplePeriodo = when (periodoSeleccionado) {
                PeriodoVentas.HOY -> vd.venta.fecha >= inicioHoy
                PeriodoVentas.SEMANA -> vd.venta.fecha >= sieteDiasAtras
                PeriodoVentas.MES -> vd.venta.fecha >= treintaDiasAtras
                PeriodoVentas.TODO -> true
            }

            val cumpleBusqueda = busquedaHistorial.isBlank() ||
                    vd.venta.id.toString().contains(busquedaHistorial) ||
                    vd.venta.metodoPago.contains(busquedaHistorial, ignoreCase = true) ||
                    vd.lineas.any { it.nombreArticulo.contains(busquedaHistorial, ignoreCase = true) }

            cumplePeriodo && cumpleBusqueda
        }
    }

    // Métricas
    val totalVentasCompletadas = ventasFiltradas.filter { it.venta.estado == "COMPLETADA" }
    val sumaIngresos = totalVentasCompletadas.sumOf { it.venta.totalPrecio }
    val promedioVenta = if (totalVentasCompletadas.isNotEmpty()) sumaIngresos / totalVentasCompletadas.size else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera y acciones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("📊 Historial de Ventas y Finanzas", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Reportes de ventas, tickets generados y anulación de pedidos", color = TextoSecundario, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                    val fd = FileDialog(null as Frame?, "Exportar Ventas a Excel (CSV)", FileDialog.SAVE).apply {
                        file = "ventas_${sdf.format(Date())}.csv"
                        isVisible = true
                    }
                    if (fd.directory != null && fd.file != null) {
                        val dest = File(fd.directory, fd.file)
                        DesktopDataExportHelper.exportarVentasCsv(dest, ventasFiltradas)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Morado)
            ) {
                Icon(Icons.Default.TableChart, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Exportar Historial a Excel")
            }
        }

        // Tarjetas de Métricas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ingresos Totales
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ingresos Totales", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("$${String.format(Locale.US, "%.2f", sumaIngresos)} MXN", color = Verde, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }

            // Cantidad de Ventas
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ventas Completadas", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("${totalVentasCompletadas.size} ventas", color = RosaClaro, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }

            // Promedio por Venta
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Ticket Promedio", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("$${String.format(Locale.US, "%.2f", promedioVenta)} MXN", color = MoradoClaro, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }

        // Filtros y Buscador
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selector de Período
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodoVentas.values().forEach { p ->
                    FilterChip(
                        selected = periodoSeleccionado == p,
                        onClick = { periodoSeleccionado = p },
                        label = { Text(p.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Rosa,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Buscador
            OutlinedTextField(
                value = busquedaHistorial,
                onValueChange = { busquedaHistorial = it },
                placeholder = { Text("Buscar por ID, artículo o método...", color = TextoSecundario, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = RosaClaro, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.width(320.dp).height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Rosa,
                    unfocusedBorderColor = Color(0xFF3D2A54),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = TextoPrimario
                )
            )
        }

        // Lista de Ventas
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCard)
        ) {
            if (ventasFiltradas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, null, tint = TextoSecundario, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No hay ventas registradas en este período", color = TextoSecundario)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(ventasFiltradas, key = { it.venta.id }) { vd ->
                        val v = vd.venta
                        val esAnulada = (v.estado == "ANULADA")

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (esAnulada) Rojo.copy(alpha = 0.2f) else Verde.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "Venta #${v.id}",
                                                color = if (esAnulada) Rojo else Verde,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }

                                        Text(
                                            sdfFechaHora.format(Date(v.fecha)),
                                            color = TextoSecundario,
                                            fontSize = 12.sp
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = FondoOscuro
                                        ) {
                                            Text(
                                                v.metodoPago,
                                                color = TextoPrimario,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (esAnulada) {
                                            Text("• ANULADA (STOCK REPUESTO)", color = Rojo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", v.totalPrecio)} MXN",
                                            color = if (esAnulada) TextoSecundario else Rosa,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )

                                        Button(
                                            onClick = { ventaParaTicket = vd },
                                            colors = ButtonDefaults.buttonColors(containerColor = FondoOscuro),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Ver Ticket", fontSize = 11.sp)
                                        }

                                        if (!esAnulada) {
                                            OutlinedButton(
                                                onClick = { ventaParaAnular = vd },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Undo, null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Anular", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                // Desglose de artículos
                                val desgloseTexto = vd.lineas.joinToString(", ") { "${it.cantidad}x ${it.nombreArticulo}" }
                                Text(
                                    "Artículos: $desgloseTexto",
                                    color = TextoSecundario,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal para ver Ticket histórico
    if (ventaParaTicket != null) {
        val ticketTexto = remember(ventaParaTicket) {
            DesktopTicketHelper.generarTextoTicketHistorico(ventaParaTicket!!, nombreTienda)
        }
        var copiadoHist by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { ventaParaTicket = null }) {
            Card(
                modifier = Modifier.width(520.dp).padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🧾 Ticket de Compra #${ventaParaTicket?.venta?.id}", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { ventaParaTicket = null }) {
                            Icon(Icons.Default.Close, null, tint = TextoSecundario)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    ) {
                        Text(
                            ticketTexto,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }

                    if (copiadoHist) {
                        Spacer(Modifier.height(8.dp))
                        Text("¡Ticket copiado al portapapeles!", color = Verde, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                DesktopTicketHelper.copiarAlPortapapeles(ticketTexto)
                                copiadoHist = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copiar", color = Color.White)
                        }

                        Button(
                            onClick = {
                                DesktopTicketHelper.imprimirTicket(ticketTexto, "Ticket_${ventaParaTicket?.venta?.id}")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Morado)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Imprimir")
                        }

                        Button(
                            onClick = { ventaParaTicket = null },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }

    // Modal confirmar anulación
    if (ventaParaAnular != null) {
        Dialog(onDismissRequest = { ventaParaAnular = null }) {
            Card(
                modifier = Modifier.width(460.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("⚠️ ¿Anular Venta #${ventaParaAnular?.venta?.id}?", color = Rojo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Al anular la venta, los artículos vendidos (${ventaParaAnular?.venta?.totalArticulos} pzas) se reincorporarán automáticamente al inventario.",
                        color = TextoPrimario,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { ventaParaAnular = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar", color = TextoSecundario)
                        }

                        Button(
                            onClick = {
                                viewModel.anularVenta(ventaParaAnular!!)
                                ventaParaAnular = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Rojo)
                        ) {
                            Text("Sí, Anular y Reponer", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
