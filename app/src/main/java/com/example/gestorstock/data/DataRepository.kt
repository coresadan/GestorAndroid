package com.example.gestorstock.data

import com.example.gestorstock.data.db.ArticuloDao
import com.example.gestorstock.data.db.CategoriaDao
import com.example.gestorstock.data.db.VentaDao
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val articuloDao: ArticuloDao,
    private val categoriaDao: CategoriaDao,
    private val ventaDao: VentaDao
) {
    // --- Artículos ---
    fun getArticulos(): Flow<List<Articulo>> = articuloDao.getAllSimple()

    fun buscarArticulos(busqueda: String, categoriaId: Int?): Flow<List<Articulo>> =
        articuloDao.buscar(busqueda, categoriaId)

    suspend fun getArticuloById(id: Int): Articulo? = articuloDao.getById(id)

    suspend fun insertarArticulo(articulo: Articulo): Long = articuloDao.insertar(articulo)

    suspend fun actualizarArticulo(articulo: Articulo) = articuloDao.actualizar(articulo)

    suspend fun eliminarArticulo(articulo: Articulo) = articuloDao.eliminar(articulo)

    suspend fun actualizarStock(id: Int, delta: Int) = articuloDao.actualizarStock(id, delta)

    // --- Categorías ---
    fun getCategorias(): Flow<List<Categoria>> = categoriaDao.getAll()

    suspend fun insertarCategoria(categoria: Categoria): Long = categoriaDao.insertar(categoria)

    suspend fun actualizarCategoria(categoria: Categoria) = categoriaDao.actualizar(categoria)

    suspend fun eliminarCategoria(categoria: Categoria) = categoriaDao.eliminar(categoria)

    // --- Ventas e Historial ---
    fun getVentas(): Flow<List<VentaConDetalles>> = ventaDao.getAllVentasConDetalles()

    fun getVentasRango(inicio: Long, fin: Long): Flow<List<VentaConDetalles>> =
        ventaDao.getVentasRango(inicio, fin)

    suspend fun registrarVentaCompleta(venta: Venta, lineas: List<LineaVenta>): Long {
        val ventaId = ventaDao.insertarVenta(venta)
        val lineasConId = lineas.map { it.copy(ventaId = ventaId) }
        ventaDao.insertarLineas(lineasConId)
        return ventaId
    }

    suspend fun anularVenta(ventaId: Long) = ventaDao.anularVenta(ventaId)

    suspend fun eliminarVenta(ventaId: Long) = ventaDao.eliminarVenta(ventaId)
}
