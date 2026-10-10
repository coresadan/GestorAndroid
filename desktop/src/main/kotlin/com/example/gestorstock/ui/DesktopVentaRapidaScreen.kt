package com.example.gestorstock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*
import java.awt.Toolkit
import java.util.Locale

@Composable
fun DesktopVentaRapidaScreen(
    viewModel: DesktopStockViewModel
) {
    val todosArticulos by viewModel.todosArticulos.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()

    // Carrito de compras: map de ArticuloId -> Cantidad
    var carrito by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var busquedaRapida by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf<Int?>(null) }
    var descuentoDirecto by remember { mutableStateOf(0.0) }

    // Modales
    var mostrarLectorBarras by remember { mutableStateOf(false) }
    var mostrarModalCobro by remember { mutableStateOf(false) }
    var resumenTicketFinal by remember { mutableStateOf<DesktopVentaResumen?>(null) }
    var mensajeCopiado by remember { mutableStateOf(false) }

    // Filtrar catálogo para la vista izquierda
    val articulosFiltrados = remember(todosArticulos, busquedaRapida, categoriaSeleccionada) {
        todosArticulos.filter { art ->
            val coincideCat = categoriaSeleccionada == null || art.categoriaId == categoriaSeleccionada
            val coincideTxt = busquedaRapida.isBlank() ||
                    art.nombre.contains(busquedaRapida, ignoreCase = true) ||
                    (art.codigoBarras?.contains(busquedaRapida, ignoreCase = true) == true)
            coincideCat && coincideTxt
        }
    }

    fun agregarAlCarrito(art: Articulo) {
        val cantActual = carrito[art.id] ?: 0
        carrito = carrito + (art.id to (cantActual + 1))
        try { Toolkit.getDefaultToolkit().beep() } catch (_: Exception) {}
    }

    fun decrementarDelCarrito(artId: Int) {
        val cantActual = carrito[artId] ?: 0
        if (cantActual > 1) {
            carrito = carrito + (artId to (cantActual - 1))
        } else {
            carrito = carrito - artId
        }
    }

    fun eliminarDelCarrito(artId: Int) {
        carrito = carrito - artId
    }

    // Cálculos del carrito
    val articulosEnCarrito = remember(carrito, todosArticulos) {
        carrito.mapNotNull { (id, cant) ->
            val art = todosArticulos.find { it.id == id }
            if (art != null) DesktopVentaItem(art, cant, art.precio * cant) else null
        }
    }

    val totalPiezas = remember(articulosEnCarrito) { articulosEnCarrito.sumOf { it.cantidad } }
    val subtotalBruto = remember(articulosEnCarrito) { articulosEnCarrito.sumOf { it.subtotal } }
    val totalNeto = remember(subtotalBruto, descuentoDirecto) { Math.max(0.0, subtotalBruto - descuentoDirecto) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PANEL IZQUIERDO: Catálogo y Búsqueda (65%)
        Column(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra de Búsqueda y Botón Pistola
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = busquedaRapida,
                    onValueChange = { busquedaRapida = it },
                    placeholder = { Text("Buscar producto por nombre o código...", color = TextoSecundario) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = RosaClaro) },
                    trailingIcon = {
                        if (busquedaRapida.isNotEmpty()) {
                            IconButton(onClick = { busquedaRapida = "" }) {
                                Icon(Icons.Default.Clear, null, tint = TextoSecundario)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Rosa,
                        unfocusedBorderColor = Color(0xFF3D2A54),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = TextoPrimario
                    )
                )

                Button(
                    onClick = { mostrarLectorBarras = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Morado),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pistola Lector USB")
                }
            }

            // Chips de Categorías
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = categoriaSeleccionada == null,
                        onClick = { categoriaSeleccionada = null },
                        label = { Text("Todas (${todosArticulos.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Rosa,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categorias, key = { it.id }) { cat ->
                    val cantidadArt = todosArticulos.count { it.categoriaId == cat.id }
                    FilterChip(
                        selected = categoriaSeleccionada == cat.id,
                        onClick = { categoriaSeleccionada = if (categoriaSeleccionada == cat.id) null else cat.id },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(parseHexColor(cat.colorHex), CircleShape))
                                Text("${cat.nombre} ($cantidadArt)")
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Morado,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // Cuadrícula de Productos
            Card(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCard)
            ) {
                if (articulosFiltrados.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SearchOff, null, tint = TextoSecundario, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No se encontraron productos coincidentes", color = TextoSecundario)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 170.dp),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(articulosFiltrados, key = { it.id }) { art ->
                            val enCarrito = carrito[art.id] ?: 0
                            val sinStock = art.stock <= 0

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { agregarAlCarrito(art) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
                                border = if (enCarrito > 0) androidx.compose.foundation.BorderStroke(2.dp, Rosa) else null
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ArticuloImage(fotoUri = art.fotoUri, modifier = Modifier.fillMaxSize())

                                        // Badge de cantidad en carrito
                                        if (enCarrito > 0) {
                                            Surface(
                                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                                shape = CircleShape,
                                                color = Rosa
                                            ) {
                                                Text(
                                                    "$enCarrito",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Badge de stock
                                        Surface(
                                            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (sinStock) Rojo.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                if (sinStock) "Sin stock" else "${art.stock} ${art.unidadMedida}",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        art.nombre,
                                        color = TextoPrimario,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", art.precio)}",
                                            color = RosaClaro,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )

                                        IconButton(
                                            onClick = { agregarAlCarrito(art) },
                                            modifier = Modifier.size(28.dp).background(Rosa, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PANEL DERECHO: Carrito y Cobro (35%)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Cabecera Carrito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ShoppingCart, null, tint = Rosa)
                        Text("Carrito de Venta", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }

                    if (carrito.isNotEmpty()) {
                        TextButton(onClick = { carrito = emptyMap(); descuentoDirecto = 0.0 }) {
                            Text("Vaciar", color = Rojo, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Lista de Artículos en el Carrito
                if (articulosEnCarrito.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingBasket, null, tint = TextoSecundario, modifier = Modifier.size(54.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("El carrito está vacío", color = TextoSecundario, fontWeight = FontWeight.Medium)
                            Text("Haz clic en los productos para agregarlos", color = TextoDeshabilitado, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(articulosEnCarrito, key = { it.articulo.id }) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = FondoCard)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ArticuloImage(fotoUri = item.articulo.fotoUri, modifier = Modifier.fillMaxSize())
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.articulo.nombre,
                                            color = TextoPrimario,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", item.articulo.precio)} c/u",
                                            color = TextoSecundario,
                                            fontSize = 11.sp
                                        )
                                    }

                                    // Controles de cantidad
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { decrementarDelCarrito(item.articulo.id) },
                                            modifier = Modifier.size(26.dp).background(FondoCardElevado, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            "${item.cantidad}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )

                                        IconButton(
                                            onClick = { agregarAlCarrito(item.articulo) },
                                            modifier = Modifier.size(26.dp).background(Rosa, CircleShape)
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }

                                    Text(
                                        "$${String.format(Locale.US, "%.2f", item.subtotal)}",
                                        color = RosaClaro,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.widthIn(min = 60.dp)
                                    )

                                    IconButton(
                                        onClick = { eliminarDelCarrito(item.articulo.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Rojo, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Totales y Cobro
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Piezas totales:", color = TextoSecundario, fontSize = 13.sp)
                            Text("$totalPiezas pzas", color = TextoPrimario, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", color = TextoSecundario, fontSize = 13.sp)
                            Text("$${String.format(Locale.US, "%.2f", subtotalBruto)}", color = TextoPrimario, fontWeight = FontWeight.SemiBold)
                        }

                        if (descuentoDirecto > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Descuento:", color = Verde, fontSize = 13.sp)
                                Text("-$${String.format(Locale.US, "%.2f", descuentoDirecto)}", color = Verde, fontWeight = FontWeight.Bold)
                            }
                        }

                        Divider(color = Color(0xFF3D2A54), modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "$${String.format(Locale.US, "%.2f", totalNeto)} MXN",
                                color = Rosa,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { mostrarModalCobro = true },
                    enabled = carrito.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Payment, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COBRAR ($${String.format(Locale.US, "%.2f", totalNeto)})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    // DIÁLOGO DE COBRO
    if (mostrarModalCobro) {
        var metodoPago by remember { mutableStateOf("Efectivo") }
        var recibidoStr by remember { mutableStateOf("") }
        var notasVenta by remember { mutableStateOf("") }

        val recibidoVal = recibidoStr.toDoubleOrNull() ?: totalNeto
        val cambioVal = if (recibidoVal >= totalNeto) recibidoVal - totalNeto else 0.0

        Dialog(onDismissRequest = { mostrarModalCobro = false }) {
            Card(
                modifier = Modifier.width(550.dp).padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💵 Cobrar Venta", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        IconButton(onClick = { mostrarModalCobro = false }) {
                            Icon(Icons.Default.Close, null, tint = TextoSecundario)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Total a pagar grande
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL A COBRAR", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "$${String.format(Locale.US, "%.2f", totalNeto)} MXN",
                                color = Rosa,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Métodos de pago
                    Text("Método de Pago:", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Efectivo", "Tarjeta", "Transferencia").forEach { metodo ->
                            val sel = (metodoPago.equals(metodo, ignoreCase = true))
                            Button(
                                onClick = { metodoPago = metodo },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (sel) Rosa else FondoCard)
                            ) {
                                Text(metodo)
                            }
                        }
                    }

                    // Si es efectivo: Billetes rápidos y cambio
                    if (metodoPago.equals("Efectivo", ignoreCase = true)) {
                        Spacer(Modifier.height(14.dp))
                        Text("Billetes Rápidos:", color = TextoSecundario, fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(totalNeto, 50.0, 100.0, 200.0, 500.0).forEach { monto ->
                                OutlinedButton(
                                    onClick = { recibidoStr = String.format(Locale.US, "%.0f", monto) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(if (monto == totalNeto) "Exacto" else "$${monto.toInt()}", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = recibidoStr,
                            onValueChange = { recibidoStr = it },
                            label = { Text("Efectivo Recibido ($)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().background(FondoOscuro, RoundedCornerShape(8.dp)).padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cambio a devolver:", color = TextoSecundario, fontWeight = FontWeight.Medium)
                            Text(
                                "$${String.format(Locale.US, "%.2f", cambioVal)} MXN",
                                color = Verde,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notasVenta,
                        onValueChange = { notasVenta = it },
                        label = { Text("Notas de venta (Opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val itemsVenta = carrito
                            val pagoCon = if (metodoPago.equals("Efectivo", true)) recibidoVal else null
                            val cambio = if (metodoPago.equals("Efectivo", true)) cambioVal else null

                            viewModel.registrarVentaCompleta(
                                items = itemsVenta,
                                metodoPago = metodoPago,
                                pagoCon = pagoCon,
                                cambio = cambio,
                                notas = notasVenta
                            ) { ventaId ->
                                // Generar ticket
                                val resumen = DesktopVentaResumen(
                                    tienda = nombreTienda,
                                    items = articulosEnCarrito,
                                    totalArticulos = totalPiezas,
                                    totalPrecio = totalNeto,
                                    metodoPago = metodoPago,
                                    pagoCon = pagoCon,
                                    cambio = cambio,
                                    descuento = descuentoDirecto
                                )
                                resumenTicketFinal = resumen
                                carrito = emptyMap()
                                descuentoDirecto = 0.0
                                mostrarModalCobro = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Verde)
                    ) {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("CONFIRMAR Y FINALIZAR VENTA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // DIÁLOGO DE TICKET COMPLETADO
    if (resumenTicketFinal != null) {
        val ticketTexto = remember(resumenTicketFinal) {
            DesktopTicketHelper.generarTextoTicket(resumenTicketFinal!!)
        }

        Dialog(onDismissRequest = { resumenTicketFinal = null }) {
            Card(
                modifier = Modifier.width(550.dp).padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Verde, modifier = Modifier.size(24.dp))
                            Text("¡Venta Realizada con Éxito!", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }

                        IconButton(onClick = { resumenTicketFinal = null }) {
                            Icon(Icons.Default.Close, null, tint = TextoSecundario)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Vista previa del ticket tipo papel
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

                    if (mensajeCopiado) {
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
                                mensajeCopiado = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copiar Ticket", color = Color.White)
                        }

                        Button(
                            onClick = {
                                DesktopTicketHelper.imprimirTicket(ticketTexto, "Ticket_${nombreTienda}")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Morado)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Imprimir")
                        }

                        Button(
                            onClick = { resumenTicketFinal = null; mensajeCopiado = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                        ) {
                            Text("Nueva Venta")
                        }
                    }
                }
            }
        }
    }

    // DIÁLOGO LECTOR DE CÓDIGOS DE BARRAS / PISTOLA USB
    if (mostrarLectorBarras) {
        DesktopBarcodeScannerDialog(
            modoContinuoInicial = true,
            onCodigoDetectado = { codigo ->
                val artEncontrado = viewModel.buscarArticuloPorCodigo(codigo)
                if (artEncontrado != null) {
                    agregarAlCarrito(artEncontrado)
                } else {
                    busquedaRapida = codigo
                }
            },
            onCerrar = { mostrarLectorBarras = false }
        )
    }
}
