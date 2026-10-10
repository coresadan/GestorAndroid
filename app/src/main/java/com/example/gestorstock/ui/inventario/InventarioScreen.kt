package com.example.gestorstock.ui.inventario

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import coil.compose.AsyncImage
import com.example.gestorstock.data.DataExportHelper
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.theme.*
import com.example.gestorstock.ui.StockViewModel
import com.example.gestorstock.ui.scanner.BarcodeScannerDialog
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    viewModel: StockViewModel,
    onNuevoArticulo: () -> Unit,
    onEditarArticulo: (Int) -> Unit,
    onVerCategorias: () -> Unit,
    onVentaRapida: () -> Unit,
    onVerHistorial: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val articulos by viewModel.articulos.collectAsState()
    val todosArticulos by viewModel.todosArticulos.collectAsState()
    val ventas by viewModel.ventas.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val filtro by viewModel.filtro.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()
    val logoTiendaUri by viewModel.logoTiendaUri.collectAsState()

    var mostrarEscanerBusqueda by remember { mutableStateOf(false) }

    val launcherRestaurar = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val jsonStr = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
                    if (!jsonStr.isNullOrBlank()) {
                        val backup = DataExportHelper.parsearBackupJson(context, jsonStr)
                        if (backup != null) {
                            viewModel.restaurarBackup(backup) { exito, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        } else {
                            snackbarHostState.showSnackbar("El archivo seleccionado no es un formato de respaldo válido.")
                        }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error al leer el archivo: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        containerColor = FondoOscuro,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                nombreTienda = nombreTienda,
                logoTiendaUri = logoTiendaUri,
                onCambiarNombre = { viewModel.setNombreTienda(it) },
                onSeleccionarLogo = { viewModel.guardarLogoTienda(it) },
                onEliminarLogo = { viewModel.eliminarLogoTienda() },
                onVerCategorias = onVerCategorias,
                onVentaRapida = onVentaRapida,
                onVerHistorial = onVerHistorial,
                onExportarCsv = {
                    try {
                        val archivo = DataExportHelper.exportarInventarioCsv(context, todosArticulos, categorias)
                        DataExportHelper.compartirArchivo(context, archivo, "text/csv", "Exportar Inventario Excel")
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("Error al exportar: ${e.message}") }
                    }
                },
                onExportarBackup = {
                    try {
                        val archivo = DataExportHelper.exportarBackupCompletoJson(context, todosArticulos, categorias, ventas)
                        DataExportHelper.compartirArchivo(context, archivo, "application/json", "Copia de Seguridad GestorStock")
                    } catch (e: Exception) {
                        scope.launch { snackbarHostState.showSnackbar("Error al crear copia: ${e.message}") }
                    }
                },
                onRestaurarBackup = {
                    launcherRestaurar.launch("*/*")
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevoArticulo,
                containerColor = Rosa,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir artículo", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Barra de búsqueda y filtros
            BusquedaYFiltros(
                busqueda = filtro.busqueda,
                categoriaSeleccionada = filtro.categoriaId,
                soloStockBajo = filtro.soloStockBajo,
                proximosACaducar = filtro.proximosACaducar,
                categorias = categorias,
                onBusquedaChange = { viewModel.setBusqueda(it) },
                onCategoriaChange = { viewModel.setCategoria(it) },
                onToggleStockBajo = { viewModel.toggleSoloStockBajo() },
                onToggleCaducidad = { viewModel.toggleProximosACaducar() },
                onAbrirEscaner = { mostrarEscanerBusqueda = true }
            )

            // Estadísticas rápidas y métricas de valor comercial
            EstadisticasRapidas(articulos = articulos, todosArticulos = todosArticulos)

            // Grid de artículos
            if (articulos.isEmpty()) {
                EmptyState(onNuevoArticulo = onNuevoArticulo)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(articulos, key = { it.id }) { articulo ->
                        val categoria = categorias.find { it.id == articulo.categoriaId }
                        ArticuloCard(
                            articulo = articulo,
                            categoria = categoria,
                            categorias = categorias,
                            onEditar = { onEditarArticulo(articulo.id) },
                            onSumar = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.cambiarStock(articulo.id, 1)
                            },
                            onRestar = {
                                if (articulo.stock > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.cambiarStock(articulo.id, -1)
                                }
                            },
                            onEliminar = { viewModel.eliminarArticulo(articulo) },
                            onCambiarCategoria = { nuevaCatId ->
                                viewModel.guardarArticulo(articulo.copy(categoriaId = nuevaCatId)) {}
                            }
                        )
                    }
                }
            }
        }

        if (mostrarEscanerBusqueda) {
            BarcodeScannerDialog(
                titulo = "Buscar por Código / QR",
                subtitulo = "Escanea el código de un artículo para buscarlo en tu inventario",
                onCodigoDetectado = { codigo ->
                    viewModel.setBusqueda(codigo)
                    mostrarEscanerBusqueda = false
                },
                onCerrar = { mostrarEscanerBusqueda = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    nombreTienda: String,
    logoTiendaUri: String?,
    onCambiarNombre: (String) -> Unit,
    onSeleccionarLogo: (android.net.Uri) -> Unit,
    onEliminarLogo: () -> Unit,
    onVerCategorias: () -> Unit,
    onVentaRapida: () -> Unit,
    onVerHistorial: () -> Unit,
    onExportarCsv: () -> Unit,
    onExportarBackup: () -> Unit,
    onRestaurarBackup: () -> Unit
) {
    var mostrarDialogoPersonalizar by remember { mutableStateOf(false) }
    var menuOpcionesAbierto by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF0D0718),
            titleContentColor = TextoPrimario
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Logo de la tienda (tappable para cambiar)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Brush.linearGradient(listOf(Rosa, Morado)), CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Rosa, Morado)),
                            CircleShape
                        )
                        .clickable { mostrarDialogoPersonalizar = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoTiendaUri != null) {
                        AsyncImage(
                            model = logoTiendaUri,
                            contentDescription = "Logo de la tienda",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.Storefront,
                            contentDescription = "Logo predeterminado",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Nombre de la tienda
                Column(
                    modifier = Modifier.clickable { mostrarDialogoPersonalizar = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            nombreTienda,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextoPrimario
                        )
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Personalizar tienda",
                            tint = TextoDeshabilitado,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text("Toca el logo o nombre para editar", fontSize = 11.sp, color = TextoSecundario)
                }
            }
        },
        actions = {
            Button(
                onClick = onVentaRapida,
                colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Venta Rápida", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onVerHistorial) {
                Icon(Icons.Filled.Assessment, contentDescription = "Historial y Cierres", tint = TextoPrimario)
            }
            IconButton(onClick = onVerCategorias) {
                Icon(Icons.Outlined.Category, contentDescription = "Categorías", tint = TextoSecundario)
            }
            Box {
                IconButton(onClick = { menuOpcionesAbierto = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones", tint = TextoPrimario)
                }
                DropdownMenu(
                    expanded = menuOpcionesAbierto,
                    onDismissRequest = { menuOpcionesAbierto = false },
                    modifier = Modifier.background(FondoCardElevado)
                ) {
                    DropdownMenuItem(
                        text = { Text("📤 Exportar a Excel (CSV)", color = TextoPrimario, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = Verde, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuOpcionesAbierto = false
                            onExportarCsv()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("💾 Copia de Seguridad (JSON)", color = TextoPrimario, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Outlined.Save, null, tint = MoradoClaro, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuOpcionesAbierto = false
                            onExportarBackup()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📥 Restaurar Respaldo", color = TextoPrimario, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Outlined.UploadFile, null, tint = Rosa, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuOpcionesAbierto = false
                            onRestaurarBackup()
                        }
                    )
                }
            }
        }
    )

    if (mostrarDialogoPersonalizar) {
        DialogoPersonalizarTienda(
            nombreActual = nombreTienda,
            logoActualUri = logoTiendaUri,
            onGuardar = { nuevoNombre ->
                onCambiarNombre(nuevoNombre)
                mostrarDialogoPersonalizar = false
            },
            onSeleccionarLogo = { onSeleccionarLogo(it) },
            onEliminarLogo = { onEliminarLogo() },
            onCerrar = { mostrarDialogoPersonalizar = false }
        )
    }
}

@Composable
private fun DialogoPersonalizarTienda(
    nombreActual: String,
    logoActualUri: String?,
    onGuardar: (String) -> Unit,
    onSeleccionarLogo: (android.net.Uri) -> Unit,
    onEliminarLogo: () -> Unit,
    onCerrar: () -> Unit
) {
    var texto by remember { mutableStateOf(nombreActual) }
    var error by remember { mutableStateOf(false) }

    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { onSeleccionarLogo(it) }
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = FondoCardElevado,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.Storefront, null, tint = Rosa, modifier = Modifier.size(24.dp))
                Text("Personalizar tu Tienda", color = TextoPrimario, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sección de Logo
                Text(
                    "Logo de la tienda",
                    color = TextoPrimario,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(2.dp, Brush.linearGradient(listOf(Rosa, Morado)), CircleShape)
                        .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (logoActualUri != null) {
                        AsyncImage(
                            model = logoActualUri,
                            contentDescription = "Logo de la tienda",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Filled.Storefront,
                            contentDescription = "Logo predeterminado",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Badge de edición
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Rosa, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { launcher.launch("image/*") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Rosa.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Upload, null, tint = Rosa, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (logoActualUri != null) "Cambiar logo" else "Subir logo", color = Rosa, fontSize = 12.sp)
                    }

                    if (logoActualUri != null) {
                        TextButton(
                            onClick = onEliminarLogo,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Quitar", color = Rojo, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF332050), modifier = Modifier.padding(vertical = 2.dp))

                // Sección de Nombre
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it; error = false },
                    label = { Text("Nombre de la tienda") },
                    leadingIcon = { Icon(Icons.Filled.Storefront, null, tint = if (error) Rojo else TextoSecundario) },
                    singleLine = true,
                    isError = error,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Rosa,
                        unfocusedBorderColor = Color(0xFF3D2B55),
                        errorBorderColor = Rojo,
                        focusedContainerColor = FondoCard,
                        unfocusedContainerColor = FondoCard,
                        focusedTextColor = TextoPrimario,
                        unfocusedTextColor = TextoPrimario,
                        focusedLabelColor = Rosa,
                        unfocusedLabelColor = TextoSecundario
                    )
                )
                if (error) {
                    Text(
                        "El nombre no puede estar vacío",
                        color = Rojo,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (texto.isBlank()) { error = true; return@Button }
                    onGuardar(texto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Guardar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}

@Composable
private fun BusquedaYFiltros(
    busqueda: String,
    categoriaSeleccionada: Int?,
    soloStockBajo: Boolean,
    proximosACaducar: Boolean,
    categorias: List<Categoria>,
    onBusquedaChange: (String) -> Unit,
    onCategoriaChange: (Int?) -> Unit,
    onToggleStockBajo: () -> Unit,
    onToggleCaducidad: () -> Unit,
    onAbrirEscaner: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = onBusquedaChange,
            placeholder = { Text("Buscar artículo o código...", color = TextoDeshabilitado) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextoSecundario) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (busqueda.isNotEmpty()) {
                        IconButton(onClick = { onBusquedaChange("") }) {
                            Icon(Icons.Filled.Clear, null, tint = TextoSecundario)
                        }
                    }
                    IconButton(onClick = onAbrirEscaner) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Escanear código de barras / QR",
                            tint = Rosa
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Rosa,
                unfocusedBorderColor = Color(0xFF3D2B55),
                focusedContainerColor = FondoCard,
                unfocusedContainerColor = FondoCard,
                focusedTextColor = TextoPrimario,
                unfocusedTextColor = TextoPrimario
            )
        )

        Spacer(Modifier.height(10.dp))

        // Chips de filtros rápidos (Stock Bajo, Caducidad y Categorías)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = categoriaSeleccionada == null && !soloStockBajo && !proximosACaducar,
                onClick = {
                    onCategoriaChange(null)
                    if (soloStockBajo) onToggleStockBajo()
                    if (proximosACaducar) onToggleCaducidad()
                },
                label = { Text("Todas") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Rosa,
                    selectedLabelColor = Color.White,
                    containerColor = FondoCard,
                    labelColor = TextoSecundario
                )
            )

            // Filtro rápido: Stock Bajo / Agotado
            FilterChip(
                selected = soloStockBajo,
                onClick = onToggleStockBajo,
                label = { Text("⚠️ Reponer / Agotado") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Rojo.copy(alpha = 0.35f),
                    selectedLabelColor = Color.White,
                    containerColor = FondoCard,
                    labelColor = Rojo
                ),
                border = BorderStroke(1.dp, if (soloStockBajo) Rojo else Color.Transparent)
            )

            // Filtro rápido: Próximos a Caducar
            FilterChip(
                selected = proximosACaducar,
                onClick = onToggleCaducidad,
                label = { Text("⏳ Por Vencer (15d)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Naranja.copy(alpha = 0.35f),
                    selectedLabelColor = Color.White,
                    containerColor = FondoCard,
                    labelColor = Naranja
                ),
                border = BorderStroke(1.dp, if (proximosACaducar) Naranja else Color.Transparent)
            )

            categorias.forEach { cat ->
                val catColor = parseColor(cat.colorHex)
                FilterChip(
                    selected = categoriaSeleccionada == cat.id,
                    onClick = { onCategoriaChange(if (categoriaSeleccionada == cat.id) null else cat.id) },
                    label = { Text(cat.nombre) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(catColor, CircleShape)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = catColor.copy(alpha = 0.25f),
                        selectedLabelColor = catColor,
                        containerColor = FondoCard,
                        labelColor = TextoSecundario
                    )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EstadisticasRapidas(articulos: List<Articulo>, todosArticulos: List<Articulo>) {
    val total = articulos.size
    val sinStock = articulos.count { it.stock == 0 }
    val stockBajo = articulos.count { it.stock in 1..it.stockMinimo }

    val inversionTotal = todosArticulos.sumOf { it.stock * it.precioCosto }
    val valorVentaTotal = todosArticulos.sumOf { it.stock * it.precio }
    val gananciaPotencial = valorVentaTotal - inversionTotal

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatChip(label = "$total mostrados", color = Morado, modifier = Modifier.weight(1f))
            StatChip(label = "$sinStock agotados", color = Rojo, modifier = Modifier.weight(1f))
            StatChip(label = "$stockBajo por reponer", color = Naranja, modifier = Modifier.weight(1.1f))
        }

        Spacer(Modifier.height(6.dp))

        // Tarjeta resumen de inversión / valor comercial del inventario
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = FondoCard,
            border = BorderStroke(1.dp, Color(0xFF2E1F45)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Inversión Costo", fontSize = 10.sp, color = TextoSecundario)
                    Text(
                        String.format(Locale.US, "$%.2f", inversionTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextoPrimario
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF3D2B55)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Valor Venta", fontSize = 10.sp, color = TextoSecundario)
                    Text(
                        String.format(Locale.US, "$%.2f", valorVentaTotal),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RosaClaro
                    )
                }
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF3D2B55)))
                Column(horizontalAlignment = Alignment.End) {
                    Text("Ganancia Potencial", fontSize = 10.sp, color = TextoSecundario)
                    Text(
                        String.format(Locale.US, "+$%.2f", gananciaPotencial),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Verde
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun StatChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

@Composable
fun ArticuloCard(
    articulo: Articulo,
    categoria: Categoria?,
    categorias: List<Categoria> = emptyList(),
    onEditar: () -> Unit,
    onSumar: () -> Unit,
    onRestar: () -> Unit,
    onEliminar: () -> Unit,
    onCambiarCategoria: ((Int?) -> Unit)? = null
) {
    var mostrarMenu by remember { mutableStateOf(false) }
    var mostrarPickerCategoria by remember { mutableStateOf(false) }
    val stockColor = when {
        articulo.stock == 0 -> Rojo
        articulo.stock <= articulo.stockMinimo -> Naranja
        else -> Verde
    }
    val catColor = categoria?.let { parseColor(it.colorHex) } ?: TextoDeshabilitado

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditar() },
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Imagen del producto
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                if (articulo.fotoUri != null) {
                    AsyncImage(
                        model = articulo.fotoUri,
                        contentDescription = articulo.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        catColor.copy(alpha = 0.3f),
                                        FondoCardElevado
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = catColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Badge de categoría — tappable para cambiar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            if (categoria != null) catColor.copy(alpha = 0.85f)
                            else Color(0x88000000),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = onCambiarCategoria != null) { mostrarPickerCategoria = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = categoria?.nombre ?: "Sin categoría",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (onCambiarCategoria != null) {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Menú
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { mostrarMenu = true }) {
                        Icon(Icons.Filled.MoreVert, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = mostrarMenu,
                        onDismissRequest = { mostrarMenu = false },
                        containerColor = FondoCardElevado
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar", color = TextoPrimario) },
                            leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Rosa) },
                            onClick = { mostrarMenu = false; onEditar() }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Rojo) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Rojo) },
                            onClick = { mostrarMenu = false; onEliminar() }
                        )
                    }
                }
            }

            // Info del artículo
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = articulo.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = TextoPrimario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (articulo.descripcion.isNotEmpty()) {
                    Text(
                        text = articulo.descripcion,
                        fontSize = 12.sp,
                        color = TextoSecundario,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!articulo.codigoBarras.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(Icons.Filled.QrCode, contentDescription = null, tint = Morado, modifier = Modifier.size(12.dp))
                        Text(
                            text = articulo.codigoBarras,
                            fontSize = 11.sp,
                            color = Morado,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$${String.format("%.0f", articulo.precio)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Rosa
                    )
                    Spacer(Modifier.weight(1f))
                    // Indicador de stock
                    Box(
                        modifier = Modifier
                            .background(stockColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Stock: ${articulo.stock}",
                            fontSize = 12.sp,
                            color = stockColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Botones +/-
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = onRestar,
                        enabled = articulo.stock > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Rojo.copy(alpha = 0.2f),
                            contentColor = Rojo,
                            disabledContainerColor = Color(0xFF2A1D40),
                            disabledContentColor = TextoDeshabilitado
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Remove, null, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "${articulo.stock}",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextoPrimario
                    )
                    FilledIconButton(
                        onClick = onSumar,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Verde.copy(alpha = 0.2f),
                            contentColor = Verde
                        ),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }

    // Diálogo picker de categoría
    if (mostrarPickerCategoria && onCambiarCategoria != null) {
        AlertDialog(
            onDismissRequest = { mostrarPickerCategoria = false },
            containerColor = FondoCardElevado,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Category, null, tint = Rosa, modifier = Modifier.size(20.dp))
                    Text(
                        text = articulo.nombre,
                        color = TextoPrimario,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecciona una categoría:", color = TextoSecundario, fontSize = 12.sp)
                    PickerCategoriaItem(
                        nombre = "Sin categoría",
                        color = TextoDeshabilitado,
                        seleccionada = articulo.categoriaId == null,
                        onClick = { onCambiarCategoria(null); mostrarPickerCategoria = false }
                    )
                    categorias.forEach { cat ->
                        PickerCategoriaItem(
                            nombre = cat.nombre,
                            color = parseColor(cat.colorHex),
                            seleccionada = articulo.categoriaId == cat.id,
                            onClick = { onCambiarCategoria(cat.id); mostrarPickerCategoria = false }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarPickerCategoria = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

@Composable
private fun EmptyState(onNuevoArticulo: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = TextoDeshabilitado,
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("¡Tu inventario está vacío!", fontSize = 18.sp, color = TextoPrimario, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Toca el botón + para añadir tu primer artículo", fontSize = 14.sp, color = TextoSecundario)
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNuevoArticulo,
                colors = ButtonDefaults.buttonColors(containerColor = Rosa)
            ) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Añadir artículo")
            }
        }
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Rosa
    }
}

@Composable
private fun PickerCategoriaItem(
    nombre: String,
    color: Color,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (seleccionada) color.copy(alpha = 0.18f) else FondoCard)
            .border(
                width = if (seleccionada) 1.5.dp else 0.dp,
                color = if (seleccionada) color else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Text(
            text = nombre,
            color = if (seleccionada) color else TextoSecundario,
            fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (seleccionada) {
            Icon(Icons.Filled.Check, null, tint = color, modifier = Modifier.size(18.dp))
        }
    }
}
