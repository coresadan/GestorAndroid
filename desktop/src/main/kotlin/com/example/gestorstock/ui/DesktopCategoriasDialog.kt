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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*

private val PALETA_COLORES = listOf(
    "#E91E63", "#9C27B0", "#673AB7", "#3F51B5",
    "#2196F3", "#00BCD4", "#009688", "#4CAF50",
    "#8BC34A", "#FF9800", "#FF5722", "#795548"
)

fun parseHexColor(hex: String, fallback: Color = Rosa): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else if (clean.length == 8) {
            Color(colorInt)
        } else fallback
    } catch (_: Exception) {
        fallback
    }
}

@Composable
fun DesktopCategoriasDialog(
    viewModel: DesktopStockViewModel,
    onCerrar: () -> Unit
) {
    val categorias by viewModel.categorias.collectAsState()

    var editandoCategoria by remember { mutableStateOf<Categoria?>(null) }
    var nombreTexto by remember { mutableStateOf("") }
    var colorSeleccionado by remember { mutableStateOf(PALETA_COLORES[0]) }

    fun abrirEditar(cat: Categoria?) {
        editandoCategoria = cat
        if (cat != null) {
            nombreTexto = cat.nombre
            colorSeleccionado = cat.colorHex
        } else {
            nombreTexto = ""
            colorSeleccionado = PALETA_COLORES[0]
        }
    }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier
                .width(550.dp)
                .heightIn(max = 620.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🏷️ Gestión de Categorías", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${categorias.size} categorías creadas", color = TextoSecundario, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, null, tint = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Formulario de creación/edición
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            if (editandoCategoria == null) "Nueva Categoría" else "Editar: ${editandoCategoria?.nombre}",
                            color = RosaClaro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = nombreTexto,
                                onValueChange = { nombreTexto = it },
                                placeholder = { Text("Nombre de la categoría...") },
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
                                onClick = {
                                    if (nombreTexto.isNotBlank()) {
                                        val catAGuardar = Categoria(
                                            id = editandoCategoria?.id ?: 0,
                                            nombre = nombreTexto.trim(),
                                            colorHex = colorSeleccionado
                                        )
                                        viewModel.guardarCategoria(catAGuardar) {
                                            abrirEditar(null)
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                            ) {
                                Text(if (editandoCategoria == null) "Agregar" else "Guardar")
                            }

                            if (editandoCategoria != null) {
                                TextButton(onClick = { abrirEditar(null) }) {
                                    Text("Cancelar", color = TextoSecundario)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Selector de colores
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PALETA_COLORES.forEach { hex ->
                                val color = parseHexColor(hex)
                                val esSeleccionado = (hex.uppercase() == colorSeleccionado.uppercase())
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { colorSeleccionado = hex }
                                        .border(
                                            width = if (esSeleccionado) 2.dp else 0.dp,
                                            color = if (esSeleccionado) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Lista de categorías
                Text("Categorías registradas:", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categorias, key = { it.id }) { cat ->
                        val catColor = parseHexColor(cat.colorHex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FondoCard, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(catColor, CircleShape)
                                )
                                Text(cat.nombre, color = TextoPrimario, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            }

                            Row {
                                IconButton(onClick = { abrirEditar(cat) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, "Editar", tint = TextoSecundario, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.eliminarCategoria(cat) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Eliminar", tint = Rojo, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
