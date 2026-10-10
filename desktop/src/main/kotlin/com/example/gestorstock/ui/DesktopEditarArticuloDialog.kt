package com.example.gestorstock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopEditarArticuloDialog(
    viewModel: DesktopStockViewModel,
    articuloExistente: Articulo?,
    onCerrar: () -> Unit
) {
    val categorias by viewModel.categorias.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()

    var nombre by remember { mutableStateOf(articuloExistente?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(articuloExistente?.descripcion ?: "") }
    var precioStr by remember { mutableStateOf(if (articuloExistente != null && articuloExistente.precio > 0) String.format(Locale.US, "%.2f", articuloExistente.precio) else "") }
    var costoStr by remember { mutableStateOf(if (articuloExistente != null && articuloExistente.precioCosto > 0) String.format(Locale.US, "%.2f", articuloExistente.precioCosto) else "") }
    var stockStr by remember { mutableStateOf(articuloExistente?.stock?.toString() ?: "1") }
    var stockMinStr by remember { mutableStateOf(articuloExistente?.stockMinimo?.toString() ?: "2") }
    var unidadMedida by remember { mutableStateOf(articuloExistente?.unidadMedida ?: "pza") }
    var categoriaId by remember { mutableStateOf(articuloExistente?.categoriaId) }
    var codigoBarras by remember { mutableStateOf(articuloExistente?.codigoBarras ?: "") }
    var fotoPath by remember { mutableStateOf(articuloExistente?.fotoUri) }

    val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var fechaCaducidadStr by remember {
        mutableStateOf(articuloExistente?.fechaCaducidad?.let { sdfFecha.format(Date(it)) } ?: "")
    }

    var mostrarDialogoEtiqueta by remember { mutableStateOf(false) }
    var mostrarCrearCategoria by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf<String?>(null) }

    val precio = precioStr.toDoubleOrNull() ?: 0.0
    val costo = costoStr.toDoubleOrNull() ?: 0.0
    val margenBeneficio = if (costo > 0) ((precio - costo) / costo) * 100 else 100.0
    val gananciaNeta = precio - costo

    val catSeleccionada = categorias.find { it.id == categoriaId }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier
                .width(820.dp)
                .heightIn(max = 700.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (articuloExistente == null) "✨ Nuevo Artículo" else "✏️ Editar Artículo",
                            color = TextoPrimario,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text("Completa la información del inventario y venta", color = TextoSecundario, fontSize = 12.sp)
                    }

                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, null, tint = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fila 1: Imagen y datos básicos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Columna Foto
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.width(180.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(FondoOscuro)
                                    .border(1.dp, Color(0xFF3D2A54), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (fotoPath != null) {
                                    ArticuloImage(fotoUri = fotoPath, modifier = Modifier.fillMaxSize())
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.AddPhotoAlternate, null, tint = RosaClaro, modifier = Modifier.size(36.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text("Sin imagen", color = TextoSecundario, fontSize = 11.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val fd = FileDialog(null as Frame?, "Seleccionar Foto del Producto", FileDialog.LOAD).apply {
                                            isVisible = true
                                        }
                                        if (fd.directory != null && fd.file != null) {
                                            val archivo = File(fd.directory, fd.file)
                                            val rutaGuardada = viewModel.guardarFotoArticulo(archivo)
                                            if (rutaGuardada != null) {
                                                fotoPath = rutaGuardada
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Morado),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Elegir", fontSize = 11.sp)
                                }

                                if (fotoPath != null) {
                                    IconButton(
                                        onClick = {
                                            val rotada = viewModel.rotarFotoArticulo(fotoPath!!)
                                            if (rotada != null) fotoPath = rotada
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.RotateRight, "Rotar", tint = RosaClaro, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = { fotoPath = null },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Quitar", tint = Rojo, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // Columna Campos Principales
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nombre,
                                onValueChange = { nombre = it },
                                label = { Text("Nombre del Producto *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Rosa,
                                    unfocusedBorderColor = Color(0xFF3D2A54),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = TextoPrimario
                                )
                            )

                            OutlinedTextField(
                                value = descripcion,
                                onValueChange = { descripcion = it },
                                label = { Text("Descripción / Detalles") },
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Rosa,
                                    unfocusedBorderColor = Color(0xFF3D2A54),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = TextoPrimario
                                )
                            )

                            // Selector de Categoría
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var expandedCat by remember { mutableStateOf(false) }

                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { expandedCat = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                catSeleccionada?.nombre ?: "Seleccionar Categoría...",
                                                color = if (catSeleccionada != null) Color.White else TextoSecundario
                                            )
                                            Icon(Icons.Default.ArrowDropDown, null)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = expandedCat,
                                        onDismissRequest = { expandedCat = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Sin categoría") },
                                            onClick = {
                                                categoriaId = null
                                                expandedCat = false
                                            }
                                        )
                                        categorias.forEach { cat ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .background(parseHexColor(cat.colorHex), CircleShape)
                                                        )
                                                        Text(cat.nombre)
                                                    }
                                                },
                                                onClick = {
                                                    categoriaId = cat.id
                                                    expandedCat = false
                                                }
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { mostrarCrearCategoria = true },
                                    modifier = Modifier.background(Morado, RoundedCornerShape(8.dp)).size(40.dp)
                                ) {
                                    Icon(Icons.Default.Add, "Nueva Categoría", tint = Color.White)
                                }
                            }
                        }
                    }

                    // Fila 2: Precios y Margen
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Precios y Ganancia", color = RosaClaro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = precioStr,
                                    onValueChange = { precioStr = it },
                                    label = { Text("Precio de Venta ($)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = costoStr,
                                    onValueChange = { costoStr = it },
                                    label = { Text("Precio de Costo ($)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                // Resumen de beneficio en vivo
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = FondoCardElevado,
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Ganancia: +$${String.format(Locale.US, "%.2f", gananciaNeta)} MXN",
                                            color = Verde,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            "Margen: ${String.format(Locale.US, "%.1f", margenBeneficio)}%",
                                            color = if (margenBeneficio >= 30) Verde else Naranja,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Fila 3: Stock y Unidades
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = stockStr,
                            onValueChange = { stockStr = it },
                            label = { Text("Stock Actual") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = stockMinStr,
                            onValueChange = { stockMinStr = it },
                            label = { Text("Stock Mínimo (Alerta)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = unidadMedida,
                            onValueChange = { unidadMedida = it },
                            label = { Text("Unidad (pza, kg, m)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = fechaCaducidadStr,
                            onValueChange = { fechaCaducidadStr = it },
                            label = { Text("Caducidad (dd/mm/aaaa)") },
                            placeholder = { Text("Opcional") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Fila 4: Código de Barras y Etiqueta
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Código de Barras (Code 39)", color = RosaClaro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = codigoBarras,
                                    onValueChange = { codigoBarras = it },
                                    label = { Text("Código de Barras") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                Button(
                                    onClick = { codigoBarras = viewModel.generarCodigoBarrasUnico() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Morado)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Generar Único")
                                }

                                if (codigoBarras.isNotBlank()) {
                                    Button(
                                        onClick = { mostrarDialogoEtiqueta = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                                    ) {
                                        Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Imprimir Etiqueta")
                                    }
                                }
                            }

                            if (codigoBarras.isNotBlank()) {
                                Spacer(Modifier.height(10.dp))
                                val barrasBitmap = remember(codigoBarras) {
                                    try {
                                        DesktopBarcodeLabelHelper.generarBarrasBufferedImage(codigoBarras, 380, 50).toComposeImageBitmap()
                                    } catch (_: Exception) { null }
                                }
                                if (barrasBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(bitmap = barrasBitmap, contentDescription = "Barras Code 39")
                                    }
                                }
                            }
                        }
                    }

                    if (errorMensaje != null) {
                        Text(errorMensaje!!, color = Rojo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCerrar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = TextoSecundario)
                    }

                    Button(
                        onClick = {
                            if (nombre.isBlank()) {
                                errorMensaje = "Por favor introduce un nombre para el artículo."
                                return@Button
                            }

                            val pVal = precioStr.toDoubleOrNull() ?: 0.0
                            val cVal = costoStr.toDoubleOrNull() ?: 0.0
                            val sVal = stockStr.toIntOrNull() ?: 0
                            val sMinVal = stockMinStr.toIntOrNull() ?: 2

                            var fechaCadLong: Long? = null
                            if (fechaCaducidadStr.isNotBlank()) {
                                try {
                                    val parsed = sdfFecha.parse(fechaCaducidadStr.trim())
                                    if (parsed != null) fechaCadLong = parsed.time
                                } catch (_: Exception) {}
                            }

                            val codFinal = codigoBarras.trim().ifBlank { null }

                            val art = Articulo(
                                id = articuloExistente?.id ?: 0,
                                nombre = nombre.trim(),
                                descripcion = descripcion.trim(),
                                precio = pVal,
                                precioCosto = cVal,
                                stock = sVal,
                                stockMinimo = sMinVal,
                                unidadMedida = unidadMedida.trim().ifBlank { "pza" },
                                fechaCaducidad = fechaCadLong,
                                fotoUri = fotoPath,
                                categoriaId = categoriaId,
                                codigoBarras = codFinal,
                                fechaCreacion = articuloExistente?.fechaCreacion ?: System.currentTimeMillis()
                            )

                            viewModel.guardarArticulo(art) {
                                onCerrar()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                    ) {
                        Text("Guardar Artículo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostrarCrearCategoria) {
        DesktopCategoriasDialog(viewModel = viewModel, onCerrar = { mostrarCrearCategoria = false })
    }

    if (mostrarDialogoEtiqueta && codigoBarras.isNotBlank()) {
        DesktopEtiquetaDialog(
            nombreTienda = nombreTienda,
            nombreArticulo = nombre.ifBlank { "Producto" },
            categoria = catSeleccionada?.nombre,
            codigo = codigoBarras,
            precio = precio,
            unidadMedida = unidadMedida,
            onCerrar = { mostrarDialogoEtiqueta = false }
        )
    }
}
