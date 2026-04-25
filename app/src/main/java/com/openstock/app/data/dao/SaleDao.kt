package com.openstock.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.openstock.app.data.model.SaleGroup
import com.openstock.app.data.model.SaleGroupSummary
import com.openstock.app.data.model.SaleItem

@Dao
interface SaleGroupDao {
    @Query("SELECT * FROM sale_groups ORDER BY createdAt DESC")
    fun getAllSaleGroups(): LiveData<List<SaleGroup>>

    @Query("SELECT * FROM sale_groups WHERE isCompleted = :completed ORDER BY createdAt DESC")
    fun getSaleGroupsByStatus(completed: Boolean): LiveData<List<SaleGroup>>

    @Query("SELECT * FROM sale_groups WHERE isCompleted = :completed ORDER BY createdAt DESC")
    suspend fun getSaleGroupsByStatusSync(completed: Boolean): List<SaleGroup>

    @Query("""
        SELECT sg.*, 
               COUNT(si.id) as itemCount,
               TOTAL(si.quantity * si.wholesalePriceAtSale) as totalWholesale,
               TOTAL(si.quantity * si.retailPriceAtSale) as totalRetail
        FROM sale_groups sg
        LEFT JOIN sale_items si ON sg.id = si.saleGroupId
        WHERE sg.isCompleted = :completed
        GROUP BY sg.id
        ORDER BY sg.createdAt DESC
    """)
    fun getSaleGroupSummariesByStatus(completed: Boolean): LiveData<List<SaleGroupSummary>>

    @Query("SELECT * FROM sale_groups WHERE id = :id")
    suspend fun getSaleGroupById(id: Long): SaleGroup?

    @Insert
    suspend fun insertSaleGroup(group: SaleGroup): Long

    @Update
    suspend fun updateSaleGroup(group: SaleGroup)

    @Delete
    suspend fun deleteSaleGroup(group: SaleGroup)
}

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items WHERE id = :id")
    suspend fun getSaleItemById(id: Long): SaleItem?

    @Query("SELECT * FROM sale_items WHERE saleGroupId = :groupId")
    fun getSaleItemsByGroup(groupId: Long): LiveData<List<SaleItem>>

    @Query("SELECT * FROM sale_items WHERE saleGroupId = :groupId")
    suspend fun getSaleItemsByGroupSync(groupId: Long): List<SaleItem>

    @Query("""
        SELECT si.*, p.name as productName FROM sale_items si
        INNER JOIN products p ON si.productId = p.id
        WHERE si.saleGroupId = :groupId
    """)
    fun getSaleItemsWithProductByGroup(groupId: Long): LiveData<List<SaleItemRaw>>

    @Insert
    suspend fun insertSaleItem(item: SaleItem): Long

    @Update
    suspend fun updateSaleItem(item: SaleItem)

    @Delete
    suspend fun deleteSaleItem(item: SaleItem)

    @Query("DELETE FROM sale_items WHERE saleGroupId = :groupId")
    suspend fun deleteAllInGroup(groupId: Long)

    @Query("""
        SELECT SUM(COALESCE(sale_groups.overrideTotalRetail, sale_items.quantity * sale_items.retailPriceAtSale)) 
        FROM sale_items
        INNER JOIN sale_groups ON sale_items.saleGroupId = sale_groups.id
        WHERE sale_groups.isCompleted = 1 AND sale_groups.isVerified = 1
    """)
    fun getTotalRevenue(): LiveData<Double?>

    @Query("""
        SELECT SUM(quantity * wholesalePriceAtSale) FROM sale_items
        INNER JOIN sale_groups ON sale_items.saleGroupId = sale_groups.id
        WHERE sale_groups.isCompleted = 1 AND sale_groups.isVerified = 1
    """)
    fun getTotalWholesale(): LiveData<Double?>
}

data class SaleItemRaw(
    val id: Long,
    val saleGroupId: Long,
    val productId: Long,
    val quantity: Double,
    val wholesalePriceAtSale: Double,
    val retailPriceAtSale: Double,
    val productName: String
)
