package com.example.gestorstock.ui.categorias

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.theme.*
import com.example.gestorstock.ui.StockViewModel
import com.example.gestorstock.ui.inventario.parseColor

// Paleta de colores predefinidos para elegir
val COLORES_DISPONIBLES = listOf(
    "#FF6B6B", "#FF9F43", "#FECA57", "#48DBFB",
    "#1DD1A1", "#54A0FF", "#5F27CD", "#FF9FF3",
    "#00D2D3", "#EA2027", "#EE5A24", "#009432",
    "#0652DD", "#833471", "#C4E538", "#FDA7DF"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    viewModel: StockViewModel,
    onVolver: () -> Unit
) {
    val categorias by viewModel.categorias.collectAsState()
    var mostrarDialog by remember { mutableStateOf(false) }
    var categoriaEditar by remember { mutableStateOf<Categoria?>(null) }

    Scaffold(
        containerColor = FondoOscuro,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoOscuro,
                    titleContentColor = TextoPrimario
                ),
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, null, tint = TextoSecundario)
                    }
                },
                title = { Text("Categorías", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { categoriaEditar = null; mostrarDialog = true },
                containerColor = Rosa,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, "Nueva categoría", modifier = Modifier.size(26.dp))
            }
        }
    ) { padding ->
        if (categorias.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Category, null, tint = TextoDeshabilitado, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No hay categorías aún", color = TextoPrimario, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Crea categorías para organizar tus artículos", color = TextoSecundario, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categorias, key = { it.id }) { cat ->
                    CategoriaItem(
                        categoria = cat,
                        onEditar = { categoriaEditar = cat; mostrarDialog = true },
                        onEliminar = { viewModel.eliminarCategoria(cat) }
                    )
                }
            }
        }
    }

    if (mostrarDialog) {
        CategoriaDialog(
            categoria = categoriaEditar,
            onGuardar = { cat ->
                viewModel.guardarCategoria(cat)
                mostrarDialog = false
            },
            onCerrar = { mostrarDialog = false }
        )
    }
}

@Composable
private fun CategoriaItem(
    categoria: Categoria,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    val color = parseColor(categoria.colorHex)
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = FondoCard),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(44.dp).background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(24.dp).background(color, CircleShape))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(categoria.nombre, fontWeight = FontWeight.SemiBold, color = TextoPrimario, fontSize = 16.sp)
                Text(categoria.colorHex, color = TextoSecundario, fontSize = 12.sp)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Filled.Edit, null, tint = TextoSecundario, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { mostrarConfirmacion = true }) {
                Icon(Icons.Filled.Delete, null, tint = Rojo.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            containerColor = FondoCardElevado,
            title = { Text("¿Eliminar categoría?", color = TextoPrimario) },
            text = { Text("Los artículos de esta categoría quedarán sin categoría.", color = TextoSecundario) },
            confirmButton = {
                TextButton(onClick = { onEliminar(); mostrarConfirmacion = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = Rojo)) {
                    Text("Eliminar", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar", color = TextoSecundario) }
            }
        )
    }
}

@Composable
private fun CategoriaDialog(
    categoria: Categoria?,
    onGuardar: (Categoria) -> Unit,
    onCerrar: () -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var colorSeleccionado by remember { mutableStateOf(categoria?.colorHex ?: COLORES_DISPONIBLES[0]) }
    var mostrarError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = FondoCardElevado,
        title = {
            Text(
                if (categoria == null) "Nueva categoría" else "Editar categoría",
                color = TextoPrimario,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la categoría") },
                    isError = mostrarError && nombre.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
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
                if (mostrarError && nombre.isBlank()) {
                    Text("El nombre no puede estar vacío", color = Rojo, fontSize = 12.sp)
                }

                Text("Color", fontWeight = FontWeight.SemiBold, color = TextoPrimario)

                // Preview del color seleccionado
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(modifier = Modifier.size(36.dp).background(parseColor(colorSeleccionado), CircleShape))
                    Text(colorSeleccionado, color = TextoSecundario, fontSize = 12.sp)
                }

                // Grid de colores
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    COLORES_DISPONIBLES.chunked(8).forEach { fila ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            fila.forEach { hex ->
                                val color = parseColor(hex)
                                val seleccionado = hex == colorSeleccionado
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(color, CircleShape)
                                        .then(
                                            if (seleccionado) Modifier.border(2.dp, Color.White, CircleShape)
                                            else Modifier
                                        )
                                        .clickable { colorSeleccionado = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (seleccionado) {
                                        Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                    if (nombre.isBlank()) { mostrarError = true; return@Button }
                    onGuardar(
                        (categoria ?: Categoria(nombre = "")).copy(
                            nombre = nombre.trim(),
                            colorHex = colorSeleccionado
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Rosa)
            ) {
                Text("Guardar", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}
