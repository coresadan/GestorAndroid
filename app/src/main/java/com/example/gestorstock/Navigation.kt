package com.example.gestorstock

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.gestorstock.ui.StockViewModel
import com.example.gestorstock.ui.articulo.EditarArticuloScreen
import com.example.gestorstock.ui.categorias.CategoriasScreen
import com.example.gestorstock.ui.historial.HistorialVentasScreen
import com.example.gestorstock.ui.inventario.InventarioScreen
import com.example.gestorstock.ui.venta.VentaRapidaScreen

@Composable
fun AppNavigation(backStack: MutableList<NavKey>) {
    val viewModel: StockViewModel = viewModel()

    NavDisplay(
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLast() },
        transitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        popTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        predictivePopTransitionSpec = { EnterTransition.None togetherWith ExitTransition.None },
        entryProvider = entryProvider {
            entry<InventarioRoute> {
                InventarioScreen(
                    viewModel = viewModel,
                    onNuevoArticulo = { backStack.add(EditarArticuloRoute(-1)) },
                    onEditarArticulo = { id -> backStack.add(EditarArticuloRoute(id)) },
                    onVerCategorias = { backStack.add(CategoriasRoute) },
                    onVentaRapida = { backStack.add(VentaRapidaRoute) },
                    onVerHistorial = { backStack.add(HistorialVentasRoute) }
                )
            }
            entry<EditarArticuloRoute> { route ->
                EditarArticuloScreen(
                    viewModel = viewModel,
                    articuloId = route.articuloId,
                    onVolver = { if (backStack.size > 1) backStack.removeLast() }
                )
            }
            entry<CategoriasRoute> {
                CategoriasScreen(
                    viewModel = viewModel,
                    onVolver = { if (backStack.size > 1) backStack.removeLast() }
                )
            }
            entry<VentaRapidaRoute> {
                VentaRapidaScreen(
                    viewModel = viewModel,
                    onVolver = { if (backStack.size > 1) backStack.removeLast() }
                )
            }
            entry<HistorialVentasRoute> {
                HistorialVentasScreen(
                    viewModel = viewModel,
                    onVolver = { if (backStack.size > 1) backStack.removeLast() }
                )
            }
        }
    )
}
