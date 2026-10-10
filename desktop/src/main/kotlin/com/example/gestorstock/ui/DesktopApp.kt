package com.example.gestorstock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*

enum class DesktopNavTab(val titulo: String, val icono: androidx.compose.ui.graphics.vector.ImageVector) {
    INVENTARIO("Inventario", Icons.Default.Inventory2),
    VENTA_RAPIDA("TPV / Venta Rápida", Icons.Default.PointOfSale),
    HISTORIAL("Historial de Ventas", Icons.Default.ReceiptLong)
}

@Composable
fun DesktopApp(viewModel: DesktopStockViewModel) {
    val nombreTienda by viewModel.nombreTienda.collectAsState()
    val logoUri by viewModel.logoTiendaUri.collectAsState()

    var pestanaActiva by remember { mutableStateOf(DesktopNavTab.INVENTARIO) }

    // Diálogos globales
    var articuloAEditar by remember { mutableStateOf<Articulo?>(null) }
    var mostrarNuevoArticulo by remember { mutableStateOf(false) }
    var mostrarCategorias by remember { mutableStateOf(false) }
    var mostrarAjustes by remember { mutableStateOf(false) }

    GestorStockTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(FondoOscuro)
        ) {
            // BARRA DE NAVEGACIÓN SUPERIOR (TOP APP BAR)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = FondoCard,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Marca / Tienda
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(FondoCardElevado),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoUri != null) {
                                ArticuloImage(fotoUri = logoUri, modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Default.Storefront, null, tint = Rosa, modifier = Modifier.size(24.dp))
                            }
                        }

                        Column {
                            Text(
                                nombreTienda,
                                color = TextoPrimario,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                "GestorStock para Windows",
                                color = RosaClaro,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Pestañas de Navegación Centrales
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesktopNavTab.values().forEach { tab ->
                            val activa = (pestanaActiva == tab)
                            Button(
                                onClick = { pestanaActiva = tab },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activa) Rosa else Color.Transparent,
                                    contentColor = if (activa) Color.White else TextoSecundario
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(tab.icono, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tab.titulo, fontWeight = if (activa) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }

                    // Acciones Rápidas a la Derecha
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { mostrarNuevoArticulo = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Morado),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Nuevo Artículo", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { mostrarCategorias = true },
                            modifier = Modifier.background(FondoCardElevado, RoundedCornerShape(8.dp)).size(38.dp)
                        ) {
                            Icon(Icons.Default.Category, "Categorías", tint = TextoPrimario, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { mostrarAjustes = true },
                            modifier = Modifier.background(FondoCardElevado, RoundedCornerShape(8.dp)).size(38.dp)
                        ) {
                            Icon(Icons.Default.Settings, "Ajustes", tint = TextoPrimario, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // CONTENIDO PRINCIPAL SEGÚN LA PESTAÑA ACTIVA
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (pestanaActiva) {
                    DesktopNavTab.INVENTARIO -> {
                        DesktopInventarioScreen(
                            viewModel = viewModel,
                            onNuevoArticulo = { mostrarNuevoArticulo = true },
                            onEditarArticulo = { art -> articuloAEditar = art },
                            onVerCategorias = { mostrarCategorias = true },
                            onAbrirAjustes = { mostrarAjustes = true }
                        )
                    }
                    DesktopNavTab.VENTA_RAPIDA -> {
                        DesktopVentaRapidaScreen(viewModel = viewModel)
                    }
                    DesktopNavTab.HISTORIAL -> {
                        DesktopHistorialVentasScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // DIÁLOGOS
        if (mostrarNuevoArticulo) {
            DesktopEditarArticuloDialog(
                viewModel = viewModel,
                articuloExistente = null,
                onCerrar = { mostrarNuevoArticulo = false }
            )
        }

        if (articuloAEditar != null) {
            DesktopEditarArticuloDialog(
                viewModel = viewModel,
                articuloExistente = articuloAEditar,
                onCerrar = { articuloAEditar = null }
            )
        }

        if (mostrarCategorias) {
            DesktopCategoriasDialog(
                viewModel = viewModel,
                onCerrar = { mostrarCategorias = false }
            )
        }

        if (mostrarAjustes) {
            DesktopAjustesDialog(
                viewModel = viewModel,
                onCerrar = { mostrarAjustes = false }
            )
        }
    }
}
