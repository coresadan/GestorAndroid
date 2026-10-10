package com.example.gestorstock.data.db

import androidx.room.*
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.ArticuloConCategoria
import com.example.gestorstock.data.model.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticuloDao {

    @Query("SELECT * FROM articulos ORDER BY nombre ASC")
    fun getAll(): Flow<List<Articulo>>

    @Query("""
        SELECT * FROM articulos
        WHERE (:categoriaId IS NULL OR categoriaId = :categoriaId)
        AND (
            :busqueda IS NULL 
            OR :busqueda = '' 
            OR nombre LIKE '%' || :busqueda || '%' 
            OR (descripcion IS NOT NULL AND descripcion LIKE '%' || :busqueda || '%')
        )
        ORDER BY nombre ASC
    """)
    fun buscar(busqueda: String, categoriaId: Int?): Flow<List<Articulo>>

    @Query("SELECT * FROM articulos ORDER BY nombre ASC")
    fun getAllSimple(): Flow<List<Articulo>>

    @Query("SELECT * FROM articulos WHERE id = :id")
    suspend fun getById(id: Int): Articulo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(articulo: Articulo): Long

    @Update
    suspend fun actualizar(articulo: Articulo)

    @Delete
    suspend fun eliminar(articulo: Articulo)

    @Query("UPDATE articulos SET stock = stock + :delta WHERE id = :id")
    suspend fun actualizarStock(id: Int, delta: Int)
}
