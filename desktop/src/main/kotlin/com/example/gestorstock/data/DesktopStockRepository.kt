package com.example.gestorstock.data

import com.example.gestorstock.data.db.DesktopDatabase
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DesktopStockRepository(private val db: DesktopDatabase) {

    // --- Artículos ---
    fun getArticulos(): Flow<List<Articulo>> = db.dataVersion.map {
        db.getArticulos()
    }

    fun buscarArticulos(busqueda: String, categoriaId: Int?): Flow<List<Articulo>> = db.dataVersion.map {
        db.buscarArticulos(busqueda, categoriaId)
    }

    fun getArticuloById(id: Int): Articulo? = db.getArticuloById(id)

    fun insertarArticulo(articulo: Articulo): Long = db.insertarArticulo(articulo)

    fun actualizarArticulo(articulo: Articulo) = db.actualizarArticulo(articulo)

    fun eliminarArticulo(id: Int) = db.eliminarArticulo(id)

    fun actualizarStock(id: Int, delta: Int) = db.actualizarStock(id, delta)

    // --- Categorías ---
    fun getCategorias(): Flow<List<Categoria>> = db.dataVersion.map {
        db.getCategorias()
    }

    fun insertarCategoria(categoria: Categoria): Long = db.insertarCategoria(categoria)

    fun actualizarCategoria(categoria: Categoria) = db.actualizarCategoria(categoria)

    fun eliminarCategoria(categoriaId: Int) = db.eliminarCategoria(categoriaId)

    // --- Ventas e Historial ---
    fun getVentas(): Flow<List<VentaConDetalles>> = db.dataVersion.map {
        db.getVentas()
    }

    fun getVentasRango(inicio: Long, fin: Long): Flow<List<VentaConDetalles>> = db.dataVersion.map {
        db.getVentasRango(inicio, fin)
    }

    fun registrarVentaCompleta(venta: Venta, lineas: List<LineaVenta>): Long {
        return db.insertarVenta(venta, lineas)
    }

    fun anularVenta(ventaId: Long) = db.anularVenta(ventaId)

    fun eliminarVenta(ventaId: Long) = db.eliminarVenta(ventaId)
}
