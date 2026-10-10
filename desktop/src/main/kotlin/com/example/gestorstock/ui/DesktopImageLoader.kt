package com.example.gestorstock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gestorstock.theme.FondoCardElevado
import com.example.gestorstock.theme.RosaClaro
import java.io.File
import javax.imageio.ImageIO

@Composable
fun ArticuloImage(
    fotoUri: String?,
    modifier: Modifier = Modifier,
    placeholderSize: Dp = 24.dp,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap: ImageBitmap? = remember(fotoUri) {
        if (fotoUri.isNullOrBlank()) null
        else try {
            val file = if (fotoUri.startsWith("file://")) {
                File(java.net.URI(fotoUri))
            } else {
                File(fotoUri)
            }
            if (file.exists() && file.isFile) {
                ImageIO.read(file)?.toComposeImageBitmap()
            } else null
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(FondoCardElevado),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Inventory2,
                contentDescription = null,
                tint = RosaClaro.copy(alpha = 0.5f),
                modifier = Modifier.size(placeholderSize)
            )
        }
    }
}
