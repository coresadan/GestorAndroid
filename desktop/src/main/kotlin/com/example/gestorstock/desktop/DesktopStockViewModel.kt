package com.example.gestorstock.desktop

import com.example.gestorstock.data.DesktopBackupContenido
import com.example.gestorstock.data.DesktopImageManager
import com.example.gestorstock.data.DesktopPreferences
import com.example.gestorstock.data.DesktopStockRepository
import com.example.gestorstock.data.db.DesktopDatabase
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DesktopFiltroState(
    val busqueda: String = "",
    val categoriaId: Int? = null,
    val soloStockBajo: Boolean = false,
    val proximosACaducar: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopStockViewModel {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val db = DesktopDatabase.getInstance()
    val repository = DesktopStockRepository(db)
    val tiendaPrefs = DesktopPreferences()
    val imageManager = DesktopImageManager()

    // Nombre de la tienda
    private val _nombreTienda = MutableStateFlow(tiendaPrefs.getNombreTienda())
    val nombreTienda: StateFlow<String> = _nombreTienda.asStateFlow()

    // Logo de la tienda
    private val _logoTiendaUri = MutableStateFlow(tiendaPrefs.getLogoTiendaUri())
    val logoTiendaUri: StateFlow<String?> = _logoTiendaUri.asStateFlow()

    fun setNombreTienda(nombre: String) {
        tiendaPrefs.setNombreTienda(nombre)
        _nombreTienda.value = tiendaPrefs.getNombreTienda()
    }

    fun guardarLogoTienda(archivo: File) {
        val guardado = tiendaPrefs.guardarLogoDesdeArchivo(archivo)
        _logoTiendaUri.value = guardado
    }

    fun eliminarLogoTienda() {
        tiendaPrefs.eliminarLogo()
        _logoTiendaUri.value = null
    }

    fun guardarFotoArticulo(archivo: File): String? {
        return imageManager.guardarImagenArticulo(archivo)
    }

    fun rotarFotoArticulo(path: String): String? {
        return imageManager.rotarImagenArticulo(path, 90.0)
    }

    val categorias: StateFlow<List<Categoria>> = repository.getCategorias()
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    private val _filtro = MutableStateFlow(DesktopFiltroState())
    val filtro: StateFlow<DesktopFiltroState> = _filtro.asStateFlow()

    val articulos: StateFlow<List<Articulo>> = _filtro.flatMapLatest { f ->
        repository.buscarArticulos(f.busqueda, f.categoriaId).map { lista ->
            var filtrada = lista
            if (f.soloStockBajo) {
                filtrada = filtrada.filter { it.stock <= it.stockMinimo || it.stock == 0 }
            }
            if (f.proximosACaducar) {
                val hoy = System.currentTimeMillis()
                val limite15Dias = hoy + 15L * 24 * 60 * 60 * 1000
                filtrada = filtrada.filter { it.fechaCaducidad != null && it.fechaCaducidad <= limite15Dias }
            }
            filtrada
        }
    }.stateIn(scope, SharingStarted.Lazily, emptyList())

    val todosArticulos: StateFlow<List<Articulo>> = repository.getArticulos()
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    val ventas: StateFlow<List<VentaConDetalles>> = repository.getVentas()
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    fun setBusqueda(texto: String) {
        _filtro.update { it.copy(busqueda = texto) }
    }

    fun setCategoria(id: Int?) {
        _filtro.update { it.copy(categoriaId = id) }
    }

    fun toggleSoloStockBajo() {
        _filtro.update { it.copy(soloStockBajo = !it.soloStockBajo) }
    }

    fun toggleProximosACaducar() {
        _filtro.update { it.copy(proximosACaducar = !it.proximosACaducar) }
    }

    fun buscarArticuloPorCodigo(codigo: String): Articulo? {
        return todosArticulos.value.find { it.codigoBarras == codigo }
    }

    fun generarCodigoBarrasUnico(): String {
        var candidato: String
        do {
            val randomNum = (100000..999999).random()
            candidato = "ART-$randomNum"
        } while (todosArticulos.value.any { it.codigoBarras == candidato })
        return candidato
    }

    // --- Artículos ---
    fun guardarArticulo(articulo: Articulo, onDone: () -> Unit) = scope.launch(Dispatchers.IO) {
        val articuloFinal = articulo

        if (articuloFinal.id == 0) {
            repository.insertarArticulo(articuloFinal)
        } else {
            val anterior = repository.getArticuloById(articuloFinal.id)
            if (anterior != null && anterior.fotoUri != null && anterior.fotoUri != articuloFinal.fotoUri) {
                imageManager.eliminarImagenArticulo(anterior.fotoUri)
            }
            repository.actualizarArticulo(articuloFinal)
        }
        withContext(Dispatchers.Main) {
            onDone()
        }
    }

    fun eliminarArticulo(articulo: Articulo) = scope.launch(Dispatchers.IO) {
        if (articulo.fotoUri != null) {
            imageManager.eliminarImagenArticulo(articulo.fotoUri)
        }
        repository.eliminarArticulo(articulo.id)
    }

    fun cambiarStock(id: Int, delta: Int) = scope.launch(Dispatchers.IO) {
        repository.actualizarStock(id, delta)
    }

    // --- Ventas ---
    fun registrarVentaCompleta(
        items: Map<Int, Int>,
        metodoPago: String = "Efectivo",
        pagoCon: Double? = null,
        cambio: Double? = null,
        notas: String = "",
        onDone: (Long) -> Unit
    ) = scope.launch(Dispatchers.IO) {
        val listaArticulos = todosArticulos.value
        val listaCategorias = categorias.value

        var totalPrecio = 0.0
        var totalArticulos = 0
        val lineas = mutableListOf<LineaVenta>()

        items.forEach { (articuloId, cantidad) ->
            if (cantidad > 0) {
                val art = listaArticulos.find { it.id == articuloId }
                if (art != null) {
                    val cat = listaCategorias.find { it.id == art.categoriaId }
                    val subtotal = art.precio * cantidad
                    totalPrecio += subtotal
                    totalArticulos += cantidad
                    lineas.add(
                        LineaVenta(
                            ventaId = 0,
                            articuloId = art.id,
                            nombreArticulo = art.nombre,
                            precioUnitario = art.precio,
                            cantidad = cantidad,
                            subtotal = subtotal,
                            fotoUri = art.fotoUri,
                            categoriaNombre = cat?.nombre
                        )
                    )
                    // Descontar stock
                    repository.actualizarStock(articuloId, -cantidad)
                }
            }
        }

        val venta = Venta(
            fecha = System.currentTimeMillis(),
            totalArticulos = totalArticulos,
            totalPrecio = totalPrecio,
            pagoCon = pagoCon,
            cambio = cambio,
            metodoPago = metodoPago,
            notas = notas,
            estado = "COMPLETADA"
        )

        val ventaId = repository.registrarVentaCompleta(venta, lineas)
        withContext(Dispatchers.Main) {
            onDone(ventaId)
        }
    }

    fun anularVenta(ventaConDetalles: VentaConDetalles) = scope.launch(Dispatchers.IO) {
        repository.anularVenta(ventaConDetalles.venta.id)
        // Reponer stock
        ventaConDetalles.lineas.forEach { linea ->
            linea.articuloId?.let { id ->
                repository.actualizarStock(id, linea.cantidad)
            }
        }
    }

    fun eliminarVenta(ventaId: Long) = scope.launch(Dispatchers.IO) {
        repository.eliminarVenta(ventaId)
    }

    // --- Categorías ---
    fun guardarCategoria(categoria: Categoria, onDone: ((Long) -> Unit)? = null) = scope.launch(Dispatchers.IO) {
        val id = if (categoria.id == 0) repository.insertarCategoria(categoria)
        else {
            repository.actualizarCategoria(categoria)
            categoria.id.toLong()
        }
        withContext(Dispatchers.Main) {
            onDone?.invoke(id)
        }
    }

    fun eliminarCategoria(categoria: Categoria) = scope.launch(Dispatchers.IO) {
        repository.eliminarCategoria(categoria.id)
    }

    fun restaurarBackup(contenido: DesktopBackupContenido, onDone: (Boolean, String) -> Unit) = scope.launch(Dispatchers.IO) {
        try {
            contenido.categorias.forEach { cat ->
                repository.insertarCategoria(cat.copy(id = 0))
            }
            contenido.articulos.forEach { art ->
                repository.insertarArticulo(art.copy(id = 0))
            }
            withContext(Dispatchers.Main) {
                onDone(true, "Se importaron ${contenido.articulos.size} artículos y ${contenido.categorias.size} categorías correctamente.")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onDone(false, "Error al importar copia de seguridad: ${e.message}")
            }
        }
    }
}
