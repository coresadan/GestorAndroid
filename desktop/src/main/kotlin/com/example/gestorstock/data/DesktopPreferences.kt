package com.example.gestorstock.data

import org.json.JSONObject
import java.io.File

class DesktopPreferences {

    private val configFile: File
    private val appDir: File

    init {
        val appData = System.getenv("APPDATA") ?: System.getProperty("user.home")
        appDir = File(appData, "GestorStock").apply { if (!exists()) mkdirs() }
        configFile = File(appDir, "config.json")
    }

    private fun readConfig(): JSONObject {
        return try {
            if (configFile.exists()) {
                val text = configFile.readText(Charsets.UTF_8)
                JSONObject(text)
            } else {
                JSONObject()
            }
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun saveConfig(json: JSONObject) {
        try {
            configFile.writeText(json.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNombreTienda(): String {
        val json = readConfig()
        return json.optString("nombre_tienda", "Mi Tiendita").ifBlank { "Mi Tiendita" }
    }

    fun setNombreTienda(nombre: String) {
        val json = readConfig()
        json.put("nombre_tienda", nombre.trim().ifEmpty { "Mi Tiendita" })
        saveConfig(json)
    }

    fun getLogoTiendaUri(): String? {
        val json = readConfig()
        val path = json.optString("logo_tienda_uri", "")
        return if (path.isNotBlank() && File(path).exists()) path else null
    }

    fun setLogoTiendaUri(path: String?) {
        val json = readConfig()
        if (path != null) {
            json.put("logo_tienda_uri", path)
        } else {
            json.remove("logo_tienda_uri")
        }
        saveConfig(json)
    }

    fun guardarLogoDesdeArchivo(origen: File): String? {
        return try {
            val destino = File(appDir, "logo_tienda_${System.currentTimeMillis()}.png")
            // Eliminar anteriores
            appDir.listFiles { file -> file.name.startsWith("logo_tienda_") }?.forEach { it.delete() }
            origen.copyTo(destino, overwrite = true)
            setLogoTiendaUri(destino.absolutePath)
            destino.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun eliminarLogo() {
        try {
            appDir.listFiles { file -> file.name.startsWith("logo_tienda_") }?.forEach { it.delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setLogoTiendaUri(null)
    }
}
