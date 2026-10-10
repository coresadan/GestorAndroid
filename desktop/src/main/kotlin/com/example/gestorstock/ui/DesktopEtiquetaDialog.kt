package com.example.gestorstock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
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
import com.example.gestorstock.theme.*
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
fun DesktopEtiquetaDialog(
    nombreTienda: String,
    nombreArticulo: String,
    categoria: String?,
    codigo: String,
    precio: Double,
    unidadMedida: String,
    onCerrar: () -> Unit
) {
    var mensajeFeedback by remember { mutableStateOf<String?>(null) }

    val etiquetaBitmap = remember(nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida) {
        DesktopBarcodeLabelHelper.generarEtiquetaComposeBitmap(
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
            modifier = Modifier
                .width(620.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("🏷️ Etiqueta de Código de Barras", color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Imprime para anaquel o empaque del producto", color = TextoSecundario, fontSize = 12.sp)
                    }
                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Default.Close, null, tint = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Vista previa de la etiqueta
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = etiquetaBitmap,
                        contentDescription = "Vista previa de etiqueta",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (mensajeFeedback != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        mensajeFeedback!!,
                        color = Verde,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val fd = FileDialog(null as Frame?, "Guardar Etiqueta PNG", FileDialog.SAVE).apply {
                                file = "etiqueta_${codigo.replace(" ", "_")}.png"
                                isVisible = true
                            }
                            if (fd.directory != null && fd.file != null) {
                                val dest = File(fd.directory, fd.file)
                                val guardado = DesktopBarcodeLabelHelper.guardarEtiqueta(
                                    dest, nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida
                                )
                                if (guardado) {
                                    mensajeFeedback = "¡Etiqueta guardada en: ${dest.name}!"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Guardar PNG")
                    }

                    Button(
                        onClick = {
                            val impreso = DesktopBarcodeLabelHelper.imprimirEtiqueta(
                                nombreTienda, nombreArticulo, categoria, codigo, precio, unidadMedida
                            )
                            if (impreso) {
                                mensajeFeedback = "¡Enviado a la impresora correctamente!"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Imprimir Etiqueta", color = Color.White)
                    }
                }
            }
        }
    }
}
