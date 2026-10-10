package com.example.gestorstock.ui.main

import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArticuloModelTest {
    @Test
    fun articulo_defaultValues() {
        val articulo = Articulo(nombre = "Amigurumi Oso", precio = 150.0)
        assertEquals("Amigurumi Oso", articulo.nombre)
        assertEquals(150.0, articulo.precio, 0.001)
        assertEquals(0, articulo.stock)
        assertNull(articulo.categoriaId)
    }

    @Test
    fun articulo_conCategoria() {
        val categoria = Categoria(id = 1, nombre = "Amigurumis", colorHex = "#FF6B6B")
        val articulo = Articulo(nombre = "Amigurumi Gato", precio = 200.0, categoriaId = categoria.id)
        assertEquals(1, articulo.categoriaId)
    }
}
