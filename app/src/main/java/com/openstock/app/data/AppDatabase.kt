package com.openstock.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.openstock.app.data.dao.InventoryDao
import com.openstock.app.data.dao.ProductDao
import com.openstock.app.data.dao.SaleGroupDao
import com.openstock.app.data.dao.SaleItemDao
import com.openstock.app.data.model.InventoryItem
import com.openstock.app.data.model.Product
import com.openstock.app.data.model.SaleGroup
import com.openstock.app.data.model.SaleItem

@Database(
    entities = [Product::class, InventoryItem::class, SaleGroup::class, SaleItem::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun saleGroupDao(): SaleGroupDao
    abstract fun saleItemDao(): SaleItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sale_groups ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sale_groups ADD COLUMN isVerified INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sale_groups ADD COLUMN overrideTotalRetail REAL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "openstock_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration() // Fallback if migration fails
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
