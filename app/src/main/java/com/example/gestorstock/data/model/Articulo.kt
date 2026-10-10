package com.example.gestorstock.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articulos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoriaId"),
        Index("codigoBarras")
    ]
)
data class Articulo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val descripcion: String = "",
    val precio: Double = 0.0,
    val precioCosto: Double = 0.0,
    val stock: Int = 0,
    val stockMinimo: Int = 2,
    val unidadMedida: String = "pza",
    val fechaCaducidad: Long? = null,
    val fotoUri: String? = null,
    val categoriaId: Int? = null,
    val codigoBarras: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis()
)
