package com.openstock.app.ui.products

import androidx.lifecycle.*
import com.openstock.app.data.model.Product
import com.openstock.app.data.repository.AppRepository
import kotlinx.coroutines.launch

class ProductsViewModel(private val repository: AppRepository) : ViewModel() {
    private val _searchQuery = MutableLiveData("")

    val filteredProducts: LiveData<List<Product>> = _searchQuery.switchMap { query ->
        repository.allProducts.map { products ->
            if (query.isNullOrBlank()) {
                products
            } else {
                products.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.barcode.contains(query, ignoreCase = true)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun insertProduct(product: Product) = viewModelScope.launch {
        repository.insertProduct(product)
    }

    fun updateProduct(product: Product) = viewModelScope.launch {
        repository.updateProduct(product)
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        repository.deleteProduct(product)
    }

    suspend fun getProductByBarcode(barcode: String) = repository.getProductByBarcode(barcode)
}

class ProductsViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
