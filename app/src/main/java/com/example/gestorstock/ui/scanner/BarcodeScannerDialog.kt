package com.example.gestorstock.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.gestorstock.theme.*
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    titulo: String = "Escanear Código de Barras / QR",
    subtitulo: String = "Apunta la cámara al código del producto",
    modoContinuoInicial: Boolean = false,
    onCodigoDetectado: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var modoContinuo by remember { mutableStateOf(modoContinuoInicial) }
    var ultimoCodigo by remember { mutableStateOf<String?>(null) }
    var tiempoUltimoCodigo by remember { mutableStateOf(0L) }
    var avisoDetectado by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(avisoDetectado) {
        if (avisoDetectado != null) {
            delay(1200)
            avisoDetectado = null
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        tienePermiso = concedido
    }

    LaunchedEffect(Unit) {
        if (!tienePermiso) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    fun reproducirFeedback(codigo: String) {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 110)
        } catch (_: Exception) {}
        avisoDetectado = codigo
    }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 500.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = FondoCardElevado),
            border = androidx.compose.foundation.BorderStroke(1.dp, Brush.linearGradient(listOf(Rosa, Morado)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Brush.linearGradient(listOf(Rosa, Morado)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.QrCodeScanner, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(titulo, color = TextoPrimario, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                if (modoContinuo) "Modo continuo activado (Pistola TPV)" else subtitulo,
                                color = if (modoContinuo) RosaClaro else TextoSecundario,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(onClick = onCerrar) {
                        Icon(Icons.Filled.Close, null, tint = TextoSecundario)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Selector de modo continuo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FondoOscuro, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (modoContinuo) Icons.Filled.FlashOn else Icons.Outlined.Repeat,
                            contentDescription = null,
                            tint = if (modoContinuo) Rosa else TextoDeshabilitado,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "Modo Pistola Continuo",
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
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!tienePermiso) {
                    // Vista cuando no hay permisos
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(FondoOscuro, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(Icons.Outlined.VideocamOff, null, tint = Rojo, modifier = Modifier.size(48.dp))
                            Text(
                                "Se requiere permiso de cámara",
                                color = TextoPrimario,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "Permite el acceso a la cámara para escanear etiquetas y códigos de tus artículos.",
                                color = TextoSecundario,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Rosa)
                            ) {
                                Text("Conceder Permiso")
                            }
                        }
                    }
                } else {
                    // Vista de Cámara con visor animado
                    var linternaEncendida by remember { mutableStateOf(false) }
                    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
                    var yaDetectado by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraPreviewScanner(
                            onCodigoDetectado = { codigo ->
                                if (modoContinuo) {
                                    val ahora = System.currentTimeMillis()
                                    if (codigo != ultimoCodigo || (ahora - tiempoUltimoCodigo > 1300L)) {
                                        ultimoCodigo = codigo
                                        tiempoUltimoCodigo = ahora
                                        reproducirFeedback(codigo)
                                        onCodigoDetectado(codigo)
                                    }
                                } else {
                                    if (!yaDetectado) {
                                        yaDetectado = true
                                        reproducirFeedback(codigo)
                                        onCodigoDetectado(codigo)
                                    }
                                }
                            },
                            onCameraReady = { control ->
                                cameraControl = control
                            }
                        )

                        // Retícula / Guía de escaneo
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .border(2.dp, if (avisoDetectado != null) Verde else Rosa, RoundedCornerShape(16.dp))
                        )

                        // Línea láser animada
                        val infiniteTransition = rememberInfiniteTransition(label = "laser")
                        val laserOffset by infiniteTransition.animateFloat(
                            initialValue = -90f,
                            targetValue = 90f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laser_y"
                        )

                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .height(2.dp)
                                .offset(y = laserOffset.dp)
                                .background(Brush.horizontalGradient(listOf(Color.Transparent, if (avisoDetectado != null) Verde else RosaClaro, Color.Transparent)))
                        )

                        // Banner flotante cuando detecta código
                        if (avisoDetectado != null) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 16.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = Verde,
                                shadowElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(
                                        "Código: $avisoDetectado",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Botón de Linterna
                        IconButton(
                            onClick = {
                                linternaEncendida = !linternaEncendida
                                cameraControl?.enableTorch(linternaEncendida)
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .background(Color(0x88000000), CircleShape)
                        ) {
                            Icon(
                                if (linternaEncendida) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = "Linterna",
                                tint = if (linternaEncendida) Color.Yellow else Color.White
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Botón cerrar / listo
                Button(
                    onClick = onCerrar,
                    colors = ButtonDefaults.buttonColors(containerColor = if (modoContinuo) Rosa else FondoCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(if (modoContinuo) "Listo / Terminar Escaneo" else "Cerrar", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewScanner(
    onCodigoDetectado: (String) -> Unit,
    onCameraReady: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val options = remember {
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
    }
    val scanner = remember { BarcodeScanning.getClient(options) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            onCodigoDetectado(rawValue)
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraReady(camera.cameraControl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
