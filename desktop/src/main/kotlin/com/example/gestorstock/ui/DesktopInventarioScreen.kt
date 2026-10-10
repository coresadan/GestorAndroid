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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gestorstock.data.DesktopDataExportHelper
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopInventarioScreen(
    viewModel: DesktopStockViewModel,
    onNuevoArticulo: () -> Unit,
    onEditarArticulo: (Articulo) -> Unit,
    onVerCategorias: () -> Unit,
    onAbrirAjustes: () -> Unit
) {
    val articulos by viewModel.articulos.collectAsState()
    val todosArticulos by viewModel.todosArticulos.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val filtro by viewModel.filtro.collectAsState()
    val nombreTienda by viewModel.nombreTienda.collectAsState()

    var esVistaCuadricula by remember { mutableStateOf(true) }
    var articuloParaEliminar by remember { mutableStateOf<Articulo?>(null) }
    var articuloParaEtiqueta by remember { mutableStateOf<Articulo?>(null) }

    // Métricas del catálogo
    val totalArticulosRegistrados = todosArticulos.size
    val valorTotalInventario = todosArticulos.sumOf { it.precio * it.stock }
    val cantidadStockBajo = todosArticulos.count { it.stock <= it.stockMinimo }
    val ahora = System.currentTimeMillis()
    val limite15Dias = ahora + 15L * 24 * 60 * 60 * 1000
    val cantidadProximosCaducar = todosArticulos.count { it.fechaCaducidad != null && it.fechaCaducidad <= limite15Dias }

    val catMap = remember(categorias) { categorias.associateBy { it.id } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoOscuro)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera Principal y Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "📦 Inventario y Catálogo de Productos",
                    color = TextoPrimario,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    "Control de existencias, precios, etiquetas y alertas de stock",
                    color = TextoSecundario,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onNuevoArticulo,
                    colors = ButtonDefaults.buttonColors(containerColor = Rosa),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo Artículo", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onVerCategorias,
                    colors = ButtonDefaults.buttonColors(containerColor = Morado),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Category, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Categorías")
                }

                OutlinedButton(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                        val fd = FileDialog(null as Frame?, "Exportar Inventario a Excel (CSV)", FileDialog.SAVE).apply {
                            file = "inventario_${sdf.format(Date())}.csv"
                            isVisible = true
                        }
                        if (fd.directory != null && fd.file != null) {
                            val dest = File(fd.directory, fd.file)
                            DesktopDataExportHelper.exportarInventarioCsv(dest, todosArticulos, categorias)
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.TableChart, null, tint = Verde, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Exportar Excel", color = Color.White)
                }

                IconButton(
                    onClick = onAbrirAjustes,
                    modifier = Modifier.background(FondoCardElevado, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Default.Settings, "Configuración", tint = TextoPrimario)
                }
            }
        }

        // Tarjetas Resumen de Métricas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Artículos Totales
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Productos", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("$totalArticulosRegistrados artículos", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            // Valor del Inventario
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Valor de Inventario", color = TextoSecundario, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("$${String.format(Locale.US, "%.2f", valorTotalInventario)} MXN", color = Verde, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }

            // Stock Bajo
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleSoloStockBajo() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filtro.soloStockBajo) Rojo.copy(alpha = 0.2f) else FondoCardElevado
                ),
                border = if (filtro.soloStockBajo) androidx.compose.foundation.BorderStroke(1.5.dp, Rojo) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Warning, null, tint = if (cantidadStockBajo > 0) Rojo else TextoSecundario, modifier = Modifier.size(14.dp))
                        Text("Stock Bajo / Agotado", color = TextoSecundario, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$cantidadStockBajo alertas",
                        color = if (cantidadStockBajo > 0) Rojo else TextoPrimario,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }

            // Próximos a Vencer
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.toggleProximosACaducar() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (filtro.proximosACaducar) Naranja.copy(alpha = 0.2f) else FondoCardElevado
                ),
                border = if (filtro.proximosACaducar) androidx.compose.foundation.BorderStroke(1.5.dp, Naranja) else null
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = if (cantidadProximosCaducar > 0) Naranja else TextoSecundario, modifier = Modifier.size(14.dp))
                        Text("Por Vencer (<15 días)", color = TextoSecundario, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$cantidadProximosCaducar productos",
                        color = if (cantidadProximosCaducar > 0) Naranja else TextoPrimario,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        // Barra de Búsqueda, Filtros y Switch de Vista
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filtro.busqueda,
                onValueChange = { viewModel.setBusqueda(it) },
                placeholder = { Text("Buscar artículo por nombre, descripción o código...", color = TextoSecundario, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = RosaClaro) },
                trailingIcon = {
                    if (filtro.busqueda.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setBusqueda("") }) {
                            Icon(Icons.Default.Clear, null, tint = TextoSecundario)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.width(420.dp).height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Rosa,
                    unfocusedBorderColor = Color(0xFF3D2A54),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = TextoPrimario
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Alternar cuadrícula vs lista
                IconButton(
                    onClick = { esVistaCuadricula = true },
                    modifier = Modifier.background(if (esVistaCuadricula) Rosa else FondoCardElevado, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.GridView, "Cuadrícula", tint = Color.White)
                }

                IconButton(
                    onClick = { esVistaCuadricula = false },
                    modifier = Modifier.background(if (!esVistaCuadricula) Rosa else FondoCardElevado, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ViewList, "Lista", tint = Color.White)
                }
            }
        }

        // Chips de Categorías
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = filtro.categoriaId == null,
                    onClick = { viewModel.setCategoria(null) },
                    label = { Text("Todas las categorías (${todosArticulos.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Rosa,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(categorias, key = { it.id }) { cat ->
                val artCount = todosArticulos.count { it.categoriaId == cat.id }
                FilterChip(
                    selected = filtro.categoriaId == cat.id,
                    onClick = { viewModel.setCategoria(if (filtro.categoriaId == cat.id) null else cat.id) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(parseHexColor(cat.colorHex), CircleShape))
                            Text("${cat.nombre} ($artCount)")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Morado,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Lista / Cuadrícula de Artículos
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCard)
        ) {
            if (articulos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inventory, null, tint = TextoSecundario, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No se encontraron artículos en el inventario", color = TextoSecundario, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onNuevoArticulo, colors = ButtonDefaults.buttonColors(containerColor = Rosa)) {
                            Text("Agregar Primer Artículo")
                        }
                    }
                }
            } else if (esVistaCuadricula) {
                // VISTA CUADRÍCULA
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 250.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(articulos, key = { it.id }) { art ->
                        val cat = catMap[art.categoriaId]
                        val esBajoStock = art.stock <= art.stockMinimo

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ArticuloImage(fotoUri = art.fotoUri, modifier = Modifier.fillMaxSize())

                                    // Badge de categoría
                                    if (cat != null) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = parseHexColor(cat.colorHex)
                                        ) {
                                            Text(
                                                cat.nombre,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Badge de Alerta Stock
                                    if (esBajoStock) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Rojo
                                        ) {
                                            Text(
                                                if (art.stock <= 0) "¡AGOTADO!" else "¡STOCK BAJO!",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    art.nombre,
                                    color = TextoPrimario,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (art.descripcion.isNotBlank()) {
                                    Text(
                                        art.descripcion,
                                        color = TextoSecundario,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "$${String.format(Locale.US, "%.2f", art.precio)} MXN",
                                            color = Rosa,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                        if (art.precioCosto > 0) {
                                            Text(
                                                "Costo: $${String.format(Locale.US, "%.2f", art.precioCosto)}",
                                                color = TextoSecundario,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    // Controles rápidos de stock (+ / -)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.background(FondoOscuro, RoundedCornerShape(8.dp)).padding(2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.cambiarStock(art.id, -1) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }

                                        Text(
                                            "${art.stock} ${art.unidadMedida}",
                                            color = if (esBajoStock) Rojo else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )

                                        IconButton(
                                            onClick = { viewModel.cambiarStock(art.id, 1) },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))

                                // Botones inferiores
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!art.codigoBarras.isNullOrBlank()) {
                                        IconButton(
                                            onClick = { articuloParaEtiqueta = art },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.QrCode, "Etiqueta", tint = MoradoClaro, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    IconButton(
                                        onClick = { onEditarArticulo(art) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Editar", tint = TextoPrimario, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { articuloParaEliminar = art },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Eliminar", tint = Rojo, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // VISTA LISTA
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(articulos, key = { it.id }) { art ->
                        val cat = catMap[art.categoriaId]
                        val esBajoStock = art.stock <= art.stockMinimo

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ArticuloImage(fotoUri = art.fotoUri, modifier = Modifier.fillMaxSize())
                                }

                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(art.nombre, color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (cat != null) {
                                            Text(cat.nombre, color = parseHexColor(cat.colorHex), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text("•", color = TextoSecundario, fontSize = 11.sp)
                                        }
                                        if (!art.codigoBarras.isNullOrBlank()) {
                                            Text(art.codigoBarras, color = TextoSecundario, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(120.dp)) {
                                    Text("$${String.format(Locale.US, "%.2f", art.precio)} MXN", color = Rosa, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (art.precioCosto > 0) {
                                        Text("Costo: $${String.format(Locale.US, "%.2f", art.precioCosto)}", color = TextoSecundario, fontSize = 11.sp)
                                    }
                                }

                                // Controles Stock
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(FondoOscuro, RoundedCornerShape(8.dp)).padding(2.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.cambiarStock(art.id, -1) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }

                                    Text(
                                        "${art.stock} ${art.unidadMedida}",
                                        color = if (esBajoStock) Rojo else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.cambiarStock(art.id, 1) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }

                                Row {
                                    if (!art.codigoBarras.isNullOrBlank()) {
                                        IconButton(onClick = { articuloParaEtiqueta = art }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.QrCode, "Etiqueta", tint = MoradoClaro, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    IconButton(onClick = { onEditarArticulo(art) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, "Editar", tint = TextoPrimario, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(onClick = { articuloParaEliminar = art }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Eliminar", tint = Rojo, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmar eliminación
    if (articuloParaEliminar != null) {
        Dialog(onDismissRequest = { articuloParaEliminar = null }) {
            Card(
                modifier = Modifier.width(420.dp).padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                    Text("🗑️ ¿Eliminar ${articuloParaEliminar?.nombre}?", color = Rojo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Esta acción eliminará el producto del inventario.", color = TextoSecundario, fontSize = 13.sp)

                    Spacer(Modifier.height(18.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { articuloParaEliminar = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancelar", color = TextoSecundario)
                        }

                        Button(
                            onClick = {
                                viewModel.eliminarArticulo(articuloParaEliminar!!)
                                articuloParaEliminar = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Rojo),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Eliminar", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal Etiqueta
    if (articuloParaEtiqueta != null) {
        val cat = catMap[articuloParaEtiqueta?.categoriaId]
        DesktopEtiquetaDialog(
            nombreTienda = nombreTienda,
            nombreArticulo = articuloParaEtiqueta!!.nombre,
            categoria = cat?.nombre,
            codigo = articuloParaEtiqueta!!.codigoBarras ?: "ART-${articuloParaEtiqueta!!.id}",
            precio = articuloParaEtiqueta!!.precio,
            unidadMedida = articuloParaEtiqueta!!.unidadMedida,
            onCerrar = { articuloParaEtiqueta = null }
        )
    }
}
