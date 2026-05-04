package com.openstock.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.openstock.app.data.model.InventoryItem

@Dao
interface InventoryDao {
    @Query("""
        SELECT inventory.*, products.name as productName, products.description as productDescription, products.retailPrice as retailPrice, products.imagePath as imagePath FROM inventory 
        INNER JOIN products ON inventory.productId = products.id
        ORDER BY products.name ASC
    """)
    fun getAllInventoryRaw(): LiveData<List<InventoryRaw>>

    @Query("""
        SELECT inventory.*, products.name as productName, products.description as productDescription, products.retailPrice as retailPrice, products.imagePath as imagePath FROM inventory 
        INNER JOIN products ON inventory.productId = products.id
        WHERE products.name LIKE '%' || :query || '%'
        ORDER BY products.name ASC
    """)
    fun searchInventoryRaw(query: String): LiveData<List<InventoryRaw>>

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
    val productDescription: String,
    val retailPrice: Double,
    val imagePath: String? = null
)
