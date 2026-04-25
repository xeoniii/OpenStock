package com.openstock.app

import android.app.Application
import com.openstock.app.data.AppDatabase
import com.openstock.app.data.repository.AppRepository

class OpenStockApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        AppRepository(
            database,
            database.productDao(),
            database.inventoryDao(),
            database.saleGroupDao(),
            database.saleItemDao()
        )
    }
}
