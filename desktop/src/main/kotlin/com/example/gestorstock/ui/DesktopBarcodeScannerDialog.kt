package com.example.gestorstock.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gestorstock.theme.*
import kotlinx.coroutines.delay
import java.awt.Toolkit

@Composable
fun DesktopBarcodeScannerDialog(
    titulo: String = "Lector de Códigos de Barras / Pistola USB",
    subtitulo: String = "Escanea con la pistola lectora o escribe el código y presiona Enter",
    modoContinuoInicial: Boolean = false,
    onCodigoDetectado: (String) -> Unit,
    onCerrar: () -> Unit
) {
    var codigoTexto by remember { mutableStateOf("") }
    var modoContinuo by remember { mutableStateOf(modoContinuoInicial) }
    var avisoDetectado by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(avisoDetectado) {
        if (avisoDetectado != null) {
            delay(1200)
            avisoDetectado = null
        }
    }

    fun procesarCodigo(cod: String) {
        val limpio = cod.trim()
        if (limpio.isNotBlank()) {
            try {
                Toolkit.getDefaultToolkit().beep()
            } catch (_: Exception) {}
            avisoDetectado = limpio
            onCodigoDetectado(limpio)
            codigoTexto = ""
            if (!modoContinuo) {
                onCerrar()
            }
        }
    }

    Dialog(onDismissRequest = onCerrar) {
        Card(
            modifier = Modifier
                .width(520.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
            border = androidx.compose.foundation.BorderStroke(1.dp, Brush.linearGradient(listOf(Rosa, Morado)))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column {
                            Text(titulo, color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                if (modoContinuo) "Modo continuo activado (Pistola TPV)" else subtitulo,
                                color = if (modoContinuo) RosaClaro else TextoSecundario,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Filled.Close, null, tint = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Selector de modo continuo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FondoOscuro, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (modoContinuo) Icons.Filled.FlashOn else Icons.Outlined.Repeat,
                            contentDescription = null,
                            tint = if (modoContinuo) Rosa else TextoDeshabilitado,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Modo Pistola Continuo (No cierra la ventana tras escanear)",
                            fontSize = 12.sp,
                            color = if (modoContinuo) TextoPrimario else TextoSecundario,
                            fontWeight = if (modoContinuo) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Switch(
                        checked = modoContinuo,
                        onCheckedChange = { modoContinuo = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Rosa,
                            uncheckedThumbColor = TextoDeshabilitado,
                            uncheckedTrackColor = FondoCard
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Área de escaneo con campo de texto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FondoOscuro, RoundedCornerShape(16.dp))
                        .border(
                            2.dp,
                            if (avisoDetectado != null) androidx.compose.ui.graphics.SolidColor(Verde) else Brush.linearGradient(listOf(Rosa, Morado)),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "APUNTA LA PISTOLA O ESCRIBE AQUÍ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RosaClaro
                        )

                        OutlinedTextField(
                            value = codigoTexto,
                            onValueChange = { codigoTexto = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.type == KeyEventType.KeyUp && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                                        procesarCodigo(codigoTexto)
                                        true
                                    } else false
                                },
                            placeholder = { Text("Ej: ART-123456 o 75010304...", color = TextoDeshabilitado) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Rosa,
                                unfocusedBorderColor = Color(0xFF3D2A54),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = TextoPrimario
                            )
                        )

                        Text(
                            "Compatible con cualquier lector de códigos de barras USB / Bluetooth",
                            fontSize = 11.sp,
                            color = TextoSecundario
                        )
                    }
                }

                if (avisoDetectado != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Verde.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Verde)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Verde, modifier = Modifier.size(18.dp))
                            Text(
                                "¡Código detectado con éxito: $avisoDetectado!",
                                color = Verde,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCerrar,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoSecundario)
                    ) {
                        Text("Cerrar")
                    }

                    Button(
                        onClick = { procesarCodigo(codigoTexto) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                    ) {
                        Text("Aceptar Código", color = Color.White)
                    }
                }
            }
        }
    }
}
