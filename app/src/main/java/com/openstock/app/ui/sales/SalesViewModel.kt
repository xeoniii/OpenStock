package com.openstock.app.ui.sales

import androidx.lifecycle.*
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.data.model.*
import com.openstock.app.data.repository.AppRepository
import kotlinx.coroutines.launch

class SalesViewModel(private val repository: AppRepository) : ViewModel() {
    val allProducts: LiveData<List<Product>> = repository.allProducts
    val allInventory: LiveData<List<InventoryRaw>> = repository.allInventory
    
    private val _inventorySearchQuery = MutableLiveData("")
    val filteredInventory: LiveData<List<InventoryRaw>> = _inventorySearchQuery.switchMap { query ->
        repository.allInventory.map { items ->
            if (query.isNullOrBlank()) {
                items
            } else {
                items.filter { it.productName.contains(query, ignoreCase = true) }
            }
        }
    }

    fun setInventorySearchQuery(query: String) {
        _inventorySearchQuery.value = query
    }
    val totalRevenue: LiveData<Double?> = repository.totalRevenue
    val totalWholesale: LiveData<Double?> = repository.totalWholesale

    private val _isSalesModeInternal = MutableLiveData<Boolean>(true)
    val isSalesMode: LiveData<Boolean> = _isSalesModeInternal
    
    val filteredSaleGroups: LiveData<List<SaleGroupSummary>> = _isSalesModeInternal.switchMap { isSales ->
        repository.getSaleGroupSummariesByStatus(completed = !isSales)
    }

    fun setSalesMode(isSales: Boolean) {
        if (_isSalesModeInternal.value != isSales) {
            _isSalesModeInternal.value = isSales
        }
    }

    fun insertSaleGroup(group: SaleGroup, onResult: (Long) -> Unit) = viewModelScope.launch {
        val id = repository.insertSaleGroup(group)
        onResult(id)
    }

    fun updateSaleGroup(group: SaleGroup) = viewModelScope.launch {
        repository.updateSaleGroup(group)
    }

    fun deleteSaleGroup(group: SaleGroup) = viewModelScope.launch {
        repository.deleteSaleGroup(group)
    }

    fun completeSaleGroup(groupId: Long) = viewModelScope.launch {
        repository.completeSaleGroup(groupId)
    }

    fun verifySaleGroup(groupId: Long) = viewModelScope.launch {
        val group = repository.getSaleGroupById(groupId)
        if (group != null) {
            repository.updateSaleGroup(group.copy(isVerified = true))
        }
    }

    fun updateTotalRevenueOverride(groupId: Long, total: Double) = viewModelScope.launch {
        val group = repository.getSaleGroupById(groupId)
        if (group != null) {
            repository.updateSaleGroup(group.copy(overrideTotalRetail = total))
        }
    }

    fun insertSaleItem(item: SaleItem) = viewModelScope.launch {
        repository.insertSaleItem(item)
    }

    fun updateSaleItem(item: SaleItem) = viewModelScope.launch {
        repository.updateSaleItem(item)
    }

    fun deleteSaleItem(item: SaleItem) = viewModelScope.launch {
        repository.deleteSaleItem(item)
    }

    fun getSaleItemsByGroup(groupId: Long) = repository.getSaleItemsByGroup(groupId)
    fun getSaleItemsWithProductByGroup(groupId: Long) = repository.getSaleItemsWithProductByGroup(groupId)

    suspend fun getSaleGroupByIdSync(id: Long) = repository.getSaleGroupById(id)
    suspend fun getAllProductsSync() = repository.getAllProductsSync()
    suspend fun getInventoryByProductId(productId: Long) = repository.getInventoryByProductId(productId)
    suspend fun getProductByBarcode(barcode: String) = repository.getProductByBarcode(barcode)
    
    suspend fun getCompletedSaleGroups() = repository.getSaleGroupsByStatusSync(completed = true)
    suspend fun getCompletedUnverifiedSaleGroups() = repository.getSaleGroupsByStatusSync(completed = true).filter { !it.isVerified }
    suspend fun getSaleItemsByGroupSync(groupId: Long) = repository.getSaleItemsByGroupSync(groupId)
    suspend fun getProductById(id: Long) = repository.getProductById(id)
}

class SalesViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SalesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
