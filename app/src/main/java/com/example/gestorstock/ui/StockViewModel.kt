package com.example.gestorstock.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestorstock.data.ImageStorageManager
import com.example.gestorstock.data.StockRepository
import com.example.gestorstock.data.TiendaPreferences
import com.example.gestorstock.data.db.AppDatabase
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FiltroState(
    val busqueda: String = "",
    val categoriaId: Int? = null,
    val soloStockBajo: Boolean = false,
    val proximosACaducar: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class StockViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    val repository = StockRepository(db.articuloDao(), db.categoriaDao(), db.ventaDao())
    private val tiendaPrefs = TiendaPreferences(app)
    private val imageManager = ImageStorageManager(app)

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

    fun guardarLogoTienda(uri: Uri) = viewModelScope.launch {
        val guardado = withContext(Dispatchers.IO) {
            tiendaPrefs.guardarLogoDesdeUri(uri)
        }
        _logoTiendaUri.value = guardado
    }

    fun eliminarLogoTienda() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            tiendaPrefs.eliminarLogo()
        }
        _logoTiendaUri.value = null
    }

    suspend fun guardarFotoArticulo(sourceUri: Uri): String? = withContext(Dispatchers.IO) {
        imageManager.guardarImagenArticulo(sourceUri)
    }

    suspend fun rotarFotoArticulo(uriString: String): String? = withContext(Dispatchers.IO) {
        imageManager.rotarImagenArticulo(uriString, 90f)
    }

    val categorias: StateFlow<List<Categoria>> = repository.getCategorias()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _filtro = MutableStateFlow(FiltroState())
    val filtro: StateFlow<FiltroState> = _filtro.asStateFlow()

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
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Para modo venta rápida, todos los artículos sin filtro
    val todosArticulos: StateFlow<List<Articulo>> = repository.getArticulos()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Historial de ventas registradas
    val ventas: StateFlow<List<VentaConDetalles>> = repository.getVentas()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun restaurarBackup(contenido: com.example.gestorstock.data.BackupContenido, onDone: (Boolean, String) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
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
                onDone(false, "Error al importar: ${e.message}")
            }
        }
    }

    fun buscarArticuloPorCodigo(codigo: String): Articulo? {
        return todosArticulos.value.find { it.codigoBarras == codigo }
    }

    // --- Artículos ---
    fun guardarArticulo(articulo: Articulo, onDone: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        var fotoFinal = articulo.fotoUri
        // Si la foto es un content:// URI externo, copiarla inmediatamente al almacenamiento interno permanente
        if (fotoFinal != null && fotoFinal.startsWith("content://")) {
            val persistente = imageManager.guardarImagenArticulo(Uri.parse(fotoFinal))
            if (persistente != null) {
                fotoFinal = persistente
            }
        }

        val articuloFinal = articulo.copy(fotoUri = fotoFinal)

        if (articuloFinal.id == 0) {
            repository.insertarArticulo(articuloFinal)
        } else {
            // Si se cambió la foto, eliminar la anterior si era local para no ocupar espacio
            val anterior = repository.getArticuloById(articuloFinal.id)
            if (anterior != null && anterior.fotoUri != null && anterior.fotoUri != fotoFinal) {
                imageManager.eliminarImagenArticulo(anterior.fotoUri)
            }
            repository.actualizarArticulo(articuloFinal)
        }
        withContext(Dispatchers.Main) {
            onDone()
        }
    }

    fun eliminarArticulo(articulo: Articulo) = viewModelScope.launch(Dispatchers.IO) {
        if (articulo.fotoUri != null) {
            imageManager.eliminarImagenArticulo(articulo.fotoUri)
        }
        repository.eliminarArticulo(articulo)
    }

    fun cambiarStock(id: Int, delta: Int) = viewModelScope.launch {
        repository.actualizarStock(id, delta)
    }

    // --- Registrar venta completa e historial ---
    fun registrarVentaCompleta(
        items: Map<Int, Int>,
        metodoPago: String = "Efectivo",
        pagoCon: Double? = null,
        cambio: Double? = null,
        notas: String = "",
        onDone: (Long) -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
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

    fun anularVenta(ventaConDetalles: VentaConDetalles) = viewModelScope.launch(Dispatchers.IO) {
        repository.anularVenta(ventaConDetalles.venta.id)
        // Reponer el stock de los artículos vendidos
        ventaConDetalles.lineas.forEach { linea ->
            linea.articuloId?.let { id ->
                repository.actualizarStock(id, linea.cantidad)
            }
        }
    }

    // --- Categorías ---
    fun guardarCategoria(categoria: Categoria, onDone: ((Long) -> Unit)? = null) = viewModelScope.launch(Dispatchers.IO) {
        val id = if (categoria.id == 0) repository.insertarCategoria(categoria)
        else {
            repository.actualizarCategoria(categoria)
            categoria.id.toLong()
        }
        withContext(Dispatchers.Main) {
            onDone?.invoke(id)
        }
    }

    fun eliminarCategoria(categoria: Categoria) = viewModelScope.launch {
        repository.eliminarCategoria(categoria)
    }

    suspend fun getArticuloById(id: Int): Articulo? = repository.getArticuloById(id)

    fun generarCodigoBarrasUnico(): String {
        var candidato: String
        do {
            val randomNum = (100000..999999).random()
            candidato = "ART-$randomNum"
        } while (todosArticulos.value.any { it.codigoBarras == candidato })
        return candidato
    }
}
