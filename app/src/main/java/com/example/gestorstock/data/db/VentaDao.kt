package com.example.gestorstock.data.db

import androidx.room.*
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta
import com.example.gestorstock.data.model.VentaConDetalles
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {

    @Transaction
    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAllVentasConDetalles(): Flow<List<VentaConDetalles>>

    @Transaction
    @Query("SELECT * FROM ventas WHERE fecha >= :inicioDia AND fecha <= :finDia ORDER BY fecha DESC")
    fun getVentasRango(inicioDia: Long, finDia: Long): Flow<List<VentaConDetalles>>

    @Transaction
    @Query("SELECT * FROM ventas WHERE id = :id")
    suspend fun getVentaById(id: Long): VentaConDetalles?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarVenta(venta: Venta): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarLineas(lineas: List<LineaVenta>)

    @Query("UPDATE ventas SET estado = 'ANULADA' WHERE id = :ventaId")
    suspend fun anularVenta(ventaId: Long)

    @Query("DELETE FROM ventas WHERE id = :ventaId")
    suspend fun eliminarVenta(ventaId: Long)

    @Query("DELETE FROM ventas")
    suspend fun eliminarTodoElHistorial()
}
