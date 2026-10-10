package com.example.gestorstock.data.model

data class Venta(
    val id: Long = 0,
    val fecha: Long = System.currentTimeMillis(),
    val totalArticulos: Int,
    val totalPrecio: Double,
    val pagoCon: Double? = null,
    val cambio: Double? = null,
    val metodoPago: String = "Efectivo",
    val notas: String = "",
    val estado: String = "COMPLETADA"
)

data class LineaVenta(
    val id: Long = 0,
    val ventaId: Long,
    val articuloId: Int?,
    val nombreArticulo: String,
    val precioUnitario: Double,
    val cantidad: Int,
    val subtotal: Double,
    val fotoUri: String? = null,
    val categoriaNombre: String? = null
)

data class VentaConDetalles(
    val venta: Venta,
    val lineas: List<LineaVenta>
)
