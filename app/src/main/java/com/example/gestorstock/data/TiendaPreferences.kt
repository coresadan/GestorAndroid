package com.example.gestorstock.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

private const val PREFS_NAME = "gestor_prefs"
private const val KEY_NOMBRE_TIENDA = "nombre_tienda"
private const val KEY_LOGO_TIENDA = "logo_tienda_uri"
private const val DEFAULT_NOMBRE = "Mi Tiendita"

class TiendaPreferences(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNombreTienda(): String =
        prefs.getString(KEY_NOMBRE_TIENDA, DEFAULT_NOMBRE) ?: DEFAULT_NOMBRE

    fun setNombreTienda(nombre: String) {
        prefs.edit().putString(KEY_NOMBRE_TIENDA, nombre.trim().ifEmpty { DEFAULT_NOMBRE }).apply()
    }

    fun getLogoTiendaUri(): String? =
        prefs.getString(KEY_LOGO_TIENDA, null)

    fun setLogoTiendaUri(uriString: String?) {
        prefs.edit().putString(KEY_LOGO_TIENDA, uriString).apply()
    }

    fun guardarLogoDesdeUri(sourceUri: Uri): String? {
        return try {
            val destino = File(context.filesDir, "logo_tienda_${System.currentTimeMillis()}.png")
            // Eliminar logos anteriores si existen
            context.filesDir.listFiles { file -> file.name.startsWith("logo_tienda_") }?.forEach { it.delete() }

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destino).use { output ->
                    input.copyTo(output)
                }
            }
            val uriResult = Uri.fromFile(destino).toString()
            setLogoTiendaUri(uriResult)
            uriResult
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun eliminarLogo() {
        try {
            context.filesDir.listFiles { file -> file.name.startsWith("logo_tienda_") }?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setLogoTiendaUri(null)
    }
}
