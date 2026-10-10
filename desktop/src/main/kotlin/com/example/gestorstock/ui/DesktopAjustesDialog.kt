package com.example.gestorstock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gestorstock.data.DesktopDataExportHelper
import com.example.gestorstock.desktop.DesktopStockViewModel
import com.example.gestorstock.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DesktopAjustesDialog(
    viewModel: DesktopStockViewModel,
    onCerrar: () -> Unit
) {
    val nombreTienda by viewModel.nombreTienda.collectAsState()
    val logoUri by viewModel.logoTiendaUri.collectAsState()
    val todosArticulos by viewModel.todosArticulos.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val ventas by viewModel.ventas.collectAsState()

    var nombreTemp by remember(nombreTienda) { mutableStateOf(nombreTienda) }
    var mensajeEstado by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier
                .width(620.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("⚙️ Configuración y Copias de Seguridad", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Personaliza tu negocio y respalda tus datos", color = TextoSecundario, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, null, tint = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Sección 1: Datos de la Tienda
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Identidad de la Tienda", color = RosaClaro, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Logo
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(FondoCardElevado),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoUri != null) {
                                    ArticuloImage(fotoUri = logoUri, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Default.Storefront, null, tint = RosaClaro, modifier = Modifier.size(28.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = nombreTemp,
                                    onValueChange = { nombreTemp = it },
                                    label = { Text("Nombre de la Tienda") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Rosa,
                                        unfocusedBorderColor = Color(0xFF3D2A54),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = TextoPrimario
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val fd = FileDialog(null as Frame?, "Seleccionar Logo de la Tienda", FileDialog.LOAD).apply {
                                        isVisible = true
                                    }
                                    if (fd.directory != null && fd.file != null) {
                                        val archivo = File(fd.directory, fd.file)
                                        viewModel.guardarLogoTienda(archivo)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Morado)
                            ) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cambiar Logo")
                            }

                            if (logoUri != null) {
                                OutlinedButton(
                                    onClick = { viewModel.eliminarLogoTienda() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rojo)
                                ) {
                                    Text("Quitar Logo")
                                }
                            }

                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = {
                                    viewModel.setNombreTienda(nombreTemp)
                                    mensajeEstado = Pair(true, "¡Nombre de tienda guardado correctamente!")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                            ) {
                                Text("Guardar Nombre")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Sección 2: Respaldos y Exportación
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = FondoOscuro)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Exportación y Copias de Seguridad", color = RosaClaro, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "Exporta tus datos para Excel o crea respaldos compatibles con la versión de Android.",
                            color = TextoSecundario,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        // Botones Excel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                    val fd = FileDialog(null as Frame?, "Exportar Inventario Excel (CSV)", FileDialog.SAVE).apply {
                                        file = "inventario_gestorstock_${sdf.format(Date())}.csv"
                                        isVisible = true
                                    }
                                    if (fd.directory != null && fd.file != null) {
                                        val dest = File(fd.directory, fd.file)
                                        DesktopDataExportHelper.exportarInventarioCsv(dest, todosArticulos, categorias)
                                        mensajeEstado = Pair(true, "Inventario exportado en: ${dest.name}")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.TableChart, null, tint = Verde, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Inventario a Excel", color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                    val fd = FileDialog(null as Frame?, "Exportar Ventas Excel (CSV)", FileDialog.SAVE).apply {
                                        file = "ventas_gestorstock_${sdf.format(Date())}.csv"
                                        isVisible = true
                                    }
                                    if (fd.directory != null && fd.file != null) {
                                        val dest = File(fd.directory, fd.file)
                                        DesktopDataExportHelper.exportarVentasCsv(dest, ventas)
                                        mensajeEstado = Pair(true, "Ventas exportadas en: ${dest.name}")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ReceiptLong, null, tint = Verde, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ventas a Excel", color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Botones JSON Copia Completa
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                    val fd = FileDialog(null as Frame?, "Crear Respaldo Completo (.JSON)", FileDialog.SAVE).apply {
                                        file = "backup_gestorstock_${sdf.format(Date())}.json"
                                        isVisible = true
                                    }
                                    if (fd.directory != null && fd.file != null) {
                                        val dest = File(fd.directory, fd.file)
                                        DesktopDataExportHelper.exportarBackupCompletoJson(dest, todosArticulos, categorias, ventas)
                                        mensajeEstado = Pair(true, "Copia de seguridad guardada en: ${dest.name}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Morado)
                            ) {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Exportar Copia (.JSON)")
                            }

                            Button(
                                onClick = {
                                    val fd = FileDialog(null as Frame?, "Restaurar Copia (.JSON)", FileDialog.LOAD).apply {
                                        isVisible = true
                                    }
                                    if (fd.directory != null && fd.file != null) {
                                        val archivo = File(fd.directory, fd.file)
                                        val jsonStr = archivo.readText(Charsets.UTF_8)
                                        val contenido = DesktopDataExportHelper.parsearBackupJson(jsonStr)
                                        if (contenido != null) {
                                            viewModel.restaurarBackup(contenido) { exito, msg ->
                                                mensajeEstado = Pair(exito, msg)
                                            }
                                        } else {
                                            mensajeEstado = Pair(false, "El archivo JSON seleccionado no es válido.")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = FondoCardElevado)
                            ) {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Importar Copia (.JSON)")
                            }
                        }
                    }
                }

                if (mensajeEstado != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        mensajeEstado!!.second,
                        color = if (mensajeEstado!!.first) Verde else Rojo,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onCerrar,
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                    ) {
                        Text("Listo / Cerrar")
                    }
                }
            }
        }
    }
}
