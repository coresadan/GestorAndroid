package com.example.gestorstock.data.db

import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

class DesktopDatabase private constructor() {

    private val dbPath: String
    private val _dataVersion = MutableStateFlow(System.currentTimeMillis())
    val dataVersion: StateFlow<Long> = _dataVersion.asStateFlow()

    init {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        val appDir = File(appData, "GestorStock").apply { if (!exists()) mkdirs() }
        val dbFile = File(appDir, "gestor_stock.db")
        dbPath = dbFile.absolutePath

        // Cargar driver SQLite
        Class.forName("org.sqlite.JDBC")

        initTables()
    }

    private fun getConnection(): Connection {
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        conn.createStatement().use { stmt ->
            stmt.execute("PRAGMA foreign_keys = ON;")
            stmt.execute("PRAGMA journal_mode = WAL;")
        }
        return conn
    }

    private fun notifyDataChanged() {
        _dataVersion.value = System.currentTimeMillis()
    }

    private fun initTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                // Categorias
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS categorias (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        colorHex TEXT NOT NULL DEFAULT '#E91E63'
                    );
                """.trimIndent())

                // Articulos
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS articulos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nombre TEXT NOT NULL,
                        descripcion TEXT NOT NULL DEFAULT '',
                        precio REAL NOT NULL DEFAULT 0.0,
                        precioCosto REAL NOT NULL DEFAULT 0.0,
                        stock INTEGER NOT NULL DEFAULT 0,
                        stockMinimo INTEGER NOT NULL DEFAULT 2,
                        unidadMedida TEXT NOT NULL DEFAULT 'pza',
                        fechaCaducidad INTEGER DEFAULT NULL,
                        fotoUri TEXT DEFAULT NULL,
                        categoriaId INTEGER DEFAULT NULL,
                        codigoBarras TEXT DEFAULT NULL,
                        fechaCreacion INTEGER NOT NULL,
                        FOREIGN KEY(categoriaId) REFERENCES categorias(id) ON DELETE SET NULL
                    );
                """.trimIndent())
                stmt.execute("CREATE INDEX IF NOT EXISTS index_articulos_categoriaId ON articulos(categoriaId);")
                stmt.execute("CREATE INDEX IF NOT EXISTS index_articulos_codigoBarras ON articulos(codigoBarras);")

                // Ventas
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS ventas (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        fecha INTEGER NOT NULL,
                        totalArticulos INTEGER NOT NULL,
                        totalPrecio REAL NOT NULL,
                        pagoCon REAL DEFAULT NULL,
                        cambio REAL DEFAULT NULL,
                        metodoPago TEXT NOT NULL DEFAULT 'Efectivo',
                        notas TEXT NOT NULL DEFAULT '',
                        estado TEXT NOT NULL DEFAULT 'COMPLETADA'
                    );
                """.trimIndent())

                // Lineas de venta
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS lineas_venta (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ventaId INTEGER NOT NULL,
                        articuloId INTEGER DEFAULT NULL,
                        nombreArticulo TEXT NOT NULL,
                        precioUnitario REAL NOT NULL,
                        cantidad INTEGER NOT NULL,
                        subtotal REAL NOT NULL,
                        fotoUri TEXT DEFAULT NULL,
                        categoriaNombre TEXT DEFAULT NULL,
                        FOREIGN KEY(ventaId) REFERENCES ventas(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                stmt.execute("CREATE INDEX IF NOT EXISTS index_lineas_venta_ventaId ON lineas_venta(ventaId);")
            }
        }
    }

    // --- Categorías ---
    fun getCategorias(): List<Categoria> {
        val list = mutableListOf<Categoria>()
        getConnection().use { conn ->
            val sql = "SELECT id, nombre, colorHex FROM categorias ORDER BY nombre ASC"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(
                        Categoria(
                            id = rs.getInt("id"),
                            nombre = rs.getString("nombre"),
                            colorHex = rs.getString("colorHex")
                        )
                    )
                }
            }
        }
        return list
    }

    fun getCategoriaById(id: Int): Categoria? {
        getConnection().use { conn ->
            val sql = "SELECT id, nombre, colorHex FROM categorias WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return Categoria(
                        id = rs.getInt("id"),
                        nombre = rs.getString("nombre"),
                        colorHex = rs.getString("colorHex")
                    )
                }
            }
        }
        return null
    }

    fun insertarCategoria(categoria: Categoria): Long {
        var newId = 0L
        getConnection().use { conn ->
            val sql = "INSERT INTO categorias (nombre, colorHex) VALUES (?, ?)"
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, categoria.nombre)
                stmt.setString(2, categoria.colorHex)
                stmt.executeUpdate()
                val rs = stmt.generatedKeys
                if (rs.next()) {
                    newId = rs.getLong(1)
                }
            }
        }
        notifyDataChanged()
        return newId
    }

    fun actualizarCategoria(categoria: Categoria) {
        getConnection().use { conn ->
            val sql = "UPDATE categorias SET nombre = ?, colorHex = ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, categoria.nombre)
                stmt.setString(2, categoria.colorHex)
                stmt.setInt(3, categoria.id)
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    fun eliminarCategoria(categoriaId: Int) {
        getConnection().use { conn ->
            val sql = "DELETE FROM categorias WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, categoriaId)
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    // --- Artículos ---
    private fun mapArticulo(rs: ResultSet): Articulo {
        val caducidadObj = rs.getObject("fechaCaducidad")
        val caducidad = if (caducidadObj != null) rs.getLong("fechaCaducidad") else null

        val catIdObj = rs.getObject("categoriaId")
        val catId = if (catIdObj != null) rs.getInt("categoriaId") else null

        return Articulo(
            id = rs.getInt("id"),
            nombre = rs.getString("nombre"),
            descripcion = rs.getString("descripcion"),
            precio = rs.getDouble("precio"),
            precioCosto = rs.getDouble("precioCosto"),
            stock = rs.getInt("stock"),
            stockMinimo = rs.getInt("stockMinimo"),
            unidadMedida = rs.getString("unidadMedida"),
            fechaCaducidad = caducidad,
            fotoUri = rs.getString("fotoUri"),
            categoriaId = catId,
            codigoBarras = rs.getString("codigoBarras"),
            fechaCreacion = rs.getLong("fechaCreacion")
        )
    }

    fun getArticulos(): List<Articulo> {
        val list = mutableListOf<Articulo>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM articulos ORDER BY nombre ASC"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(mapArticulo(rs))
                }
            }
        }
        return list
    }

    fun buscarArticulos(busqueda: String, categoriaId: Int?): List<Articulo> {
        val list = mutableListOf<Articulo>()
        getConnection().use { conn ->
            val sb = StringBuilder("SELECT * FROM articulos WHERE 1=1")
            val params = mutableListOf<Any>()

            if (categoriaId != null) {
                sb.append(" AND categoriaId = ?")
                params.add(categoriaId)
            }
            if (busqueda.isNotBlank()) {
                sb.append(" AND (nombre LIKE ? OR descripcion LIKE ? OR codigoBarras LIKE ?)")
                val q = "%$busqueda%"
                params.add(q)
                params.add(q)
                params.add(q)
            }
            sb.append(" ORDER BY nombre ASC")

            conn.prepareStatement(sb.toString()).use { stmt ->
                params.forEachIndexed { index, param ->
                    when (param) {
                        is Int -> stmt.setInt(index + 1, param)
                        is String -> stmt.setString(index + 1, param)
                    }
                }
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    list.add(mapArticulo(rs))
                }
            }
        }
        return list
    }

    fun getArticuloById(id: Int): Articulo? {
        getConnection().use { conn ->
            val sql = "SELECT * FROM articulos WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    return mapArticulo(rs)
                }
            }
        }
        return null
    }

    fun insertarArticulo(articulo: Articulo): Long {
        var newId = 0L
        getConnection().use { conn ->
            val sql = """
                INSERT INTO articulos (
                    nombre, descripcion, precio, precioCosto, stock, stockMinimo,
                    unidadMedida, fechaCaducidad, fotoUri, categoriaId, codigoBarras, fechaCreacion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                stmt.setString(1, articulo.nombre)
                stmt.setString(2, articulo.descripcion)
                stmt.setDouble(3, articulo.precio)
                stmt.setDouble(4, articulo.precioCosto)
                stmt.setInt(5, articulo.stock)
                stmt.setInt(6, articulo.stockMinimo)
                stmt.setString(7, articulo.unidadMedida)
                if (articulo.fechaCaducidad != null) stmt.setLong(8, articulo.fechaCaducidad) else stmt.setNull(8, java.sql.Types.BIGINT)
                stmt.setString(9, articulo.fotoUri)
                if (articulo.categoriaId != null) stmt.setInt(10, articulo.categoriaId) else stmt.setNull(10, java.sql.Types.INTEGER)
                stmt.setString(11, articulo.codigoBarras)
                stmt.setLong(12, articulo.fechaCreacion)

                stmt.executeUpdate()
                val rs = stmt.generatedKeys
                if (rs.next()) {
                    newId = rs.getLong(1)
                }
            }
        }
        notifyDataChanged()
        return newId
    }

    fun actualizarArticulo(articulo: Articulo) {
        getConnection().use { conn ->
            val sql = """
                UPDATE articulos SET
                    nombre = ?, descripcion = ?, precio = ?, precioCosto = ?, stock = ?, stockMinimo = ?,
                    unidadMedida = ?, fechaCaducidad = ?, fotoUri = ?, categoriaId = ?, codigoBarras = ?
                WHERE id = ?
            """.trimIndent()
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, articulo.nombre)
                stmt.setString(2, articulo.descripcion)
                stmt.setDouble(3, articulo.precio)
                stmt.setDouble(4, articulo.precioCosto)
                stmt.setInt(5, articulo.stock)
                stmt.setInt(6, articulo.stockMinimo)
                stmt.setString(7, articulo.unidadMedida)
                if (articulo.fechaCaducidad != null) stmt.setLong(8, articulo.fechaCaducidad) else stmt.setNull(8, java.sql.Types.BIGINT)
                stmt.setString(9, articulo.fotoUri)
                if (articulo.categoriaId != null) stmt.setInt(10, articulo.categoriaId) else stmt.setNull(10, java.sql.Types.INTEGER)
                stmt.setString(11, articulo.codigoBarras)
                stmt.setInt(12, articulo.id)

                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    fun eliminarArticulo(id: Int) {
        getConnection().use { conn ->
            val sql = "DELETE FROM articulos WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, id)
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    fun actualizarStock(id: Int, delta: Int) {
        getConnection().use { conn ->
            val sql = "UPDATE articulos SET stock = stock + ? WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setInt(1, delta)
                stmt.setInt(2, id)
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    // --- Ventas ---
    fun getVentas(): List<VentaConDetalles> {
        val list = mutableListOf<VentaConDetalles>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM ventas ORDER BY fecha DESC"
            conn.prepareStatement(sql).use { stmt ->
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val venta = mapVenta(rs)
                    val lineas = getLineasVenta(conn, venta.id)
                    list.add(VentaConDetalles(venta, lineas))
                }
            }
        }
        return list
    }

    fun getVentasRango(inicio: Long, fin: Long): List<VentaConDetalles> {
        val list = mutableListOf<VentaConDetalles>()
        getConnection().use { conn ->
            val sql = "SELECT * FROM ventas WHERE fecha >= ? AND fecha <= ? ORDER BY fecha DESC"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, inicio)
                stmt.setLong(2, fin)
                val rs = stmt.executeQuery()
                while (rs.next()) {
                    val venta = mapVenta(rs)
                    val lineas = getLineasVenta(conn, venta.id)
                    list.add(VentaConDetalles(venta, lineas))
                }
            }
        }
        return list
    }

    fun getVentaById(id: Long): VentaConDetalles? {
        getConnection().use { conn ->
            val sql = "SELECT * FROM ventas WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, id)
                val rs = stmt.executeQuery()
                if (rs.next()) {
                    val venta = mapVenta(rs)
                    val lineas = getLineasVenta(conn, venta.id)
                    return VentaConDetalles(venta, lineas)
                }
            }
        }
        return null
    }

    private fun mapVenta(rs: ResultSet): Venta {
        val pagoConObj = rs.getObject("pagoCon")
        val pagoCon = if (pagoConObj != null) rs.getDouble("pagoCon") else null

        val cambioObj = rs.getObject("cambio")
        val cambio = if (cambioObj != null) rs.getDouble("cambio") else null

        return Venta(
            id = rs.getLong("id"),
            fecha = rs.getLong("fecha"),
            totalArticulos = rs.getInt("totalArticulos"),
            totalPrecio = rs.getDouble("totalPrecio"),
            pagoCon = pagoCon,
            cambio = cambio,
            metodoPago = rs.getString("metodoPago"),
            notas = rs.getString("notas"),
            estado = rs.getString("estado")
        )
    }

    private fun getLineasVenta(conn: Connection, ventaId: Long): List<LineaVenta> {
        val list = mutableListOf<LineaVenta>()
        val sql = "SELECT * FROM lineas_venta WHERE ventaId = ? ORDER BY id ASC"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, ventaId)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val artIdObj = rs.getObject("articuloId")
                val artId = if (artIdObj != null) rs.getInt("articuloId") else null

                list.add(
                    LineaVenta(
                        id = rs.getLong("id"),
                        ventaId = rs.getLong("ventaId"),
                        articuloId = artId,
                        nombreArticulo = rs.getString("nombreArticulo"),
                        precioUnitario = rs.getDouble("precioUnitario"),
                        cantidad = rs.getInt("cantidad"),
                        subtotal = rs.getDouble("subtotal"),
                        fotoUri = rs.getString("fotoUri"),
                        categoriaNombre = rs.getString("categoriaNombre")
                    )
                )
            }
        }
        return list
    }

    fun insertarVenta(venta: Venta, lineas: List<LineaVenta>): Long {
        var ventaId = 0L
        getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val sqlVenta = """
                    INSERT INTO ventas (
                        fecha, totalArticulos, totalPrecio, pagoCon, cambio, metodoPago, notas, estado
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                    stmt.setLong(1, venta.fecha)
                    stmt.setInt(2, venta.totalArticulos)
                    stmt.setDouble(3, venta.totalPrecio)
                    if (venta.pagoCon != null) stmt.setDouble(4, venta.pagoCon) else stmt.setNull(4, java.sql.Types.DOUBLE)
                    if (venta.cambio != null) stmt.setDouble(5, venta.cambio) else stmt.setNull(5, java.sql.Types.DOUBLE)
                    stmt.setString(6, venta.metodoPago)
                    stmt.setString(7, venta.notas)
                    stmt.setString(8, venta.estado)

                    stmt.executeUpdate()
                    val rs = stmt.generatedKeys
                    if (rs.next()) {
                        ventaId = rs.getLong(1)
                    }
                }

                val sqlLinea = """
                    INSERT INTO lineas_venta (
                        ventaId, articuloId, nombreArticulo, precioUnitario, cantidad, subtotal, fotoUri, categoriaNombre
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                conn.prepareStatement(sqlLinea).use { stmt ->
                    for (linea in lineas) {
                        stmt.setLong(1, ventaId)
                        if (linea.articuloId != null) stmt.setInt(2, linea.articuloId) else stmt.setNull(2, java.sql.Types.INTEGER)
                        stmt.setString(3, linea.nombreArticulo)
                        stmt.setDouble(4, linea.precioUnitario)
                        stmt.setInt(5, linea.cantidad)
                        stmt.setDouble(6, linea.subtotal)
                        stmt.setString(7, linea.fotoUri)
                        stmt.setString(8, linea.categoriaNombre)
                        stmt.addBatch()
                    }
                    stmt.executeBatch()
                }

                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
        notifyDataChanged()
        return ventaId
    }

    fun anularVenta(ventaId: Long) {
        getConnection().use { conn ->
            val sql = "UPDATE ventas SET estado = 'ANULADA' WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, ventaId)
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    fun eliminarVenta(ventaId: Long) {
        getConnection().use { conn ->
            val sql = "DELETE FROM ventas WHERE id = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, ventaId)
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    fun eliminarTodoElHistorial() {
        getConnection().use { conn ->
            val sql = "DELETE FROM ventas"
            conn.prepareStatement(sql).use { stmt ->
                stmt.executeUpdate()
            }
        }
        notifyDataChanged()
    }

    companion object {
        @Volatile
        private var instance: DesktopDatabase? = null

        fun getInstance(): DesktopDatabase {
            return instance ?: synchronized(this) {
                instance ?: DesktopDatabase().also { instance = it }
            }
        }
    }
}
