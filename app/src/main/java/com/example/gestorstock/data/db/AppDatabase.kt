package com.example.gestorstock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gestorstock.data.model.Articulo
import com.example.gestorstock.data.model.Categoria
import com.example.gestorstock.data.model.LineaVenta
import com.example.gestorstock.data.model.Venta

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE articulos ADD COLUMN codigoBarras TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_articulos_codigoBarras ON articulos(codigoBarras)")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ventas (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                fecha INTEGER NOT NULL,
                totalArticulos INTEGER NOT NULL,
                totalPrecio REAL NOT NULL,
                pagoCon REAL,
                cambio REAL,
                metodoPago TEXT NOT NULL DEFAULT 'Efectivo',
                notas TEXT NOT NULL DEFAULT '',
                estado TEXT NOT NULL DEFAULT 'COMPLETADA'
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS lineas_venta (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ventaId INTEGER NOT NULL,
                articuloId INTEGER,
                nombreArticulo TEXT NOT NULL,
                precioUnitario REAL NOT NULL,
                cantidad INTEGER NOT NULL,
                subtotal REAL NOT NULL,
                fotoUri TEXT,
                categoriaNombre TEXT,
                FOREIGN KEY(ventaId) REFERENCES ventas(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_lineas_venta_ventaId ON lineas_venta(ventaId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE articulos ADD COLUMN precioCosto REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE articulos ADD COLUMN unidadMedida TEXT NOT NULL DEFAULT 'pza'")
        db.execSQL("ALTER TABLE articulos ADD COLUMN fechaCaducidad INTEGER DEFAULT NULL")
    }
}

@Database(
    entities = [
        Articulo::class,
        Categoria::class,
        Venta::class,
        LineaVenta::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun articuloDao(): ArticuloDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun ventaDao(): VentaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor_stock.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
