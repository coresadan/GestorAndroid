package com.example.gestorstock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.example.gestorstock.theme.GestorStockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestorStockTheme {
                // rememberNavBackStack no acepta tipo genérico explícito en Nav3 1.0.1
                val backStack = rememberNavBackStack(InventarioRoute)
                AppNavigation(backStack = backStack)
            }
        }
    }
}
