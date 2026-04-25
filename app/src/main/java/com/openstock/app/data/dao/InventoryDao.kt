package com.openstock.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.openstock.app.data.model.InventoryItem

@Dao
interface InventoryDao {
    @Query("""
        SELECT inventory.*, products.name as productName, products.retailPrice as retailPrice FROM inventory 
        INNER JOIN products ON inventory.productId = products.id
        ORDER BY products.name ASC
    """)
    fun getAllInventoryRaw(): LiveData<List<InventoryRaw>>

    @Query("SELECT * FROM inventory WHERE productId = :productId LIMIT 1")
    suspend fun getInventoryByProductId(productId: Long): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(item: InventoryItem): Long

    @Update
    suspend fun updateInventory(item: InventoryItem)

    @Delete
    suspend fun deleteInventory(item: InventoryItem)

    @Query("DELETE FROM inventory WHERE productId = :productId")
    suspend fun deleteByProductId(productId: Long)
}

data class InventoryRaw(
    val id: Long,
    val productId: Long,
    val quantityInStock: Double,
    val lastUpdated: Long,
    val productName: String,
    val retailPrice: Double
)
