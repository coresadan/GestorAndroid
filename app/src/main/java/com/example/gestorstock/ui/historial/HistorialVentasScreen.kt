package com.example.gestorstock.ui.historial

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.gestorstock.data.DataExportHelper
import com.example.gestorstock.data.model.VentaConDetalles
import com.example.gestorstock.theme.*
import com.example.gestorstock.ui.StockViewModel
import com.example.gestorstock.ui.venta.TicketShareHelper
import java.text.SimpleDateFormat
import java.util.*

enum class FiltroFecha(val label: String) {
    HOY("Hoy"),
    ULTIMOS_7_DIAS("7 Días"),
    ESTE_MES("Este Mes"),
    TODO("Todo")
}

data class ArticuloVendidoStat(
    val nombre: String,
    val totalCantidad: Int,
    val totalIngresos: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialVentasScreen(
    viewModel: StockViewModel,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val ventas by viewModel.ventas.collectAsState()
    val todosArticulos by viewModel.todosArticulos.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()

    var filtroFecha by remember { mutableStateOf(FiltroFecha.HOY) }
    var mostrarEstadisticasAvanzadas by remember { mutableStateOf(false) }
    var ventaSeleccionadaDetalle by remember { mutableStateOf<VentaConDetalles?>(null) }
    var ventaParaAnular by remember { mutableStateOf<VentaConDetalles?>(null) }
    var mostrarCierreCaja by remember { mutableStateOf(false) }

    // Filtrar ventas por rango de fecha
    val ahora = Calendar.getInstance()
    val ventasFiltradas = remember(ventas, filtroFecha) {
        when (filtroFecha) {
            FiltroFecha.HOY -> {
                val inicioHoy = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                ventas.filter { it.venta.fecha >= inicioHoy }
            }
            FiltroFecha.ULTIMOS_7_DIAS -> {
                val hace7Dias = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                ventas.filter { it.venta.fecha >= hace7Dias }
            }
            FiltroFecha.ESTE_MES -> {
                val inicioMes = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
                ventas.filter { it.venta.fecha >= inicioMes }
            }
            FiltroFecha.TODO -> ventas
        }
    }

    // Cálculos de métricas
    val ventasValidas = ventasFiltradas.filter { it.venta.estado == "COMPLETADA" }
    val totalIngresos = ventasValidas.sumOf { it.venta.totalPrecio }
    val totalPiezasVendidas = ventasValidas.sumOf { it.venta.totalArticulos }
    val cantidadVentas = ventasValidas.size
    val ticketPromedio = if (cantidadVentas > 0) totalIngresos / cantidadVentas else 0.0

    // Cálculo de rentabilidad neta estimada
    val mapaCostos = remember(todosArticulos) {
        todosArticulos.associate { it.id to it.precioCosto }
    }
    val costoMercanciaTotal = remember(ventasValidas, mapaCostos) {
        ventasValidas.sumOf { v ->
            v.lineas.sumOf { l ->
                (mapaCostos[l.articuloId] ?: 0.0) * l.cantidad
            }
        }
    }
    val gananciaNetaEstimada = totalIngresos - costoMercanciaTotal
    val margenNetoEstimado = if (totalIngresos > 0) (gananciaNetaEstimada / totalIngresos) * 100 else 0.0

    // Desglose por método de pago
    val totalEfectivo = remember(ventasValidas) {
        ventasValidas.filter { it.venta.metodoPago.equals("EFECTIVO", ignoreCase = true) }.sumOf { it.venta.totalPrecio }
    }
    val totalTransf = remember(ventasValidas) {
        ventasValidas.filter { it.venta.metodoPago.equals("TRANSFERENCIA", ignoreCase = true) }.sumOf { it.venta.totalPrecio }
    }
    val totalTarjeta = remember(ventasValidas) {
        ventasValidas.filter { it.venta.metodoPago.equals("TARJETA", ignoreCase = true) }.sumOf { it.venta.totalPrecio }
    }

    // Top 5 artículos vendidos
    val topVendidos = remember(ventasValidas) {
        val mapa = mutableMapOf<String, Pair<Int, Double>>()
        ventasValidas.forEach { v ->
            v.lineas.forEach { l ->
                val prev = mapa[l.nombreArticulo] ?: (0 to 0.0)
                mapa[l.nombreArticulo] = (prev.first + l.cantidad) to (prev.second + l.subtotal)
            }
        }
        mapa.entries
            .map { ArticuloVendidoStat(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.totalCantidad }
            .take(5)
    }
    val maxCantidadVendida = remember(topVendidos) {
        (topVendidos.maxOfOrNull { it.totalCantidad } ?: 1).coerceAtLeast(1)
    }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0718),
                    titleContentColor = TextoPrimario
                ),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextoSecundario)
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PointOfSale, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Historial y Cierre de Ventas", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                if (nombreTienda.isNotBlank()) nombreTienda else "Mi Tiendita",
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val archivo = DataExportHelper.exportarVentasCsv(context, ventasFiltradas)
                            DataExportHelper.compartirArchivo(context, archivo, "text/csv", "Exportar Ventas a CSV")
                        }
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Exportar CSV", tint = Verde)
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            mostrarCierreCaja = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Morado),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(Icons.Filled.Assessment, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cierre del Día", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Filtros de fecha (Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FiltroFecha.values().forEach { f ->
                    val seleccionado = filtroFecha == f
                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            filtroFecha = f
                        },
                        label = { Text(f.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Rosa,
                            selectedLabelColor = Color.White,
                            containerColor = FondoCard,
                            labelColor = TextoSecundario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionado,
                            borderColor = if (seleccionado) Rosa else Color.Transparent
                        )
                    )
                }
            }

            // Tarjetas de Métricas Resumen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricaCard(
                    titulo = "Ingresos Totales",
                    valor = "$${String.format(Locale.US, "%.2f", totalIngresos)}",
                    color = Verde,
                    icono = Icons.Filled.AttachMoney,
                    modifier = Modifier.weight(1f)
                )

                MetricaCard(
                    titulo = "Piezas Vendidas",
                    valor = "$totalPiezasVendidas pzs",
                    color = MoradoClaro,
                    icono = Icons.Filled.ShoppingBag,
                    modifier = Modifier.weight(0.85f)
                )

                MetricaCard(
                    titulo = "Ticket Promedio",
                    valor = "$${String.format(Locale.US, "%.0f", ticketPromedio)}",
                    color = Rosa,
                    icono = Icons.Filled.TrendingUp,
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Tarjeta Desplegable: Estadísticas Avanzadas & Rentabilidad
            Card(
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(Rosa.copy(alpha = 0.5f), Morado.copy(alpha = 0.4f), Color.Transparent)
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mostrarEstadisticasAvanzadas = !mostrarEstadisticasAvanzadas
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Insights, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(
                                    "Estadísticas y Rentabilidad",
                                    color = TextoPrimario,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    "Ganancia neta, Top 5 y métodos de cobro",
                                    color = TextoSecundario,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val archivo = DataExportHelper.exportarVentasCsv(context, ventasFiltradas)
                                    DataExportHelper.compartirArchivo(context, archivo, "text/csv", "Exportar Ventas a CSV")
                                }
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Exportar CSV", tint = MoradoClaro, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                mostrarEstadisticasAvanzadas = !mostrarEstadisticasAvanzadas
                            }) {
                                Icon(
                                    if (mostrarEstadisticasAvanzadas) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Desplegar",
                                    tint = TextoSecundario
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = mostrarEstadisticasAvanzadas,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = Color(0xFF2E1C48))

                            // 1. Rentabilidad Neta Estimada
                            Surface(
                                color = FondoOscuro,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Ganancia Neta Estimada", color = TextoSecundario, fontSize = 11.sp)
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", gananciaNetaEstimada)} MXN",
                                            color = if (gananciaNetaEstimada >= 0) Verde else Rojo,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            "Costo mercancía: $${String.format(Locale.US, "%.2f", costoMercanciaTotal)}",
                                            color = TextoDeshabilitado,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Surface(
                                        color = (if (margenNetoEstimado >= 0) Verde else Rojo).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, (if (margenNetoEstimado >= 0) Verde else Rojo).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            "Margen: ${String.format(Locale.US, "%.1f", margenNetoEstimado)}%",
                                            color = if (margenNetoEstimado >= 0) Verde else Rojo,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Desglose de Métodos de Pago
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Desglose por Forma de Pago", color = TextoPrimario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MetodoPagoPill(
                                        label = "Efectivo",
                                        monto = totalEfectivo,
                                        porcentaje = if (totalIngresos > 0) (totalEfectivo / totalIngresos) * 100 else 0.0,
                                        color = Verde,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetodoPagoPill(
                                        label = "Transferencia",
                                        monto = totalTransf,
                                        porcentaje = if (totalIngresos > 0) (totalTransf / totalIngresos) * 100 else 0.0,
                                        color = MoradoClaro,
                                        modifier = Modifier.weight(1f)
                                    )
                                    MetodoPagoPill(
                                        label = "Tarjeta",
                                        monto = totalTarjeta,
                                        porcentaje = if (totalIngresos > 0) (totalTarjeta / totalIngresos) * 100 else 0.0,
                                        color = Rosa,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // 3. Top 5 Productos Más Vendidos con Barras Gráficas
                            if (topVendidos.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Top 5 Más Vendidos", color = TextoPrimario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Unidades / Total", color = TextoDeshabilitado, fontSize = 10.sp)
                                    }

                                    topVendidos.forEachIndexed { index, item ->
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "${index + 1}. ${item.nombre}",
                                                    color = TextoPrimario,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    "${item.totalCantidad} pzs • $${String.format(Locale.US, "%.0f", item.totalIngresos)}",
                                                    color = Rosa,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            // Barra de Progreso Gráfica
                                            val proporcion = (item.totalCantidad.toFloat() / maxCantidadVendida.toFloat()).coerceIn(0.05f, 1f)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .background(FondoOscuro, RoundedCornerShape(3.dp))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(proporcion)
                                                        .fillMaxHeight()
                                                        .background(
                                                            Brush.horizontalGradient(listOf(MoradoClaro, Rosa)),
                                                            RoundedCornerShape(3.dp)
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lista de ventas realizadas
            if (ventasFiltradas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(FondoCardElevado, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.ReceiptLong, null, tint = TextoDeshabilitado, modifier = Modifier.size(36.dp))
                        }
                        Text("No hay ventas en este período", color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "Las ventas que cobres en la pestaña de Venta Rápida quedarán registradas aquí.",
                            color = TextoSecundario,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ventasFiltradas, key = { it.venta.id }) { itemVenta ->
                        VentaItemCard(
                            item = itemVenta,
                            nombreTienda = nombreTienda,
                            onClick = { ventaSeleccionadaDetalle = itemVenta },
                            onCompartirWhatsApp = {
                                val texto = TicketShareHelper.generarTextoTicketHistorico(itemVenta, nombreTienda)
                                TicketShareHelper.compartirTicketWhatsApp(context, texto)
                            },
                            onAnular = { ventaParaAnular = itemVenta }
                        )
                    }
                }
            }
        }
    }

    // Modal de Detalle de Venta
    ventaSeleccionadaDetalle?.let { detalle ->
        DetalleVentaDialog(
            ventaConDetalles = detalle,
            nombreTienda = nombreTienda,
            onCompartirWhatsApp = {
                val texto = TicketShareHelper.generarTextoTicketHistorico(detalle, nombreTienda)
                TicketShareHelper.compartirTicketWhatsApp(context, texto)
            },
            onCompartirTexto = {
                val texto = TicketShareHelper.generarTextoTicketHistorico(detalle, nombreTienda)
                TicketShareHelper.compartirTicketTexto(context, texto)
            },
            onAnular = {
                ventaParaAnular = detalle
                ventaSeleccionadaDetalle = null
            },
            onCerrar = { ventaSeleccionadaDetalle = null }
        )
    }

    // Modal de Confirmación para Anular Venta
    ventaParaAnular?.let { paraAnular ->
        AlertDialog(
            onDismissRequest = { ventaParaAnular = null },
            containerColor = FondoCardElevado,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = Rojo, modifier = Modifier.size(24.dp))
                    Text("¿Anular Venta / Devolución?", color = TextoPrimario, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Se marcará esta venta como ANULADA y se devolverán las siguientes unidades al inventario:",
                        color = TextoSecundario,
                        fontSize = 13.sp
                    )
                    paraAnular.lineas.forEach { linea ->
                        Text("• ${linea.nombreArticulo} (+${linea.cantidad} unidades)", color = TextoPrimario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.anularVenta(paraAnular)
                        ventaParaAnular = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo)
                ) {
                    Text("Anular y Reponer Stock", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ventaParaAnular = null }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }

    // Modal de Cierre de Caja / Balance del Día
    if (mostrarCierreCaja) {
        CierreCajaDialog(
            ventas = ventasValidas,
            nombreTienda = nombreTienda,
            filtroActual = filtroFecha.label,
            totalIngresos = totalIngresos,
            totalPiezas = totalPiezasVendidas,
            totalVentas = cantidadVentas,
            onCompartir = { textoCierre ->
                TicketShareHelper.compartirTicketWhatsApp(context, textoCierre)
            },
            onCerrar = { mostrarCierreCaja = false }
        )
    }
}

// -------------------------------------------------------------
// Componente: Tarjeta de Métrica
// -------------------------------------------------------------
@Composable
private fun MetricaCard(
    titulo: String,
    valor: String,
    color: Color,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
                Text(titulo, color = TextoSecundario, fontSize = 11.sp, maxLines = 1)
            }
            Text(valor, color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

// -------------------------------------------------------------
// Componente: Pill de Método de Pago
// -------------------------------------------------------------
@Composable
private fun MetodoPagoPill(
    label: String,
    monto: Double,
    porcentaje: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = FondoOscuro,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                Text(label, color = TextoSecundario, fontSize = 10.sp, maxLines = 1)
            }
            Text("$${String.format(Locale.US, "%.0f", monto)}", color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${String.format(Locale.US, "%.0f", porcentaje)}%", color = TextoDeshabilitado, fontSize = 9.sp)
        }
    }
}

// -------------------------------------------------------------
// Componente: Fila de Venta en el Historial
// -------------------------------------------------------------
@Composable
private fun VentaItemCard(
    item: VentaConDetalles,
    nombreTienda: String,
    onClick: () -> Unit,
    onCompartirWhatsApp: () -> Unit,
    onAnular: () -> Unit
) {
    val esAnulada = item.venta.estado == "ANULADA"
    val sdf = SimpleDateFormat("dd/MM/yy - hh:mm a", Locale("es", "MX"))
    val fechaStr = sdf.format(Date(item.venta.fecha))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (esAnulada) FondoCard.copy(alpha = 0.5f) else FondoCard
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            if (esAnulada) Rojo.copy(alpha = 0.4f) else Color(0xFF2E1C48)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icono / Estado
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (esAnulada) Rojo.copy(alpha = 0.15f)
                        else Verde.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (esAnulada) Icons.Filled.Cancel else Icons.Filled.Receipt,
                    null,
                    tint = if (esAnulada) Rojo else Verde,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Datos de la venta
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        fechaStr,
                        color = TextoSecundario,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (esAnulada) {
                        Surface(
                            color = Rojo.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("ANULADA", color = Rojo, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Resumen de productos
                val nombres = item.lineas.joinToString(", ") { "${it.nombreArticulo} (x${it.cantidad})" }
                Text(
                    nombres,
                    color = if (esAnulada) TextoDeshabilitado else TextoPrimario,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${item.venta.totalArticulos} artículos • Método: ${item.venta.metodoPago}",
                    color = TextoDeshabilitado,
                    fontSize = 11.sp
                )
            }

            // Precio Total y Botón WhatsApp
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "$${String.format(Locale.US, "%.2f", item.venta.totalPrecio)}",
                    color = if (esAnulada) TextoDeshabilitado else Rosa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (!esAnulada) {
                    IconButton(
                        onClick = onCompartirWhatsApp,
                        modifier = Modifier
                            .size(30.dp)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Diálogo Detalle de Venta
// -------------------------------------------------------------
@Composable
private fun DetalleVentaDialog(
    ventaConDetalles: VentaConDetalles,
    nombreTienda: String,
    onCompartirWhatsApp: () -> Unit,
    onCompartirTexto: () -> Unit,
    onAnular: () -> Unit,
    onCerrar: () -> Unit
) {
    val sdf = SimpleDateFormat("EEEE, dd 'de' MMMM yyyy - hh:mm a", Locale("es", "MX"))
    val fechaStr = sdf.format(Date(ventaConDetalles.venta.fecha))
    val esAnulada = ventaConDetalles.venta.estado == "ANULADA"

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 500.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Rosa, Morado)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Detalle de Venta", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(nombreTienda.ifBlank { "Mi Tiendita" }, color = MoradoClaro, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Filled.Close, null, tint = TextoSecundario)
                    }
                }

                Text(
                    fechaStr.replaceFirstChar { it.uppercase() },
                    color = TextoSecundario,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.Start).padding(vertical = 4.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Lista de artículos comprados
                Surface(
                    color = FondoOscuro,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ventaConDetalles.lineas.forEach { linea ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(linea.nombreArticulo, color = TextoPrimario, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("${linea.cantidad} pzs x $${String.format(Locale.US, "%.0f", linea.precioUnitario)}", color = TextoDeshabilitado, fontSize = 11.sp)
                                }
                                Text("$${String.format(Locale.US, "%.2f", linea.subtotal)}", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 8.dp))

                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("TOTAL (${ventaConDetalles.venta.totalArticulos} pzs):", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("$${String.format(Locale.US, "%.2f", ventaConDetalles.venta.totalPrecio)} MXN", color = Rosa, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        }

                        if (ventaConDetalles.venta.pagoCon != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Efectivo recibido:", color = TextoSecundario, fontSize = 12.sp)
                                Text("$${String.format(Locale.US, "%.2f", ventaConDetalles.venta.pagoCon)}", color = TextoPrimario, fontSize = 12.sp)
                            }
                            ventaConDetalles.venta.cambio?.let { cambio ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cambio entregado:", color = Verde, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("$${String.format(Locale.US, "%.2f", cambio)} MXN", color = Verde, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botones de acción
                if (!esAnulada) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCompartirWhatsApp,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onAnular,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo),
                            border = BorderStroke(1.dp, Rojo.copy(alpha = 0.5f)),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text("Anular", color = Rojo, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Diálogo Cierre de Caja / Balance
// -------------------------------------------------------------
@Composable
private fun CierreCajaDialog(
    ventas: List<VentaConDetalles>,
    nombreTienda: String,
    filtroActual: String,
    totalIngresos: Double,
    totalPiezas: Int,
    totalVentas: Int,
    onCompartir: (String) -> Unit,
    onCerrar: () -> Unit
) {
    // Desglose por método de pago
    val efectivoTotal = ventas.filter { it.venta.metodoPago == "EFECTIVO" }.sumOf { it.venta.totalPrecio }
    val transfTotal = ventas.filter { it.venta.metodoPago == "TRANSFERENCIA" }.sumOf { it.venta.totalPrecio }
    val tarjetaTotal = ventas.filter { it.venta.metodoPago == "TARJETA" }.sumOf { it.venta.totalPrecio }

    // Calcular artículo más vendido
    val agrupados = mutableMapOf<String, Int>()
    ventas.forEach { v ->
        v.lineas.forEach { linea ->
            agrupados[linea.nombreArticulo] = (agrupados[linea.nombreArticulo] ?: 0) + linea.cantidad
        }
    }
    val topVendidos = agrupados.entries.sortedByDescending { it.value }.take(3)

    fun generarTextoCierre(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy - hh:mm a", Locale("es", "MX"))
        val sb = StringBuilder()
        sb.append("📊 *CIERRE DE CAJA / BALANCE - ${nombreTienda.ifBlank { "Mi Tiendita" }}* 📊\n")
        sb.append("📅 Período: $filtroActual (${sdf.format(Date())})\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("💰 *TOTAL RECAUDADO:* *$${String.format(Locale.US, "%.2f", totalIngresos)} MXN*\n\n")
        sb.append("💳 *DESGLOSE POR FORMA DE PAGO:*\n")
        sb.append("• 💵 Efectivo en Caja: $${String.format(Locale.US, "%.2f", efectivoTotal)} MXN\n")
        sb.append("• 📲 Transferencias / SPEI: $${String.format(Locale.US, "%.2f", transfTotal)} MXN\n")
        sb.append("• 💳 Tarjetas: $${String.format(Locale.US, "%.2f", tarjetaTotal)} MXN\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🛍️ *Total de Piezas Vendidas:* $totalPiezas piezas\n")
        sb.append("🧾 *Ventas Realizadas:* $totalVentas transacciones\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━\n")
        if (topVendidos.isNotEmpty()) {
            sb.append("🌟 *Artículos más vendidos:*\n")
            topVendidos.forEachIndexed { i, e ->
                sb.append("${i + 1}. ${e.key}: ${e.value} pzs\n")
            }
        }
        return sb.toString()
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 480.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Verde, Morado)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Brush.linearGradient(listOf(Verde, Color(0xFF1B5E20))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Assessment, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }

                Spacer(Modifier.height(8.dp))

                Text("Cierre de Caja y Balance", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    "Resumen ($filtroActual)",
                    color = TextoSecundario,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(14.dp))

                // Métricas principales
                Surface(
                    color = FondoOscuro,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Recaudado:", color = TextoSecundario, fontSize = 13.sp)
                            Text("$${String.format(Locale.US, "%.2f", totalIngresos)} MXN", color = Verde, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 2.dp))

                        Text("Desglose de Ingresos:", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💵 Efectivo:", color = TextoPrimario, fontSize = 12.sp)
                            Text("$${String.format(Locale.US, "%.2f", efectivoTotal)}", color = Verde, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📲 Transferencia / SPEI:", color = TextoPrimario, fontSize = 12.sp)
                            Text("$${String.format(Locale.US, "%.2f", transfTotal)}", color = MoradoClaro, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💳 Tarjeta:", color = TextoPrimario, fontSize = 12.sp)
                            Text("$${String.format(Locale.US, "%.2f", tarjetaTotal)}", color = Rosa, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Piezas Vendidas:", color = TextoSecundario, fontSize = 13.sp)
                            Text("$totalPiezas pzs", color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transacciones cobradas:", color = TextoSecundario, fontSize = 13.sp)
                            Text("$totalVentas", color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }

                        if (topVendidos.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 4.dp))
                            Text("🌟 Más vendidos:", color = Rosa, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            topVendidos.forEachIndexed { idx, top ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${idx + 1}. ${top.key}", color = TextoPrimario, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                                    Text("${top.value} pzs", color = MoradoClaro, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCerrar,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("Cerrar", color = TextoSecundario)
                    }

                    Button(
                        onClick = { onCompartir(generarTextoCierre()) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Compartir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
