package com.example.gestorstock.data.model

data class Articulo(
    val id: Int = 0,
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
