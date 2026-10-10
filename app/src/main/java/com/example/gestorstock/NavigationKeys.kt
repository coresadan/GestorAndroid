package com.example.gestorstock

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object InventarioRoute : NavKey

@Serializable
data class EditarArticuloRoute(val articuloId: Int = -1) : NavKey

@Serializable
data object CategoriasRoute : NavKey

@Serializable
data object VentaRapidaRoute : NavKey

@Serializable
data object HistorialVentasRoute : NavKey
