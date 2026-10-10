package com.example.gestorstock.data.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "ventas")
data class Venta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long = System.currentTimeMillis(),
    val totalArticulos: Int,
    val totalPrecio: Double,
    val pagoCon: Double? = null,
    val cambio: Double? = null,
    val metodoPago: String = "Efectivo",
    val notas: String = "",
    val estado: String = "COMPLETADA" // COMPLETADA o ANULADA
)

@Entity(
    tableName = "lineas_venta",
    foreignKeys = [
        ForeignKey(
            entity = Venta::class,
            parentColumns = ["id"],
            childColumns = ["ventaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ventaId")]
)
data class LineaVenta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    @Embedded val venta: Venta,
    @Relation(
        parentColumn = "id",
        entityColumn = "ventaId"
    )
    val lineas: List<LineaVenta>
)
