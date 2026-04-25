package com.openstock.app.ui.inventory

import androidx.lifecycle.*
import com.openstock.app.data.dao.InventoryRaw
import com.openstock.app.data.model.InventoryItem
import com.openstock.app.data.model.Product
import com.openstock.app.data.repository.AppRepository
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: AppRepository) : ViewModel() {
    private val _searchQuery = MutableLiveData("")
    
    val filteredInventory: LiveData<List<InventoryRaw>> = _searchQuery.switchMap { query ->
        repository.allInventory.map { items ->
            if (query.isNullOrBlank()) {
                items
            } else {
                items.filter {
                    it.productName.contains(query, ignoreCase = true)
                }
            }
        }
    }

    val allProducts: LiveData<List<Product>> = repository.allProducts

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun upsertInventory(productId: Long, quantity: Double) = viewModelScope.launch {
        repository.upsertInventory(productId, quantity)
    }

    fun deleteInventory(item: InventoryItem) = viewModelScope.launch {
        repository.deleteInventory(item)
    }

    suspend fun getAllProductsSync() = repository.getAllProductsSync()
    
    suspend fun getProductByBarcode(barcode: String) = repository.getProductByBarcode(barcode)
    
    suspend fun getInventoryByProductId(productId: Long) = repository.getInventoryByProductId(productId)
}

class InventoryViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
