package com.example.gestorstock.ui.articulo

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.VentaConDetalles
import com.example.gestorstock.theme.*
import com.example.gestorstock.ui.StockViewModel
import com.example.gestorstock.ui.categorias.COLORES_DISPONIBLES
import com.example.gestorstock.ui.inventario.parseColor
import com.example.gestorstock.ui.scanner.BarcodeScannerDialog
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val UNIDADES_DISPONIBLES = listOf("pza", "kg", "lt", "gr", "paq", "caja", "doc")

@Composable
fun EditarArticuloScreen(
    viewModel: StockViewModel,
    articuloId: Int,
    onVolver: () -> Unit
) {
    val categorias by viewModel.categorias.collectAsState()
    val todosArticulos by viewModel.todosArticulos.collectAsState()
    val ventas by viewModel.ventas.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()

    var articuloCargado by remember { mutableStateOf<Articulo?>(null) }
    var estaCargando by remember { mutableStateOf(true) }

    LaunchedEffect(articuloId) {
        if (articuloId == -1) {
            articuloCargado = Articulo(nombre = "", stock = 1, stockMinimo = 2, unidadMedida = "pza")
            estaCargando = false
        } else {
            val art = viewModel.getArticuloById(articuloId)
            articuloCargado = art ?: Articulo(nombre = "")
            estaCargando = false
        }
    }

    if (estaCargando || articuloCargado == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Morado.copy(alpha = 0.2f), FondoOscuro),
                        radius = 1000f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Rosa, strokeWidth = 3.dp)
        }
    } else {
        key(articuloCargado?.id) {
            EditarArticuloForm(
                articuloInicial = articuloCargado!!,
                categorias = categorias,
                todosArticulos = todosArticulos,
                ventas = ventas,
                nombreTienda = nombreTienda,
                viewModel = viewModel,
                esNuevoInicial = articuloId == -1,
                onVolver = onVolver
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarArticuloForm(
    articuloInicial: Articulo,
    categorias: List<Categoria>,
    todosArticulos: List<Articulo>,
    ventas: List<VentaConDetalles>,
    nombreTienda: String,
    viewModel: StockViewModel,
    esNuevoInicial: Boolean,
    onVolver: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    var esNuevo by remember { mutableStateOf(esNuevoInicial) }
    var articuloActualizado by remember { mutableStateOf(articuloInicial) }

    // Estados del formulario
    var nombre by remember { mutableStateOf(articuloInicial.nombre) }
    var descripcion by remember { mutableStateOf(articuloInicial.descripcion) }
    var precio by remember { mutableStateOf(if (articuloInicial.precio > 0) articuloInicial.precio.toString() else "") }
    var precioCosto by remember { mutableStateOf(if (articuloInicial.precioCosto > 0) articuloInicial.precioCosto.toString() else "") }
    var stock by remember { mutableStateOf(articuloInicial.stock.toString()) }
    var stockMinimo by remember { mutableStateOf(articuloInicial.stockMinimo.toString()) }
    var unidadMedida by remember { mutableStateOf(articuloInicial.unidadMedida) }
    var fechaCaducidad by remember { mutableStateOf(articuloInicial.fechaCaducidad) }
    var codigoBarras by remember { mutableStateOf(articuloInicial.codigoBarras ?: "") }
    var fotoUri by remember { mutableStateOf(articuloInicial.fotoUri) }
    var categoriaId by remember { mutableStateOf(articuloInicial.categoriaId) }
    var guardando by remember { mutableStateOf(false) }
    var mostrarErrores by remember { mutableStateOf(false) }

    // Diálogos y modales
    var mostrarPickerCategorias by remember { mutableStateOf(false) }
    var mostrarDialogoCrearCategoria by remember { mutableStateOf(false) }
    var mostrarEscanerCodigo by remember { mutableStateOf(false) }
    var mostrarDialogoDescartar by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarFotoCompleta by remember { mutableStateOf(false) }
    var mostrarSelectorOrigenFoto by remember { mutableStateOf(false) }
    var mostrarDialogoEtiqueta by remember { mutableStateOf(false) }

    // Detección de cambios sin guardar
    val hayCambiosSinGuardar = remember(
        nombre, descripcion, precio, precioCosto, stock, stockMinimo, unidadMedida, fechaCaducidad, codigoBarras, fotoUri, categoriaId, articuloActualizado, esNuevo
    ) {
        val pVenta = precio.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val pCosto = precioCosto.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val stockActual = stock.toIntOrNull() ?: 0
        val stockMinimoActual = stockMinimo.toIntOrNull() ?: 2
        val codigoActual = codigoBarras.trim().ifEmpty { null }

        nombre.trim() != articuloActualizado.nombre.trim() ||
                descripcion.trim() != articuloActualizado.descripcion.trim() ||
                pVenta != articuloActualizado.precio ||
                pCosto != articuloActualizado.precioCosto ||
                stockActual != articuloActualizado.stock ||
                stockMinimoActual != articuloActualizado.stockMinimo ||
                unidadMedida != articuloActualizado.unidadMedida ||
                fechaCaducidad != articuloActualizado.fechaCaducidad ||
                codigoActual != articuloActualizado.codigoBarras ||
                fotoUri != articuloActualizado.fotoUri ||
                categoriaId != articuloActualizado.categoriaId
    }

    BackHandler(enabled = hayCambiosSinGuardar) {
        mostrarDialogoDescartar = true
    }

    fun intentarVolver() {
        if (hayCambiosSinGuardar) {
            mostrarDialogoDescartar = true
        } else {
            onVolver()
        }
    }

    // Cámara y fotos
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun prepararUriCamara(): Uri {
        val tempFile = File(context.filesDir, "temp_camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
        tempCameraUri = uri
        return uri
    }

    val galeriaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val persistente = viewModel.guardarFotoArticulo(uri)
                fotoUri = persistente ?: uri.toString()
                snackbarHostState.showSnackbar("Foto cargada desde galería")
            }
        }
    }

    val camaraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { exito ->
        if (exito && tempCameraUri != null) {
            scope.launch {
                val persistente = viewModel.guardarFotoArticulo(tempCameraUri!!)
                fotoUri = persistente ?: tempCameraUri.toString()
                snackbarHostState.showSnackbar("Foto capturada con la cámara")
            }
        }
    }

    val categoriaActual = categorias.find { it.id == categoriaId }
    val catColorActual = categoriaActual?.let { parseColor(it.colorHex) } ?: MoradoClaro

    // Detección de código de barras duplicado
    val articuloDuplicado = remember(codigoBarras, articuloActualizado.id, todosArticulos) {
        val c = codigoBarras.trim()
        if (c.isEmpty()) null
        else todosArticulos.find { it.codigoBarras.equals(c, ignoreCase = true) && it.id != articuloActualizado.id }
    }

    // Estadísticas de ventas
    val estadisticas = remember(ventas, articuloActualizado.id) {
        if (esNuevo || articuloActualizado.id <= 0) null
        else {
            var totalCantidad = 0
            var totalRecaudado = 0.0
            var ultimaVenta: Long? = null
            ventas.forEach { v ->
                v.lineas.forEach { l ->
                    if (l.articuloId == articuloActualizado.id) {
                        totalCantidad += l.cantidad
                        totalRecaudado += l.subtotal
                        if (ultimaVenta == null || v.venta.fecha > ultimaVenta!!) {
                            ultimaVenta = v.venta.fecha
                        }
                    }
                }
            }
            Triple(totalCantidad, totalRecaudado, ultimaVenta)
        }
    }

    fun guardar() {
        val pNum = precio.trim().replace(',', '.').toDoubleOrNull()
        if (nombre.isBlank() || pNum == null || pNum < 0.0) {
            mostrarErrores = true
            scope.launch {
                if (nombre.isBlank()) {
                    snackbarHostState.showSnackbar("El nombre del artículo es obligatorio")
                } else {
                    snackbarHostState.showSnackbar("Ingresa un precio de venta válido (ej. 150.00)")
                }
            }
            return
        }

        guardando = true
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val pCostoNum = precioCosto.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        val articuloParaGuardar = articuloActualizado.copy(
            id = if (esNuevo) 0 else articuloActualizado.id,
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            precio = pNum,
            precioCosto = pCostoNum,
            stock = stock.toIntOrNull() ?: 0,
            stockMinimo = stockMinimo.toIntOrNull() ?: 2,
            unidadMedida = unidadMedida,
            fechaCaducidad = fechaCaducidad,
            codigoBarras = codigoBarras.trim().ifEmpty { null },
            fotoUri = fotoUri,
            categoriaId = categoriaId
        )

        scope.launch {
            viewModel.guardarArticulo(articuloParaGuardar) {
                onVolver()
            }
        }
    }

    fun duplicarArticuloComoNuevo() {
        val nuevoCodigo = viewModel.generarCodigoBarrasUnico()
        nombre = if (nombre.endsWith("(Copia)")) nombre else "$nombre (Copia)"
        codigoBarras = nuevoCodigo
        esNuevo = true
        articuloActualizado = articuloActualizado.copy(id = 0)
        scope.launch {
            snackbarHostState.showSnackbar("Artículo duplicado con código $nuevoCodigo. Listo para guardar.")
        }
    }

    // Animación de pulso cuando hay cambios sin guardar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Scaffold(
        containerColor = FondoOscuro,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = Color(0xF50D0718),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(
                            1.dp,
                            Brush.horizontalGradient(listOf(Rosa.copy(alpha = 0.45f), Morado.copy(alpha = 0.2f), Color.Transparent))
                        )
                    )
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TextoPrimario
                    ),
                    navigationIcon = {
                        IconButton(onClick = { intentarVolver() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextoSecundario)
                        }
                    },
                    title = {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (esNuevo) "Nuevo Artículo" else "Editar Artículo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextoPrimario
                                )
                                if (hayCambiosSinGuardar) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .scale(pulseGlow)
                                            .background(
                                                Brush.radialGradient(listOf(RosaClaro, Rosa)),
                                                CircleShape
                                            )
                                    )
                                }
                            }
                            AnimatedContent(
                                targetState = if (nombre.isNotBlank()) nombre else if (esNuevo) "Sin guardar" else articuloActualizado.nombre,
                                transitionSpec = { fadeIn(tween(250)).togetherWith(fadeOut(tween(150))) },
                                label = "title_sub"
                            ) { sub ->
                                Text(
                                    sub,
                                    color = TextoSecundario,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // Etiqueta imprimible
                        if (codigoBarras.isNotBlank()) {
                            IconButton(onClick = { mostrarDialogoEtiqueta = true }) {
                                Icon(Icons.Outlined.Print, contentDescription = "Etiqueta imprimible", tint = MoradoClaro)
                            }
                        }

                        // Duplicar
                        if (!esNuevo) {
                            IconButton(onClick = { duplicarArticuloComoNuevo() }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicar artículo", tint = MoradoClaro)
                            }
                        }

                        // Eliminar
                        if (!esNuevo) {
                            IconButton(onClick = { mostrarDialogoEliminar = true }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar artículo", tint = Rojo)
                            }
                        }

                        // Botón Guardar con resplandor degradado
                        Button(
                            onClick = { guardar() },
                            enabled = !guardando,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hayCambiosSinGuardar) Rosa else Rosa.copy(alpha = 0.85f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (hayCambiosSinGuardar) 4.dp else 1.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .then(
                                    if (hayCambiosSinGuardar) {
                                        Modifier.border(
                                            BorderStroke(1.dp, RosaClaro.copy(alpha = 0.7f)),
                                            RoundedCornerShape(12.dp)
                                        )
                                    } else Modifier
                                )
                        ) {
                            if (guardando) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                )
            }
        }
    ) { padding ->
        // Fondo con degradado ambiental OLED
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Morado.copy(alpha = 0.15f),
                            Color(0xFF0F081D),
                            FondoOscuro
                        ),
                        radius = 1200f
                    )
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
            ) {
                val anchoMax = maxWidth
                val altoMax = maxHeight
                val configuration = LocalConfiguration.current
                val esLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val esPantallaAncha = anchoMax >= 640.dp || (esLandscape && altoMax < 520.dp)

                if (esPantallaAncha) {
                    // Layout Horizontal / Tablet / Móvil apaisado
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Columna Izquierda: Foto, Categoría y Estadísticas
                        Column(
                            modifier = Modifier
                                .width(if (anchoMax >= 720.dp) 320.dp else 270.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SeccionFoto(
                                fotoUri = fotoUri,
                                catColor = catColorActual,
                                onAbrirOpcionesFoto = { mostrarSelectorOrigenFoto = true },
                                onVerFotoCompleta = { mostrarFotoCompleta = true },
                                onRotarFoto = {
                                    if (fotoUri != null) {
                                        scope.launch {
                                            val rotada = viewModel.rotarFotoArticulo(fotoUri!!)
                                            if (rotada != null) {
                                                fotoUri = rotada
                                                snackbarHostState.showSnackbar("Foto rotada 90°")
                                            }
                                        }
                                    }
                                },
                                onEliminarFoto = { fotoUri = null }
                            )

                            SeccionSelectorCategoria(
                                categoriaActual = categoriaActual,
                                categorias = categorias,
                                categoriaIdSeleccionada = categoriaId,
                                onSeleccionarCategoria = { categoriaId = it },
                                onAbrirDialogoCompleto = { mostrarPickerCategorias = true },
                                onCrearNuevaCategoria = { mostrarDialogoCrearCategoria = true }
                            )

                            if (estadisticas != null) {
                                TarjetaEstadisticasVenta(estadisticas = estadisticas)
                            }
                        }

                        // Columna Derecha: Campos del Formulario
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CamposFormulario(
                                nombre = nombre,
                                onNombreChange = { nombre = it },
                                descripcion = descripcion,
                                onDescripcionChange = { descripcion = it },
                                precio = precio,
                                onPrecioChange = { precio = it },
                                precioCosto = precioCosto,
                                onPrecioCostoChange = { precioCosto = it },
                                stock = stock,
                                onStockChange = { stock = it },
                                stockMinimo = stockMinimo,
                                onStockMinimoChange = { stockMinimo = it },
                                unidadMedida = unidadMedida,
                                onUnidadMedidaChange = { unidadMedida = it },
                                fechaCaducidad = fechaCaducidad,
                                onFechaCaducidadChange = { fechaCaducidad = it },
                                codigoBarras = codigoBarras,
                                onCodigoBarrasChange = { codigoBarras = it },
                                articuloDuplicado = articuloDuplicado,
                                onEscanearCodigo = { mostrarEscanerCodigo = true },
                                onGenerarCodigo = {
                                    val nuevo = viewModel.generarCodigoBarrasUnico()
                                    codigoBarras = nuevo
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Código asignado: $nuevo")
                                    }
                                },
                                onVerEtiqueta = { mostrarDialogoEtiqueta = true },
                                onDuplicarArticulo = { duplicarArticuloComoNuevo() },
                                esNuevo = esNuevo,
                                onEliminarArticulo = { mostrarDialogoEliminar = true },
                                mostrarErrores = mostrarErrores,
                                focusManager = focusManager,
                                context = context
                            )
                        }
                    }
                } else {
                    // Layout Móvil Vertical (Columna única fluida)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        SeccionFoto(
                            fotoUri = fotoUri,
                            catColor = catColorActual,
                            onAbrirOpcionesFoto = { mostrarSelectorOrigenFoto = true },
                            onVerFotoCompleta = { mostrarFotoCompleta = true },
                            onRotarFoto = {
                                if (fotoUri != null) {
                                    scope.launch {
                                        val rotada = viewModel.rotarFotoArticulo(fotoUri!!)
                                        if (rotada != null) {
                                            fotoUri = rotada
                                            snackbarHostState.showSnackbar("Foto rotada 90°")
                                        }
                                    }
                                }
                            },
                            onEliminarFoto = { fotoUri = null }
                        )

                        SeccionSelectorCategoria(
                            categoriaActual = categoriaActual,
                            categorias = categorias,
                            categoriaIdSeleccionada = categoriaId,
                            onSeleccionarCategoria = { categoriaId = it },
                            onAbrirDialogoCompleto = { mostrarPickerCategorias = true },
                            onCrearNuevaCategoria = { mostrarDialogoCrearCategoria = true }
                        )

                        CamposFormulario(
                            nombre = nombre,
                            onNombreChange = { nombre = it },
                            descripcion = descripcion,
                            onDescripcionChange = { descripcion = it },
                            precio = precio,
                            onPrecioChange = { precio = it },
                            precioCosto = precioCosto,
                            onPrecioCostoChange = { precioCosto = it },
                            stock = stock,
                            onStockChange = { stock = it },
                            stockMinimo = stockMinimo,
                            onStockMinimoChange = { stockMinimo = it },
                            unidadMedida = unidadMedida,
                            onUnidadMedidaChange = { unidadMedida = it },
                            fechaCaducidad = fechaCaducidad,
                            onFechaCaducidadChange = { fechaCaducidad = it },
                            codigoBarras = codigoBarras,
                            onCodigoBarrasChange = { codigoBarras = it },
                            articuloDuplicado = articuloDuplicado,
                            onEscanearCodigo = { mostrarEscanerCodigo = true },
                            onGenerarCodigo = {
                                val nuevo = viewModel.generarCodigoBarrasUnico()
                                codigoBarras = nuevo
                                scope.launch {
                                    snackbarHostState.showSnackbar("Código asignado: $nuevo")
                                }
                            },
                            onVerEtiqueta = { mostrarDialogoEtiqueta = true },
                            onDuplicarArticulo = { duplicarArticuloComoNuevo() },
                            esNuevo = esNuevo,
                            onEliminarArticulo = { mostrarDialogoEliminar = true },
                            mostrarErrores = mostrarErrores,
                            focusManager = focusManager,
                            context = context
                        )

                        if (estadisticas != null) {
                            TarjetaEstadisticasVenta(estadisticas = estadisticas)
                        }
                    }
                }
            }
        }
    }

    // Modal para Elegir Origen de Foto
    if (mostrarSelectorOrigenFoto) {
        Dialog(onDismissRequest = { mostrarSelectorOrigenFoto = false }) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
                border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Rosa, MoradoClaro, Color.Transparent))),
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Foto del Artículo", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Elige el origen de la imagen", color = TextoSecundario, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Cámara
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                mostrarSelectorOrigenFoto = false
                                val uri = prepararUriCamara()
                                camaraLauncher.launch(uri)
                            },
                        color = FondoCard,
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Rosa.copy(alpha = 0.5f), Color.Transparent)))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.size(38.dp).background(Rosa.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.PhotoCamera, null, tint = Rosa, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Hacer foto con la cámara", color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Captura el producto en vivo", color = TextoSecundario, fontSize = 11.sp)
                            }
                        }
                    }

                    // Galería
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                mostrarSelectorOrigenFoto = false
                                galeriaLauncher.launch("image/*")
                            },
                        color = FondoCard,
                        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(MoradoClaro.copy(alpha = 0.5f), Color.Transparent)))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.size(38.dp).background(MoradoClaro.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.PhotoLibrary, null, tint = MoradoClaro, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Elegir de la galería", color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Selecciona imagen guardada", color = TextoSecundario, fontSize = 11.sp)
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { mostrarSelectorOrigenFoto = false }) {
                            Text("Cancelar", color = TextoSecundario)
                        }
                    }
                }
            }
        }
    }

    // Modal para Ver Foto Completa
    if (mostrarFotoCompleta && fotoUri != null) {
        Dialog(onDismissRequest = { mostrarFotoCompleta = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(Rosa.copy(alpha = 0.6f), Morado))),
                modifier = Modifier.fillMaxWidth().padding(6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = fotoUri,
                            contentDescription = "Foto ampliada",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { mostrarFotoCompleta = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    // Modal: Etiqueta de Producto Imprimible
    if (mostrarDialogoEtiqueta) {
        DialogoEtiquetaImprimible(
            nombreTienda = nombreTienda,
            nombreArticulo = nombre.ifBlank { "Artículo" },
            categoria = categoriaActual?.nombre,
            codigo = codigoBarras.ifBlank { "SIN-CODIGO" },
            precio = precio.trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
            unidadMedida = unidadMedida,
            onCerrar = { mostrarDialogoEtiqueta = false }
        )
    }

    // Escáner de Código de Barras / QR
    if (mostrarEscanerCodigo) {
        BarcodeScannerDialog(
            titulo = "Escanear Código / QR",
            subtitulo = "Apunta al código para asignarlo al artículo",
            onCodigoDetectado = { codigo ->
                codigoBarras = codigo
                mostrarEscanerCodigo = false
                scope.launch {
                    snackbarHostState.showSnackbar("Código escaneado: $codigo")
                }
            },
            onCerrar = { mostrarEscanerCodigo = false }
        )
    }

    // Diálogo Selector de Categoría
    if (mostrarPickerCategorias) {
        DialogoPickerCategorias(
            categorias = categorias,
            categoriaIdSeleccionada = categoriaId,
            onSeleccionar = {
                categoriaId = it
                mostrarPickerCategorias = false
            },
            onCrearNueva = {
                mostrarPickerCategorias = false
                mostrarDialogoCrearCategoria = true
            },
            onCerrar = { mostrarPickerCategorias = false }
        )
    }

    // Diálogo Crear Categoría Rápida
    if (mostrarDialogoCrearCategoria) {
        DialogoNuevaCategoriaRapida(
            onGuardar = { nuevaCat ->
                viewModel.guardarCategoria(nuevaCat) { nuevoId ->
                    categoriaId = nuevoId.toInt()
                    mostrarDialogoCrearCategoria = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Categoría '${nuevaCat.nombre}' creada y asignada")
                    }
                }
            },
            onCerrar = { mostrarDialogoCrearCategoria = false }
        )
    }

    // Confirmación Descartar Cambios
    if (mostrarDialogoDescartar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoDescartar = false },
            containerColor = FondoCardElevado,
            shape = RoundedCornerShape(26.dp),
            icon = {
                Box(
                    modifier = Modifier.size(52.dp).background(Naranja.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WarningAmber, null, tint = Naranja, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("¿Descartar cambios?", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center) },
            text = { Text("Tienes modificaciones sin guardar. Si sales ahora, se perderán todos los cambios realizados.", color = TextoSecundario, fontSize = 13.sp, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoDescartar = false
                        onVolver()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Descartar y salir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoDescartar = false }) {
                    Text("Seguir editando", color = TextoSecundario)
                }
            }
        )
    }

    // Confirmación Eliminar
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            containerColor = FondoCardElevado,
            shape = RoundedCornerShape(26.dp),
            icon = {
                Box(
                    modifier = Modifier.size(52.dp).background(Rojo.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DeleteForever, null, tint = Rojo, modifier = Modifier.size(28.dp))
                }
            },
            title = { Text("¿Eliminar artículo?", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center) },
            text = { Text("¿Estás seguro de que deseas eliminar permanentemente \"${articuloActualizado.nombre}\"? Se borrará del catálogo y su historial.", color = TextoSecundario, fontSize = 13.sp, textAlign = TextAlign.Center) },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoEliminar = false
                        scope.launch {
                            viewModel.eliminarArticulo(articuloActualizado)
                            onVolver()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Eliminar definitivamente", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// Componente: Sección de Foto con Efecto Estudio
// -------------------------------------------------------------
@Composable
private fun SeccionFoto(
    fotoUri: String?,
    catColor: Color,
    onAbrirOpcionesFoto: () -> Unit,
    onVerFotoCompleta: () -> Unit,
    onRotarFoto: () -> Unit,
    onEliminarFoto: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    catColor.copy(alpha = 0.6f),
                    Rosa.copy(alpha = 0.3f),
                    Color.Transparent
                )
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .then(
                        if (fotoUri != null) Modifier.background(Color.Black)
                        else Modifier.background(
                            Brush.verticalGradient(
                                listOf(
                                    catColor.copy(alpha = 0.22f),
                                    FondoCardElevado
                                )
                            )
                        )
                    )
                    .clickable {
                        if (fotoUri != null) onVerFotoCompleta() else onAbrirOpcionesFoto()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (fotoUri != null) {
                    AsyncImage(
                        model = fotoUri,
                        contentDescription = "Foto del artículo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Badge flotante para ampliar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color(0x99000000), RoundedCornerShape(10.dp))
                            .border(0.5.dp, Color(0x44FFFFFF), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Filled.ZoomIn, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Text("Ampliar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    Brush.radialGradient(listOf(Rosa.copy(alpha = 0.35f), Color.Transparent)),
                                    CircleShape
                                )
                                .border(1.dp, Rosa.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Rosa, modifier = Modifier.size(24.dp))
                        }
                        Text("Toca para añadir foto", color = TextoPrimario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Cámara o Galería", color = TextoSecundario, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onAbrirOpcionesFoto,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, MoradoClaro.copy(alpha = 0.6f))
                ) {
                    Icon(
                        if (fotoUri != null) Icons.Filled.Edit else Icons.Filled.AddAPhoto,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MoradoClaro
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (fotoUri != null) "Cambiar" else "Añadir Foto", fontSize = 11.sp, color = MoradoClaro, fontWeight = FontWeight.SemiBold)
                }

                if (fotoUri != null) {
                    var rotando by remember { mutableStateOf(false) }
                    val rotacionAnimada by animateFloatAsState(
                        targetValue = if (rotando) 90f else 0f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        finishedListener = { rotando = false },
                        label = "rot"
                    )

                    // Rotar 90° con micro-animación
                    IconButton(
                        onClick = {
                            rotando = true
                            onRotarFoto()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.RotateRight,
                            contentDescription = "Rotar 90°",
                            tint = MoradoClaro,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotacionAnimada)
                        )
                    }

                    // Eliminar
                    IconButton(
                        onClick = onEliminarFoto,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Quitar foto", tint = Rojo.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Selector de Categoría con Aura Ambiental
// -------------------------------------------------------------
@Composable
private fun SeccionSelectorCategoria(
    categoriaActual: Categoria?,
    categorias: List<Categoria>,
    categoriaIdSeleccionada: Int?,
    onSeleccionarCategoria: (Int?) -> Unit,
    onAbrirDialogoCompleto: () -> Unit,
    onCrearNuevaCategoria: () -> Unit
) {
    val catColor = categoriaActual?.let { parseColor(it.colorHex) } ?: TextoDeshabilitado
    val animatedBorderColor by animateColorAsState(targetValue = catColor, animationSpec = tween(350), label = "cat_color")

    Card(
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    animatedBorderColor.copy(alpha = 0.5f),
                    Color(0xFF261840),
                    Color.Transparent
                )
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Category, null, tint = Rosa, modifier = Modifier.size(18.dp))
                    Text("Categoría", fontWeight = FontWeight.Bold, color = TextoPrimario, fontSize = 14.sp)
                }
                TextButton(
                    onClick = onCrearNuevaCategoria,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Filled.Add, null, tint = Rosa, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Nueva", color = Rosa, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onAbrirDialogoCompleto() },
                color = FondoCardElevado,
                border = BorderStroke(1.dp, animatedBorderColor.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(13.dp).background(animatedBorderColor, CircleShape))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            categoriaActual?.nombre ?: "Sin categoría",
                            color = if (categoriaActual != null) TextoPrimario else TextoSecundario,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text("Toca para selector completo", color = TextoDeshabilitado, fontSize = 10.sp)
                    }
                    Icon(Icons.Filled.ArrowDropDown, null, tint = TextoSecundario, modifier = Modifier.size(18.dp))
                }
            }

            Text("Selección rápida:", color = TextoSecundario, fontSize = 11.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = categoriaIdSeleccionada == null,
                    onClick = { onSeleccionarCategoria(null) },
                    label = { Text("Ninguna", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF4A3B69),
                        selectedLabelColor = Color.White,
                        containerColor = SuperficieOscura,
                        labelColor = TextoSecundario
                    )
                )

                categorias.forEach { cat ->
                    val color = parseColor(cat.colorHex)
                    val seleccionada = categoriaIdSeleccionada == cat.id
                    FilterChip(
                        selected = seleccionada,
                        onClick = { onSeleccionarCategoria(cat.id) },
                        label = { Text(cat.nombre, fontSize = 11.sp) },
                        leadingIcon = { Box(modifier = Modifier.size(8.dp).background(color, CircleShape)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.35f),
                            selectedLabelColor = color,
                            containerColor = SuperficieOscura,
                            labelColor = TextoSecundario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionada,
                            borderColor = if (seleccionada) color else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Tarjeta de Estadísticas de Ventas
// -------------------------------------------------------------
@Composable
private fun TarjetaEstadisticasVenta(estadisticas: Triple<Int, Double, Long?>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Verde.copy(alpha = 0.4f), Color.Transparent))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.AutoMirrored.Outlined.TrendingUp, null, tint = Verde, modifier = Modifier.size(18.dp))
                Text("Historial de Ventas", fontWeight = FontWeight.Bold, color = TextoPrimario, fontSize = 13.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Piezas vendidas", color = TextoSecundario, fontSize = 11.sp)
                    Text("${estadisticas.first} unidades", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total generado", color = TextoSecundario, fontSize = 11.sp)
                    Text("$${String.format(Locale.US, "%.2f", estadisticas.second)} MXN", color = Verde, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            if (estadisticas.third != null) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaStr = sdf.format(Date(estadisticas.third!!))
                Text("Última venta: $fechaStr", color = TextoDeshabilitado, fontSize = 10.sp)
            } else {
                Text("Sin ventas registradas todavía", color = TextoDeshabilitado, fontSize = 10.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Campos del Formulario con Micro-Animaciones
// -------------------------------------------------------------
@Composable
private fun CamposFormulario(
    nombre: String,
    onNombreChange: (String) -> Unit,
    descripcion: String,
    onDescripcionChange: (String) -> Unit,
    precio: String,
    onPrecioChange: (String) -> Unit,
    precioCosto: String,
    onPrecioCostoChange: (String) -> Unit,
    stock: String,
    onStockChange: (String) -> Unit,
    stockMinimo: String,
    onStockMinimoChange: (String) -> Unit,
    unidadMedida: String,
    onUnidadMedidaChange: (String) -> Unit,
    fechaCaducidad: Long?,
    onFechaCaducidadChange: (Long?) -> Unit,
    codigoBarras: String,
    onCodigoBarrasChange: (String) -> Unit,
    articuloDuplicado: Articulo?,
    onEscanearCodigo: () -> Unit,
    onGenerarCodigo: () -> Unit,
    onVerEtiqueta: () -> Unit,
    onDuplicarArticulo: () -> Unit,
    esNuevo: Boolean,
    onEliminarArticulo: () -> Unit,
    mostrarErrores: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager,
    context: Context
) {
    val haptic = LocalHapticFeedback.current
    Card(
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(MoradoClaro.copy(alpha = 0.3f), Color.Transparent))),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Detalles del Producto", fontWeight = FontWeight.Bold, color = TextoPrimario, fontSize = 15.sp)

            // 1. Nombre
            CampoTextoPersonalizado(
                value = nombre,
                onValueChange = onNombreChange,
                label = "Nombre del artículo *",
                leadingIcon = Icons.AutoMirrored.Outlined.Label,
                isError = mostrarErrores && nombre.isBlank(),
                errorMsg = "El nombre es obligatorio",
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) }
            )

            // 2. Código de Barras / QR con Escáner y Generador
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = codigoBarras,
                    onValueChange = onCodigoBarrasChange,
                    label = { Text("Código de Barras / QR (opcional)") },
                    placeholder = { Text("Ej. 7501234567890 o ART-10293", color = TextoDeshabilitado) },
                    leadingIcon = { Icon(Icons.Outlined.QrCode, null, tint = TextoSecundario) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (codigoBarras.isNotEmpty()) {
                                IconButton(onClick = { onCodigoBarrasChange("") }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Limpiar código", tint = TextoSecundario)
                                }
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onGenerarCodigo()
                            }) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = "Generar código interno", tint = MoradoClaro)
                            }
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onEscanearCodigo()
                            }) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = "Escanear con cámara", tint = Rosa)
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Rosa,
                        unfocusedBorderColor = Color(0xFF3D2B55),
                        focusedContainerColor = SuperficieOscura,
                        unfocusedContainerColor = SuperficieOscura,
                        focusedTextColor = TextoPrimario,
                        unfocusedTextColor = TextoPrimario,
                        focusedLabelColor = Rosa,
                        unfocusedLabelColor = TextoSecundario
                    )
                )

                // Aviso de Código Duplicado con animación
                AnimatedVisibility(
                    visible = articuloDuplicado != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (articuloDuplicado != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Naranja.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.dp, Naranja.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, null, tint = Naranja, modifier = Modifier.size(16.dp))
                            Text(
                                "Asignado a: \"${articuloDuplicado.nombre}\" (Stock: ${articuloDuplicado.stock})",
                                color = Naranja,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (codigoBarras.isNotBlank() && articuloDuplicado == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onVerEtiqueta,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Outlined.Print, null, tint = MoradoClaro, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Imprimir / Ver Etiqueta", color = MoradoClaro, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Precios: Venta y Costo (con cálculo de margen reactivo y animado)
            val pVenta = precio.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
            val pCosto = precioCosto.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
            val ganancia = pVenta - pCosto
            val margenPct = if (pVenta > 0) (ganancia / pVenta) * 100 else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CampoTextoPersonalizado(
                    value = precio,
                    onValueChange = onPrecioChange,
                    label = "Precio venta (MXN) *",
                    leadingIcon = Icons.Outlined.AttachMoney,
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    isError = mostrarErrores && (pVenta <= 0.0 && precio.toDoubleOrNull() == null),
                    errorMsg = "Obligatorio",
                    modifier = Modifier.weight(1f)
                )

                CampoTextoPersonalizado(
                    value = precioCosto,
                    onValueChange = onPrecioCostoChange,
                    label = "Costo compra (MXN)",
                    leadingIcon = Icons.Outlined.ShoppingBag,
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Tarjeta de Rentabilidad y Margen Animada
            if (pVenta > 0) {
                val targetColorMargen = when {
                    ganancia < 0 -> Rojo
                    margenPct < 15 -> Naranja
                    else -> Verde
                }
                val colorMargenAnimado by animateColorAsState(targetValue = targetColorMargen, animationSpec = tween(400), label = "margin_color")

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorMargenAnimado.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, colorMargenAnimado.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (ganancia >= 0) Icons.Outlined.MonetizationOn else Icons.AutoMirrored.Outlined.TrendingDown,
                                null,
                                tint = colorMargenAnimado,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                if (ganancia >= 0) "Ganancia: +$${String.format(Locale.US, "%.2f", ganancia)}"
                                else "Pérdida: -$${String.format(Locale.US, "%.2f", -ganancia)}",
                                color = colorMargenAnimado,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            "Margen: ${String.format(Locale.US, "%.1f", margenPct)}%",
                            color = colorMargenAnimado,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 4. Unidad de Medida
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Unidad de medida:", color = TextoSecundario, fontSize = 11.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UNIDADES_DISPONIBLES.forEach { u ->
                        val sel = u.equals(unidadMedida, ignoreCase = true)
                        FilterChip(
                            selected = sel,
                            onClick = { onUnidadMedidaChange(u) },
                            label = { Text(u.uppercase(), fontSize = 11.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MoradoClaro.copy(alpha = 0.4f),
                                selectedLabelColor = MoradoClaro,
                                containerColor = SuperficieOscura,
                                labelColor = TextoSecundario
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = sel,
                                borderColor = if (sel) MoradoClaro else Color.Transparent
                            )
                        )
                    }
                }
            }

            // 5. Stock actual con Steppers Táctiles y Contador Animado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Stock con Stepper Elástico
                CampoTextoPersonalizado(
                    value = stock,
                    onValueChange = { nuevo ->
                        if (nuevo.isEmpty() || nuevo.all { it.isDigit() }) onStockChange(nuevo)
                    },
                    label = "Stock (${unidadMedida.lowercase()})",
                    leadingIcon = Icons.Outlined.Inventory,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var pulsandoMenos by remember { mutableStateOf(false) }
                            val escalaMenos by animateFloatAsState(if (pulsandoMenos) 0.82f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "less_scale")
                            IconButton(
                                onClick = {
                                    pulsandoMenos = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val act = stock.toIntOrNull() ?: 0
                                    if (act > 0) onStockChange((act - 1).toString())
                                    pulsandoMenos = false
                                },
                                modifier = Modifier.size(30.dp).scale(escalaMenos)
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Restar 1", tint = Rosa, modifier = Modifier.size(16.dp))
                            }

                            var pulsandoMas by remember { mutableStateOf(false) }
                            val escalaMas by animateFloatAsState(if (pulsandoMas) 0.82f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "more_scale")
                            IconButton(
                                onClick = {
                                    pulsandoMas = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val act = stock.toIntOrNull() ?: 0
                                    onStockChange((act + 1).toString())
                                    pulsandoMas = false
                                },
                                modifier = Modifier.size(30.dp).scale(escalaMas)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Sumar 1", tint = Rosa, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1.3f)
                )

                // Stock Mínimo
                CampoTextoPersonalizado(
                    value = stockMinimo,
                    onValueChange = { nuevo ->
                        if (nuevo.isEmpty() || nuevo.all { it.isDigit() }) onStockMinimoChange(nuevo)
                    },
                    label = "Aviso mín (${unidadMedida.lowercase()})",
                    leadingIcon = Icons.Outlined.Warning,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Indicador de Estado del Stock con Micro-Animación
            val stockNum = stock.toIntOrNull() ?: 0
            val minNum = stockMinimo.toIntOrNull() ?: 2
            val targetColorStock = when {
                stockNum == 0 -> Rojo
                stockNum <= minNum -> Naranja
                else -> Verde
            }
            val colorStockAnimado by animateColorAsState(targetValue = targetColorStock, animationSpec = tween(350), label = "stock_color")

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorStockAnimado.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, colorStockAnimado.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when {
                            stockNum == 0 -> Icons.Filled.ErrorOutline
                            stockNum <= minNum -> Icons.Filled.Warning
                            else -> Icons.Filled.CheckCircle
                        },
                        null,
                        tint = colorStockAnimado,
                        modifier = Modifier.size(18.dp)
                    )
                    AnimatedContent(
                        targetState = when {
                            stockNum == 0 -> "Agotado (0 unidades)"
                            stockNum <= minNum -> "Stock bajo ($stockNum unidades disponibles)"
                            else -> "Stock suficiente ($stockNum unidades disponibles)"
                        },
                        transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                        label = "stock_text"
                    ) { textoStock ->
                        Text(
                            textoStock,
                            color = colorStockAnimado,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Chips de Ajuste Rápido de Stock
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ajuste rápido:", color = TextoSecundario, fontSize = 11.sp)
                listOf(1, 5, 10, 20).forEach { inc ->
                    SuggestionChip(
                        onClick = {
                            val actual = stock.toIntOrNull() ?: 0
                            onStockChange((actual + inc).toString())
                        },
                        label = { Text("+$inc", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SuperficieOscura, labelColor = Rosa),
                        border = BorderStroke(1.dp, Rosa.copy(alpha = 0.35f))
                    )
                }
                SuggestionChip(
                    onClick = { onStockChange("0") },
                    label = { Text("0 (Agotar)", fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = SuperficieOscura, labelColor = Rojo),
                    border = BorderStroke(1.dp, Rojo.copy(alpha = 0.35f))
                )
            }

            // 6. Fecha de Caducidad / Vencimiento
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fecha de caducidad (opcional):", color = TextoSecundario, fontSize = 11.sp)
                    if (fechaCaducidad != null) {
                        TextButton(
                            onClick = { onFechaCaducidadChange(null) },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Quitar fecha", color = Rojo, fontSize = 10.sp)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            val cal = Calendar.getInstance()
                            if (fechaCaducidad != null) cal.timeInMillis = fechaCaducidad
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val sel = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                    }
                                    onFechaCaducidadChange(sel.timeInMillis)
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                    color = FondoCardElevado,
                    border = BorderStroke(1.dp, Color(0xFF3D2B55))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Outlined.Event, null, tint = Rosa, modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            if (fechaCaducidad != null) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val hoy = System.currentTimeMillis()
                                val diasRestantes = ((fechaCaducidad - hoy) / (1000 * 60 * 60 * 24)).toInt()

                                val colorEstado = when {
                                    diasRestantes < 0 -> Rojo
                                    diasRestantes <= 15 -> Naranja
                                    else -> Verde
                                }
                                val estadoTexto = when {
                                    diasRestantes < 0 -> "⛔ Caducado hace ${-diasRestantes} días (${sdf.format(Date(fechaCaducidad))})"
                                    diasRestantes == 0 -> "⚠️ Caduca hoy (${sdf.format(Date(fechaCaducidad))})"
                                    diasRestantes <= 15 -> "⚠️ Caduca en $diasRestantes días (${sdf.format(Date(fechaCaducidad))})"
                                    else -> "✅ Vence el ${sdf.format(Date(fechaCaducidad))} (en $diasRestantes días)"
                                }
                                Text(estadoTexto, color = colorEstado, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            } else {
                                Text("Sin fecha de caducidad asignada", color = TextoDeshabilitado, fontSize = 12.sp)
                                Text("Toca para seleccionar fecha", color = TextoSecundario, fontSize = 10.sp)
                            }
                        }
                        Icon(Icons.Filled.EditCalendar, null, tint = TextoSecundario, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 7. Descripción o Notas
            CampoTextoPersonalizado(
                value = descripcion,
                onValueChange = onDescripcionChange,
                label = "Descripción o notas (opcional)",
                leadingIcon = Icons.Outlined.Description,
                singleLine = false,
                minLines = 3,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() }
            )

            // 8. Botones de Acción
            if (!esNuevo) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDuplicarArticulo,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MoradoClaro.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = MoradoClaro, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Duplicar", color = MoradoClaro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onEliminarArticulo,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Rojo.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo)
                    ) {
                        Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = Rojo, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Eliminar", color = Rojo, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Modal de Etiqueta Imprimible con Código de Barras
// -------------------------------------------------------------
@Composable
private fun DialogoEtiquetaImprimible(
    nombreTienda: String,
    nombreArticulo: String,
    categoria: String?,
    codigo: String,
    precio: Double,
    unidadMedida: String,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val etiquetaBitmap = remember(nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida) {
        BarcodeLabelHelper.generarEtiquetaCompleta(
            nombreTienda = nombreTienda,
            nombreArticulo = nombreArticulo,
            categoria = categoria,
            codigo = codigo,
            precio = precio,
            unidadMedida = unidadMedida
        )
    }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
            border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Rosa, Morado))),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Outlined.Print, null, tint = Rosa, modifier = Modifier.size(20.dp))
                        Text("Etiqueta para Anaquel / Producto", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Filled.Close, null, tint = TextoSecundario)
                    }
                }

                // Vista previa de la etiqueta
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = etiquetaBitmap.asImageBitmap(),
                        contentDescription = "Etiqueta comercial",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text("Lista para imprimir en papel adhesivo, ticket térmico o compartir por WhatsApp.", color = TextoSecundario, fontSize = 11.sp, textAlign = TextAlign.Center)

                // Botones Imprimir y Compartir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { BarcodeLabelHelper.compartirEtiqueta(context, etiquetaBitmap, nombreArticulo) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MoradoClaro.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Share, null, tint = MoradoClaro, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir", color = MoradoClaro, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { BarcodeLabelHelper.imprimirEtiqueta(context, etiquetaBitmap, nombreArticulo) },
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                    ) {
                        Icon(Icons.Filled.Print, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Imprimir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Campo de Texto Estilizado con Foco Luminoso
// -------------------------------------------------------------
@Composable
private fun CampoTextoPersonalizado(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    errorMsg: String = "",
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var tieneFoco by remember { mutableStateOf(false) }
    val escalaIcono by animateFloatAsState(
        targetValue = if (tieneFoco) 1.15f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "icon_scale"
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            leadingIcon = {
                Icon(
                    leadingIcon,
                    null,
                    tint = if (isError) Rojo else if (tieneFoco) Rosa else TextoSecundario,
                    modifier = Modifier.size(18.dp).scale(escalaIcono)
                )
            },
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() }
            ),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { tieneFoco = it.isFocused },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Rosa,
                unfocusedBorderColor = Color(0xFF3D2B55),
                errorBorderColor = Rojo,
                focusedContainerColor = SuperficieOscura,
                unfocusedContainerColor = FondoCardElevado,
                errorContainerColor = Rojo.copy(alpha = 0.05f),
                focusedTextColor = TextoPrimario,
                unfocusedTextColor = TextoPrimario,
                focusedLabelColor = Rosa,
                unfocusedLabelColor = TextoSecundario,
                errorLabelColor = Rojo
            )
        )
        if (isError && errorMsg.isNotEmpty()) {
            Text(errorMsg, color = Rojo, fontSize = 10.sp, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
        }
    }
}

// -------------------------------------------------------------
// Componente: Diálogo Selector Completo de Categorías
// -------------------------------------------------------------
@Composable
private fun DialogoPickerCategorias(
    categorias: List<Categoria>,
    categoriaIdSeleccionada: Int?,
    onSeleccionar: (Int?) -> Unit,
    onCrearNueva: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = FondoCardElevado,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Category, null, tint = Rosa, modifier = Modifier.size(22.dp))
                Text("Seleccionar Categoría", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilaOpcionCategoria(
                    nombre = "Sin categoría",
                    color = TextoDeshabilitado,
                    seleccionada = categoriaIdSeleccionada == null,
                    onClick = { onSeleccionar(null) }
                )

                categorias.forEach { cat ->
                    val c = parseColor(cat.colorHex)
                    FilaOpcionCategoria(
                        nombre = cat.nombre,
                        color = c,
                        seleccionada = categoriaIdSeleccionada == cat.id,
                        onClick = { onSeleccionar(cat.id) }
                    )
                }

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onCrearNueva,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Rosa.copy(alpha = 0.6f))
                ) {
                    Icon(Icons.Filled.Add, null, tint = Rosa, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Crear nueva categoría", color = Rosa, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cancelar", color = TextoSecundario)
            }
        }
    )
}

@Composable
private fun FilaOpcionCategoria(
    nombre: String,
    color: Color,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (seleccionada) color.copy(alpha = 0.2f) else FondoCard,
        border = BorderStroke(
            width = if (seleccionada) 1.5.dp else 1.dp,
            color = if (seleccionada) color else Color(0xFF332050)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
            Text(
                text = nombre,
                color = if (seleccionada) color else TextoPrimario,
                fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            if (seleccionada) {
                Icon(Icons.Filled.Check, null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Componente: Diálogo Rápido para Crear Categoría
// -------------------------------------------------------------
@Composable
private fun DialogoNuevaCategoriaRapida(
    onGuardar: (Categoria) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var colorSeleccionado by remember { mutableStateOf(COLORES_DISPONIBLES[0]) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = FondoCardElevado,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Category, null, tint = Rosa, modifier = Modifier.size(18.dp))
                Text("Nueva Categoría", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = false },
                    label = { Text("Nombre de la categoría") },
                    isError = error && nombre.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Rosa,
                        unfocusedBorderColor = Color(0xFF3D2B55),
                        focusedContainerColor = FondoCard,
                        unfocusedContainerColor = FondoCard,
                        focusedTextColor = TextoPrimario,
                        unfocusedTextColor = TextoPrimario,
                        focusedLabelColor = Rosa,
                        unfocusedLabelColor = TextoSecundario
                    )
                )
                if (error && nombre.isBlank()) {
                    Text("El nombre no puede estar vacío", color = Rojo, fontSize = 11.sp)
                }

                Text("Selecciona un color:", color = TextoSecundario, fontSize = 11.sp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    COLORES_DISPONIBLES.chunked(8).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            fila.forEach { hex ->
                                val c = parseColor(hex)
                                val sel = hex == colorSeleccionado
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(c, CircleShape)
                                        .then(
                                            if (sel) Modifier.border(2.dp, Color.White, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { colorSeleccionado = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (sel) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isBlank()) {
                        error = true
                        return@Button
                    }
                    onGuardar(Categoria(nombre = nombre.trim(), colorHex = colorSeleccionado))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Crear y Asignar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cancelar", color = TextoSecundario)
            }
        }
    )
}
