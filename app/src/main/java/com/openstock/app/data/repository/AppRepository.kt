package com.openstock.app.data.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.openstock.app.data.AppDatabase
import com.openstock.app.data.dao.*
import com.openstock.app.data.model.*

class AppRepository(
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val inventoryDao: InventoryDao,
    private val saleGroupDao: SaleGroupDao,
    private val saleItemDao: SaleItemDao
) {
    // Products
    val allProducts: LiveData<List<Product>> = productDao.getAllProducts()
    suspend fun getAllProductsSync() = productDao.getAllProductsSync()
    suspend fun getProductById(id: Long) = productDao.getProductById(id)
    suspend fun getProductByBarcode(barcode: String) = productDao.getProductByBarcode(barcode)
    suspend fun insertProduct(product: Product) = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)

    // Inventory
    val allInventory: LiveData<List<InventoryRaw>> = inventoryDao.getAllInventoryRaw()
    fun searchInventory(query: String) = inventoryDao.searchInventoryRaw(query)
    suspend fun getInventoryByProductId(productId: Long) = inventoryDao.getInventoryByProductId(productId)
    suspend fun insertInventory(item: InventoryItem) = inventoryDao.insertInventory(item)
    suspend fun updateInventory(item: InventoryItem) = inventoryDao.updateInventory(item)
    suspend fun deleteInventory(item: InventoryItem) = inventoryDao.deleteInventory(item)
    
    suspend fun upsertInventory(productId: Long, quantity: Double) {
        if (quantity <= 0) {
            inventoryDao.deleteByProductId(productId)
            return
        }
        val existing = inventoryDao.getInventoryByProductId(productId)
        if (existing != null) {
            inventoryDao.updateInventory(existing.copy(quantityInStock = quantity, lastUpdated = System.currentTimeMillis()))
        } else {
            inventoryDao.insertInventory(InventoryItem(productId = productId, quantityInStock = quantity))
        }
    }

    // Sales
    val allSaleGroups: LiveData<List<SaleGroup>> = saleGroupDao.getAllSaleGroups()
    fun getSaleGroupsByStatus(completed: Boolean): LiveData<List<SaleGroup>> = saleGroupDao.getSaleGroupsByStatus(completed)
    suspend fun getSaleGroupsByStatusSync(completed: Boolean): List<SaleGroup> = saleGroupDao.getSaleGroupsByStatusSync(completed)
    fun getSaleGroupSummariesByStatus(completed: Boolean): LiveData<List<SaleGroupSummary>> = saleGroupDao.getSaleGroupSummariesByStatus(completed)
    suspend fun getSaleGroupById(id: Long) = saleGroupDao.getSaleGroupById(id)
    suspend fun insertSaleGroup(group: SaleGroup) = saleGroupDao.insertSaleGroup(group)
    suspend fun updateSaleGroup(group: SaleGroup) = saleGroupDao.updateSaleGroup(group)
    
    suspend fun deleteSaleGroup(group: SaleGroup) {
        database.withTransaction {
            if (!group.isVerified) {
                // If the sale is not verified, return items to inventory.
                // This covers both active sales and completed sales that haven't been verified in history.
                val items = saleItemDao.getSaleItemsByGroupSync(group.id)
                for (item in items) {
                    adjustInventory(item.productId, item.quantity)
                }
            }
            saleGroupDao.deleteSaleGroup(group)
        }
    }

    fun getSaleItemsByGroup(groupId: Long) = saleItemDao.getSaleItemsByGroup(groupId)
    fun getSaleItemsWithProductByGroup(groupId: Long) = saleItemDao.getSaleItemsWithProductByGroup(groupId)
    suspend fun getSaleItemsByGroupSync(groupId: Long) = saleItemDao.getSaleItemsByGroupSync(groupId)

    suspend fun completeSaleGroup(groupId: Long) {
        val group = saleGroupDao.getSaleGroupById(groupId)
        if (group != null) {
            saleGroupDao.updateSaleGroup(group.copy(isCompleted = true))
        }
    }

    suspend fun insertSaleItem(item: SaleItem) {
        database.withTransaction {
            val existingItems = saleItemDao.getSaleItemsByGroupSync(item.saleGroupId)
            val existing = existingItems.find { it.productId == item.productId && it.retailPriceAtSale == item.retailPriceAtSale && it.wholesalePriceAtSale == item.wholesalePriceAtSale }
            
            if (existing != null) {
                saleItemDao.updateSaleItem(existing.copy(quantity = existing.quantity + item.quantity))
            } else {
                saleItemDao.insertSaleItem(item)
            }

            adjustInventory(item.productId, -item.quantity)
        }
    }

    suspend fun updateSaleItem(item: SaleItem) {
        database.withTransaction {
            val oldItem = saleItemDao.getSaleItemById(item.id) ?: return@withTransaction
            saleItemDao.updateSaleItem(item)
            adjustInventory(item.productId, oldItem.quantity - item.quantity)
        }
    }

    suspend fun deleteSaleItem(item: SaleItem) {
        database.withTransaction {
            saleItemDao.deleteSaleItem(item)
            adjustInventory(item.productId, item.quantity)
        }
    }

    private suspend fun adjustInventory(productId: Long, delta: Double) {
        val inventory = inventoryDao.getInventoryByProductId(productId)
        if (inventory != null) {
            val newQty = inventory.quantityInStock + delta
            if (newQty <= 0) {
                inventoryDao.deleteByProductId(productId)
            } else {
                inventoryDao.updateInventory(
                    inventory.copy(
                        quantityInStock = newQty,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        } else if (delta > 0) {
            inventoryDao.insertInventory(InventoryItem(productId = productId, quantityInStock = delta))
        }
    }

    suspend fun deleteAllSaleItemsInGroup(groupId: Long) = saleItemDao.deleteAllInGroup(groupId)
    val totalRevenue: LiveData<Double?> = saleItemDao.getTotalRevenue()
    val totalWholesale: LiveData<Double?> = saleItemDao.getTotalWholesale()
}
