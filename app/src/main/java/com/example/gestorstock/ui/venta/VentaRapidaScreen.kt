package com.example.gestorstock.ui.venta

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.theme.*
import com.example.gestorstock.ui.StockViewModel
import com.example.gestorstock.ui.inventario.parseColor
import com.example.gestorstock.ui.scanner.BarcodeScannerDialog
import java.util.Locale

data class VentaItemResumen(
    val articulo: Articulo,
    val cantidad: Int,
    val subtotal: Double
)

data class VentaResumen(
    val ventaId: Long = 0,
    val tienda: String,
    val logoUri: String? = null,
    val items: List<VentaItemResumen>,
    val totalArticulos: Int,
    val totalPrecio: Double,
    val pagoCon: Double?,
    val cambio: Double?,
    val metodoPago: String = "EFECTIVO",
    val descuento: Double = 0.0,
    val porcentajeDescuento: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaRapidaScreen(
    viewModel: StockViewModel,
    onVolver: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val articulos by viewModel.todosArticulos.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()
    val logoTiendaUri by viewModel.logoTiendaUri.collectAsState()

    // Carrito de compras: Map<ArticuloId, Cantidad>
    var carrito by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var busqueda by remember { mutableStateOf("") }
    var categoriaFiltro by remember { mutableStateOf<Int?>(null) }
    var metodoPago by remember { mutableStateOf("EFECTIVO") } // EFECTIVO, TRANSFERENCIA, TARJETA
    var pagaConTexto by remember { mutableStateOf("") }
    var porcentajeDescuento by remember { mutableStateOf<Int?>(null) }
    var ventaResumen by remember { mutableStateOf<VentaResumen?>(null) }
    var mostrarConfirmarLimpiar by remember { mutableStateOf(false) }
    var sheetCarritoAbierto by remember { mutableStateOf(false) }
    var mostrarEscanerVenta by remember { mutableStateOf(false) }
    var mensajeFeedback by remember { mutableStateOf<String?>(null) }

    // Cálculos del carrito
    val totalArticulos = carrito.values.sum()
    val subtotalBruto = carrito.entries.sumOf { (id, cant) ->
        val art = articulos.find { it.id == id }
        (art?.precio ?: 0.0) * cant
    }
    val montoDescuento = if (porcentajeDescuento != null && porcentajeDescuento!! > 0) {
        subtotalBruto * (porcentajeDescuento!! / 100.0)
    } else 0.0
    val totalPrecio = maxOf(0.0, subtotalBruto - montoDescuento)
    val pagaConDouble = pagaConTexto.toDoubleOrNull()
    val cambio = if (pagaConDouble != null && pagaConDouble >= totalPrecio) pagaConDouble - totalPrecio else 0.0

    // Filtro de artículos
    val articulosFiltrados = remember(articulos, busqueda, categoriaFiltro) {
        articulos.filter { art ->
            val matchCat = categoriaFiltro == null || art.categoriaId == categoriaFiltro
            val matchBusqueda = busqueda.isBlank() ||
                    art.nombre.contains(busqueda, ignoreCase = true) ||
                    (art.descripcion.contains(busqueda, ignoreCase = true)) ||
                    (art.codigoBarras?.contains(busqueda, ignoreCase = true) == true)
            matchCat && matchBusqueda
        }
    }

    // Funciones del carrito
    fun agregarAlCarrito(articulo: Articulo) {
        val actual = carrito[articulo.id] ?: 0
        if (actual < articulo.stock) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            carrito = carrito + (articulo.id to (actual + 1))
        }
    }

    fun restarDelCarrito(articuloId: Int) {
        val actual = carrito[articuloId] ?: return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (actual <= 1) {
            carrito = carrito - articuloId
        } else {
            carrito = carrito + (articuloId to (actual - 1))
        }
    }

    fun eliminarDelCarrito(articuloId: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        carrito = carrito - articuloId
    }

    fun limpiarCarrito() {
        carrito = emptyMap()
        pagaConTexto = ""
        porcentajeDescuento = null
    }

    fun procesarCodigoEscaneado(codigo: String) {
        val art = viewModel.buscarArticuloPorCodigo(codigo)
        if (art != null) {
            val actual = carrito[art.id] ?: 0
            if (actual < art.stock) {
                agregarAlCarrito(art)
                mensajeFeedback = "✨ Añadido: ${art.nombre}"
            } else {
                mensajeFeedback = "⚠️ Sin stock disponible para ${art.nombre}"
            }
        } else {
            mensajeFeedback = "❌ No se encontró producto con código $codigo"
        }
    }

    fun finalizarVenta() {
        if (carrito.isEmpty()) return

        // Preparamos resumen
        val itemsResumen = carrito.mapNotNull { (id, cant) ->
            articulos.find { it.id == id }?.let { art ->
                VentaItemResumen(
                    articulo = art,
                    cantidad = cant,
                    subtotal = art.precio * cant
                )
            }
        }

        val notasDescuento = if (montoDescuento > 0) {
            "Descuento ${porcentajeDescuento}% (-$${String.format(Locale.US, "%.2f", montoDescuento)} MXN)"
        } else ""

        val resumen = VentaResumen(
            tienda = nombreTienda,
            logoUri = logoTiendaUri,
            items = itemsResumen,
            totalArticulos = totalArticulos,
            totalPrecio = totalPrecio,
            pagoCon = if (metodoPago == "EFECTIVO") pagaConDouble else totalPrecio,
            cambio = if (metodoPago == "EFECTIVO" && pagaConDouble != null && pagaConDouble >= totalPrecio) cambio else null,
            metodoPago = metodoPago,
            descuento = montoDescuento,
            porcentajeDescuento = porcentajeDescuento
        )

        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // Registrar en Base de Datos de Ventas (descuenta stock)
        viewModel.registrarVentaCompleta(
            items = carrito,
            metodoPago = metodoPago,
            pagoCon = if (metodoPago == "EFECTIVO") pagaConDouble else totalPrecio,
            cambio = if (metodoPago == "EFECTIVO" && pagaConDouble != null && pagaConDouble >= totalPrecio) cambio else null,
            notas = notasDescuento
        ) { ventaId ->
            ventaResumen = resumen.copy(ventaId = ventaId)
            limpiarCarrito()
            sheetCarritoAbierto = false
        }
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
                                .clip(CircleShape)
                                .border(1.dp, Brush.linearGradient(listOf(Rosa, Morado)), CircleShape)
                                .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoTiendaUri != null) {
                                AsyncImage(
                                    model = logoTiendaUri,
                                    contentDescription = "Logo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Filled.Storefront, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Modo Tienda / Venta Rápida", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(
                                if (nombreTienda.isNotBlank()) nombreTienda else "Mi Tiendita",
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { mostrarEscanerVenta = true }) {
                        Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear producto", tint = Rosa)
                    }

                    if (totalArticulos > 0) {
                        // Badge con total en carrito
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Rosa.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Rosa.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.ShoppingCart, null, tint = Rosa, modifier = Modifier.size(16.dp))
                                Text(
                                    "$totalArticulos en carrito",
                                    color = TextoPrimario,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(onClick = { mostrarConfirmarLimpiar = true }) {
                            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Vaciar carrito", tint = Rojo)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Banner de Feedback del Escáner
            AnimatedVisibility(
                visible = mensajeFeedback != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                mensajeFeedback?.let { msg ->
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(3000)
                        mensajeFeedback = null
                    }
                    val esError = msg.startsWith("❌") || msg.startsWith("⚠️")
                    Surface(
                        color = if (esError) Rojo.copy(alpha = 0.95f) else Verde.copy(alpha = 0.95f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            IconButton(
                                onClick = { mensajeFeedback = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val esPantallaAncha = maxWidth >= 700.dp

                if (esPantallaAncha) {
                    // Layout Tablet Horizontal (2 Columnas: Catálogo | Carrito & Cobro)
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Columna Izquierda: Catálogo de Productos
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                        ) {
                            BarraBusquedaYFiltros(
                                busqueda = busqueda,
                                onBusquedaChange = { busqueda = it },
                                categoriaFiltro = categoriaFiltro,
                                onCategoriaFiltroChange = { categoriaFiltro = it },
                                categorias = categorias,
                                onAbrirEscaner = { mostrarEscanerVenta = true }
                            )

                            CatalogoGrid(
                                articulos = articulosFiltrados,
                                categorias = categorias,
                                carrito = carrito,
                                onAgregar = { agregarAlCarrito(it) },
                                onRestar = { restarDelCarrito(it) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Divisor vertical sutil
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(Color(0xFF2A1D40))
                        )

                        // Columna Derecha: Carrito & Cobro
                        Column(
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                                .background(FondoCard)
                        ) {
                            PanelCarrito(
                                carrito = carrito,
                                articulos = articulos,
                                categorias = categorias,
                                totalArticulos = totalArticulos,
                                subtotalBruto = subtotalBruto,
                                montoDescuento = montoDescuento,
                                porcentajeDescuento = porcentajeDescuento,
                                onPorcentajeDescuentoChange = { porcentajeDescuento = it },
                                totalPrecio = totalPrecio,
                                metodoPago = metodoPago,
                                onMetodoPagoChange = { metodoPago = it },
                                pagaConTexto = pagaConTexto,
                                onPagaConChange = { pagaConTexto = it },
                                cambio = cambio,
                                onAgregar = { artId ->
                                    articulos.find { it.id == artId }?.let { agregarAlCarrito(it) }
                                },
                                onRestar = { restarDelCarrito(it) },
                                onEliminar = { eliminarDelCarrito(it) },
                                onLimpiar = { mostrarConfirmarLimpiar = true },
                                onCobrar = { finalizarVenta() }
                            )
                        }
                    }
                } else {
                    // Layout Móvil / Pantalla Estrecha
                    Column(modifier = Modifier.fillMaxSize()) {
                        BarraBusquedaYFiltros(
                            busqueda = busqueda,
                            onBusquedaChange = { busqueda = it },
                            categoriaFiltro = categoriaFiltro,
                            onCategoriaFiltroChange = { categoriaFiltro = it },
                            categorias = categorias,
                            onAbrirEscaner = { mostrarEscanerVenta = true }
                        )

                        CatalogoGrid(
                            articulos = articulosFiltrados,
                            categorias = categorias,
                            carrito = carrito,
                            onAgregar = { agregarAlCarrito(it) },
                            onRestar = { restarDelCarrito(it) },
                            modifier = Modifier.weight(1f)
                        )

                        // Barra inferior flotante para abrir carrito
                        AnimatedVisibility(visible = totalArticulos > 0) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sheetCarritoAbierto = true },
                                color = Morado,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Badge(containerColor = Rosa) {
                                            Text("$totalArticulos", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        Text("Ver Carrito de Compra", color = Color.White, fontWeight = FontWeight.SemiBold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        if (montoDescuento > 0) {
                                            Text(
                                                "-$${String.format(Locale.US, "%.2f", montoDescuento)} Dcto",
                                                color = Verde,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", totalPrecio)} MXN",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Modal Bottom Sheet para Carrito en Móvil
                    if (sheetCarritoAbierto) {
                        ModalBottomSheet(
                            onDismissRequest = { sheetCarritoAbierto = false },
                            containerColor = FondoCard
                        ) {
                            PanelCarrito(
                                carrito = carrito,
                                articulos = articulos,
                                categorias = categorias,
                                totalArticulos = totalArticulos,
                                subtotalBruto = subtotalBruto,
                                montoDescuento = montoDescuento,
                                porcentajeDescuento = porcentajeDescuento,
                                onPorcentajeDescuentoChange = { porcentajeDescuento = it },
                                totalPrecio = totalPrecio,
                                metodoPago = metodoPago,
                                onMetodoPagoChange = { metodoPago = it },
                                pagaConTexto = pagaConTexto,
                                onPagaConChange = { pagaConTexto = it },
                                cambio = cambio,
                                onAgregar = { artId ->
                                    articulos.find { it.id == artId }?.let { agregarAlCarrito(it) }
                                },
                                onRestar = { restarDelCarrito(it) },
                                onEliminar = { eliminarDelCarrito(it) },
                                onLimpiar = { mostrarConfirmarLimpiar = true },
                                onCobrar = { finalizarVenta() }
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de Escáner de Código de Barras / QR para Venta en Modo Pistola Continuo
    if (mostrarEscanerVenta) {
        BarcodeScannerDialog(
            titulo = "Escanear al Carrito (Pistola TPV)",
            subtitulo = "Escanea productos sucesivamente para sumarlos",
            modoContinuoInicial = true,
            onCodigoDetectado = { codigo ->
                procesarCodigoEscaneado(codigo)
            },
            onCerrar = { mostrarEscanerVenta = false }
        )
    }

    // Diálogo de Confirmación para Vaciar Carrito
    if (mostrarConfirmarLimpiar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmarLimpiar = false },
            containerColor = FondoCardElevado,
            title = { Text("¿Vaciar carrito?", color = TextoPrimario, fontWeight = FontWeight.Bold) },
            text = { Text("Se quitarán todos los artículos agregados a la venta actual.", color = TextoSecundario) },
            confirmButton = {
                Button(
                    onClick = {
                        limpiarCarrito()
                        mostrarConfirmarLimpiar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo)
                ) {
                    Text("Vaciar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmarLimpiar = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }

    // Diálogo de Ticket de Venta / Resumen Final
    ventaResumen?.let { resumen ->
        TicketVentaDialog(
            resumen = resumen,
            onNuevaVenta = { ventaResumen = null },
            onVolverInventario = {
                ventaResumen = null
                onVolver()
            }
        )
    }
}

// -------------------------------------------------------------
// Componente: Barra de Búsqueda y Filtros
// -------------------------------------------------------------
@Composable
private fun BarraBusquedaYFiltros(
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    categoriaFiltro: Int?,
    onCategoriaFiltroChange: (Int?) -> Unit,
    categorias: List<Categoria>,
    onAbrirEscaner: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        // Buscador
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusquedaChange,
            placeholder = { Text("Buscar artículo o escanear...", color = TextoDeshabilitado, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextoSecundario, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Filled.Clear, null, tint = TextoSecundario, modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onAbrirEscaner) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear código de barras / QR",
                            tint = Rosa,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = FondoCard,
                unfocusedContainerColor = FondoCard,
                focusedBorderColor = MoradoClaro,
                unfocusedBorderColor = Color(0xFF332050),
                focusedTextColor = TextoPrimario,
                unfocusedTextColor = TextoPrimario
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        // Chips de categorías
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoriaFiltro == null,
                onClick = { onCategoriaFiltroChange(null) },
                label = { Text("Todos", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Rosa,
                    selectedLabelColor = Color.White,
                    containerColor = FondoCard,
                    labelColor = TextoSecundario
                )
            )
            categorias.forEach { cat ->
                val c = parseColor(cat.colorHex)
                FilterChip(
                    selected = categoriaFiltro == cat.id,
                    onClick = {
                        onCategoriaFiltroChange(if (categoriaFiltro == cat.id) null else cat.id)
                    },
                    label = { Text(cat.nombre, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = c.copy(alpha = 0.3f),
                        selectedLabelColor = c,
                        containerColor = FondoCard,
                        labelColor = TextoSecundario
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = categoriaFiltro == cat.id,
                        borderColor = if (categoriaFiltro == cat.id) c else Color.Transparent
                    )
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Catálogo Grid
// -------------------------------------------------------------
@Composable
private fun CatalogoGrid(
    articulos: List<Articulo>,
    categorias: List<Categoria>,
    carrito: Map<Int, Int>,
    onAgregar: (Articulo) -> Unit,
    onRestar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (articulos.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.SearchOff, null, tint = TextoDeshabilitado, modifier = Modifier.size(56.dp))
                Text("No hay artículos que coincidan", color = TextoSecundario, fontSize = 14.sp)
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = modifier
        ) {
            items(articulos, key = { it.id }) { articulo ->
                val categoria = categorias.find { it.id == articulo.categoriaId }
                val cantidadEnCarrito = carrito[articulo.id] ?: 0
                val stockDisponible = articulo.stock - cantidadEnCarrito

                VentaArticuloCard(
                    articulo = articulo,
                    categoria = categoria,
                    cantidadEnCarrito = cantidadEnCarrito,
                    stockDisponible = stockDisponible,
                    onAgregar = { onAgregar(articulo) },
                    onRestar = { onRestar(articulo.id) }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Tarjeta de Artículo para la Tienda
// -------------------------------------------------------------
@Composable
private fun VentaArticuloCard(
    articulo: Articulo,
    categoria: Categoria?,
    cantidadEnCarrito: Int,
    stockDisponible: Int,
    onAgregar: () -> Unit,
    onRestar: () -> Unit
) {
    val agotadoTotal = articulo.stock == 0
    val sinStockDisponible = stockDisponible <= 0
    val enCarrito = cantidadEnCarrito > 0
    val catColor = categoria?.let { parseColor(it.colorHex) } ?: Morado

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (enCarrito) {
                    Modifier.border(2.dp, Brush.linearGradient(listOf(Rosa, Morado)), RoundedCornerShape(14.dp))
                } else {
                    Modifier.border(1.dp, Color(0xFF2E1C48), RoundedCornerShape(14.dp))
                }
            )
            .clickable(enabled = !agotadoTotal && !sinStockDisponible) { onAgregar() },
        colors = CardDefaults.cardColors(
            containerColor = if (agotadoTotal) FondoCard.copy(alpha = 0.4f) else FondoCard
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(if (enCarrito) 4.dp else 1.dp)
    ) {
        Column {
            // Imagen con badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                if (articulo.fotoUri != null && !agotadoTotal) {
                    AsyncImage(
                        model = articulo.fotoUri,
                        contentDescription = articulo.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = if (agotadoTotal) 0.3f else 1f
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (agotadoTotal) Modifier.background(Color(0xFF161022))
                                else Modifier.background(Brush.linearGradient(listOf(catColor.copy(alpha = 0.35f), FondoCardElevado)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (agotadoTotal) {
                            Text("AGOTADO", color = Rojo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Outlined.Checkroom, null, tint = catColor.copy(alpha = 0.6f), modifier = Modifier.size(38.dp))
                        }
                    }
                }

                // Badge de Stock disponible
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(
                            when {
                                agotadoTotal -> Rojo
                                stockDisponible <= 0 -> Naranja
                                articulo.stock <= articulo.stockMinimo -> Naranja
                                else -> Verde
                            },
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (agotadoTotal) "0" else "Stock: ${articulo.stock}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Badge de cantidad en carrito sobre la imagen
                if (enCarrito) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Rosa, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "x$cantidadEnCarrito",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Info del producto
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    articulo.nombre,
                    fontWeight = FontWeight.SemiBold,
                    color = if (agotadoTotal) TextoDeshabilitado else TextoPrimario,
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
                        "$${String.format(Locale.US, "%.0f", articulo.precio)}",
                        color = if (agotadoTotal) TextoDeshabilitado else Rosa,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    categoria?.let {
                        Text(
                            it.nombre,
                            color = catColor,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Controles de Carrito
                if (enCarrito) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FondoCardElevado, RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onRestar,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = "Restar", tint = Rosa, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            "$cantidadEnCarrito",
                            color = TextoPrimario,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        IconButton(
                            onClick = onAgregar,
                            enabled = !sinStockDisponible,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "Sumar",
                                tint = if (sinStockDisponible) TextoDeshabilitado else Verde,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onAgregar,
                        enabled = !agotadoTotal && !sinStockDisponible,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Morado,
                            disabledContainerColor = FondoCardElevado
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Panel del Carrito de Compras & Cobro
// -------------------------------------------------------------
@Composable
private fun PanelCarrito(
    carrito: Map<Int, Int>,
    articulos: List<Articulo>,
    categorias: List<Categoria>,
    totalArticulos: Int,
    subtotalBruto: Double,
    montoDescuento: Double,
    porcentajeDescuento: Int?,
    onPorcentajeDescuentoChange: (Int?) -> Unit,
    totalPrecio: Double,
    metodoPago: String,
    onMetodoPagoChange: (String) -> Unit,
    pagaConTexto: String,
    onPagaConChange: (String) -> Unit,
    cambio: Double,
    onAgregar: (Int) -> Unit,
    onRestar: (Int) -> Unit,
    onEliminar: (Int) -> Unit,
    onLimpiar: () -> Unit,
    onCobrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Cabecera del Carrito
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, null, tint = Rosa, modifier = Modifier.size(22.dp))
                Text("Carrito de Venta", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextoPrimario)
            }
            if (totalArticulos > 0) {
                TextButton(
                    onClick = onLimpiar,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Outlined.Delete, null, tint = Rojo.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Vaciar", color = Rojo.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 8.dp))

        if (carrito.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(FondoCardElevado, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ProductionQuantityLimits, null, tint = TextoDeshabilitado, modifier = Modifier.size(32.dp))
                    }
                    Text("El carrito está vacío", color = TextoSecundario, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        "Toca artículos del catálogo o escanea su código para sumarlos",
                        color = TextoDeshabilitado,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            // Lista de artículos agregados
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(carrito.entries.toList(), key = { it.key }) { (articuloId, cantidad) ->
                    val articulo = articulos.find { it.id == articuloId }
                    if (articulo != null) {
                        val categoria = categorias.find { it.id == articulo.categoriaId }
                        ItemFilaCarrito(
                            articulo = articulo,
                            categoria = categoria,
                            cantidad = cantidad,
                            onSumar = { onAgregar(articuloId) },
                            onRestar = { onRestar(articuloId) },
                            onEliminar = { onEliminar(articuloId) }
                        )
                    }
                }
            }

            Divider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 6.dp))

            // Selector de Método de Pago
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Forma de pago:", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val metodos = listOf(
                        Triple("EFECTIVO", "Efectivo", Icons.Filled.AttachMoney),
                        Triple("TRANSFERENCIA", "Transf. / SPEI", Icons.Filled.SendToMobile),
                        Triple("TARJETA", "Tarjeta", Icons.Filled.CreditCard)
                    )
                    metodos.forEach { (clave, label, icon) ->
                        val seleccionado = metodoPago == clave
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMetodoPagoChange(clave) },
                            color = if (seleccionado) Rosa.copy(alpha = 0.25f) else FondoCardElevado,
                            border = BorderStroke(
                                1.dp,
                                if (seleccionado) Rosa else Color(0xFF3B255C)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = if (seleccionado) Rosa else TextoSecundario, modifier = Modifier.size(16.dp))
                                Text(
                                    label,
                                    color = if (seleccionado) TextoPrimario else TextoSecundario,
                                    fontSize = 10.sp,
                                    fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Sección de Pago en Efectivo y Calculadora de Cambio
            if (metodoPago == "EFECTIVO") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FondoCardElevado, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paga con:", color = TextoSecundario, fontSize = 13.sp)
                        OutlinedTextField(
                            value = pagaConTexto,
                            onValueChange = onPagaConChange,
                            placeholder = { Text("$ 0.00", color = TextoDeshabilitado, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FondoCard,
                                unfocusedContainerColor = FondoCard,
                                focusedBorderColor = Verde,
                                unfocusedBorderColor = Color(0xFF3B255C),
                                focusedTextColor = TextoPrimario,
                                unfocusedTextColor = TextoPrimario
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(130.dp)
                                .height(44.dp),
                            singleLine = true
                        )
                    }

                    // Botones de efectivo rápido ($100, $200, $500, Exacto)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val montosRapidos = listOf(100, 200, 500)
                        Button(
                            onClick = { onPagaConChange(String.format(Locale.US, "%.0f", totalPrecio)) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuperficieOscura),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                        ) {
                            Text("Exacto", fontSize = 10.sp, color = TextoPrimario)
                        }
                        montosRapidos.forEach { monto ->
                            Button(
                                onClick = { onPagaConChange("$monto") },
                                colors = ButtonDefaults.buttonColors(containerColor = SuperficieOscura),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(28.dp)
                            ) {
                                Text("$$monto", fontSize = 10.sp, color = TextoSecundario)
                            }
                        }
                    }

                    // Cálculo de cambio
                    if (pagaConTexto.isNotBlank()) {
                        val pagaDouble = pagaConTexto.toDoubleOrNull() ?: 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Cambio a devolver:", color = TextoSecundario, fontSize = 12.sp)
                            if (pagaDouble >= totalPrecio) {
                                Text(
                                    "$${String.format(Locale.US, "%.2f", cambio)} MXN",
                                    color = Verde,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    "Faltan $${String.format(Locale.US, "%.2f", totalPrecio - pagaDouble)}",
                                    color = Rojo,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Mensaje informativo para Transferencia o Tarjeta
                Surface(
                    color = Morado.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Verified, null, tint = MoradoClaro, modifier = Modifier.size(18.dp))
                        Text(
                            if (metodoPago == "TRANSFERENCIA") "Pago electrónico vía SPEI / Transferencia. Cobro exacto."
                            else "Pago con Tarjeta de Débito/Crédito. Cobro exacto.",
                            color = TextoPrimario,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Selector de Descuentos Rápidos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SuperficieOscura, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Percent, null, tint = if (montoDescuento > 0) Verde else TextoSecundario, modifier = Modifier.size(15.dp))
                    Text("Descuento:", color = TextoSecundario, fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(null to "0%", 5 to "5%", 10 to "10%", 15 to "15%", 20 to "20%").forEach { (pct, label) ->
                        val seleccionado = porcentajeDescuento == pct
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (seleccionado) Verde else FondoCard,
                            border = BorderStroke(1.dp, if (seleccionado) Verde else Color(0xFF3B255C)),
                            modifier = Modifier.clickable {
                                onPorcentajeDescuentoChange(if (seleccionado) null else pct)
                            }
                        ) {
                            Text(
                                label,
                                fontSize = 10.sp,
                                color = if (seleccionado) Color.White else TextoSecundario,
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Desglose si hay descuento aplicado
            if (montoDescuento > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:", color = TextoDeshabilitado, fontSize = 12.sp)
                    Text("$${String.format(Locale.US, "%.2f", subtotalBruto)}", color = TextoSecundario, fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Descuento (${porcentajeDescuento}%):", color = Verde, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("-$${String.format(Locale.US, "%.2f", montoDescuento)}", color = Verde, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(4.dp))
            }

            // Resumen de Totales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total a Pagar", color = TextoSecundario, fontSize = 13.sp)
                    Text("$totalArticulos piezas", color = TextoDeshabilitado, fontSize = 11.sp)
                }
                Text(
                    "$${String.format(Locale.US, "%.2f", totalPrecio)} MXN",
                    color = Rosa,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            // Botón Principal de Cobrar / Finalizar Venta
            Button(
                onClick = onCobrar,
                colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Cobrar $${String.format(Locale.US, "%.2f", totalPrecio)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Fila de Artículo en el Carrito
// -------------------------------------------------------------
@Composable
private fun ItemFilaCarrito(
    articulo: Articulo,
    categoria: Categoria?,
    cantidad: Int,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onEliminar: () -> Unit
) {
    val subtotal = articulo.precio * cantidad
    val catColor = categoria?.let { parseColor(it.colorHex) } ?: Morado

    Surface(
        color = FondoCardElevado,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Miniatura o icono
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(catColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                if (articulo.fotoUri != null) {
                    AsyncImage(
                        model = articulo.fotoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Outlined.Inventory2, null, tint = catColor, modifier = Modifier.size(18.dp))
                }
            }

            // Datos del producto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    articulo.nombre,
                    color = TextoPrimario,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$${String.format(Locale.US, "%.0f", articulo.precio)} c/u",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }

            // Controles de cantidad
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SuperficieOscura, RoundedCornerShape(6.dp))
                    .padding(horizontal = 2.dp)
            ) {
                IconButton(
                    onClick = onRestar,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.Remove, null, tint = Rosa, modifier = Modifier.size(14.dp))
                }
                Text(
                    "$cantidad",
                    color = TextoPrimario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                IconButton(
                    onClick = onSumar,
                    enabled = cantidad < articulo.stock,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        null,
                        tint = if (cantidad < articulo.stock) Verde else TextoDeshabilitado,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Subtotal
            Text(
                "$${String.format(Locale.US, "%.0f", subtotal)}",
                color = RosaClaro,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.width(50.dp),
                textAlign = TextAlign.End
            )

            // Quitar
            IconButton(
                onClick = onEliminar,
                modifier = Modifier.size(22.dp)
            ) {
                Icon(Icons.Outlined.Close, null, tint = TextoDeshabilitado, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Diálogo / Ticket de Venta Completada
// -------------------------------------------------------------
@Composable
private fun TicketVentaDialog(
    resumen: VentaResumen,
    onNuevaVenta: () -> Unit,
    onVolverInventario: () -> Unit
) {
    Dialog(
        onDismissRequest = onNuevaVenta,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 480.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Brush.linearGradient(listOf(Rosa, Morado)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icono de Éxito
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Brush.linearGradient(listOf(Verde, Color(0xFF2E7D32))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "¡Venta Registrada con Éxito!",
                    color = TextoPrimario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (resumen.logoUri != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(1.dp, Brush.linearGradient(listOf(Rosa, Morado)), CircleShape)
                        ) {
                            AsyncImage(
                                model = resumen.logoUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Text(
                        resumen.tienda.ifBlank { "Mi Tiendita" },
                        color = MoradoClaro,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Tabla resumen de lo comprado
                Surface(
                    color = FondoOscuro,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ARTÍCULO", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("SUBTOTAL", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 6.dp))

                        // Items
                        resumen.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.articulo.nombre,
                                        color = TextoPrimario,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${item.cantidad} x $${String.format(Locale.US, "%.0f", item.articulo.precio)}",
                                        color = TextoDeshabilitado,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    "$${String.format(Locale.US, "%.2f", item.subtotal)}",
                                    color = TextoPrimario,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF2E1C48), modifier = Modifier.padding(vertical = 8.dp))

                        // Descuento si aplica
                        if (resumen.descuento > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Descuento (${resumen.porcentajeDescuento ?: 0}%):",
                                    color = Verde,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "-$${String.format(Locale.US, "%.2f", resumen.descuento)} MXN",
                                    color = Verde,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL (${resumen.totalArticulos} pzs):",
                                color = TextoPrimario,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "$${String.format(Locale.US, "%.2f", resumen.totalPrecio)} MXN",
                                color = Rosa,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Método de Pago
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Método de pago:", color = TextoSecundario, fontSize = 12.sp)
                            val metodoLabel = when (resumen.metodoPago) {
                                "TRANSFERENCIA" -> "📲 Transferencia SPEI"
                                "TARJETA" -> "💳 Tarjeta"
                                else -> "💵 Efectivo"
                            }
                            Text(metodoLabel, color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        // Si pagó en efectivo y hay cambio
                        if (resumen.metodoPago == "EFECTIVO" && resumen.pagoCon != null) {
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Efectivo recibido:", color = TextoSecundario, fontSize = 12.sp)
                                Text("$${String.format(Locale.US, "%.2f", resumen.pagoCon)}", color = TextoPrimario, fontSize = 12.sp)
                            }
                            resumen.cambio?.let { cambioDevuelto ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cambio entregado:", color = Verde, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text(
                                        "$${String.format(Locale.US, "%.2f", cambioDevuelto)} MXN",
                                        color = Verde,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                val context = LocalContext.current

                // Botón Compartir por WhatsApp
                Button(
                    onClick = {
                        val texto = TicketShareHelper.generarTextoTicket(resumen)
                        TicketShareHelper.compartirTicketWhatsApp(context, texto)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Filled.Share, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar Ticket por WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(Modifier.height(8.dp))

                // Botón Compartir Texto General
                OutlinedButton(
                    onClick = {
                        val texto = TicketShareHelper.generarTextoTicket(resumen)
                        TicketShareHelper.compartirTicketTexto(context, texto)
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    border = BorderStroke(1.dp, Rosa.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Rosa, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Compartir Ticket (Otras Apps)", color = Rosa, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                // Botones de acción inferiores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onVolverInventario,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        border = BorderStroke(1.dp, TextoSecundario.copy(alpha = 0.5f))
                    ) {
                        Text("Inventario", color = TextoSecundario, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onNuevaVenta,
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nueva Venta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
