package com.example.gestorstock.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.gestorstock.ui.DesktopApp
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1260.dp, 840.dp),
        position = WindowPosition.PlatformDefault
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "GestorStock - Control de Inventario y Ventas",
        icon = painterResource("icon.png")
    ) {
        window.minimumSize = Dimension(960, 620)

        val viewModel = remember { DesktopStockViewModel() }
        DesktopApp(viewModel = viewModel)
    }
}
