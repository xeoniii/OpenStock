package com.openstock.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val _searchQuery = MutableLiveData<String>("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _isSalesMode = MutableLiveData<Boolean>(true) // Default to Sales mode
    val isSalesMode: LiveData<Boolean> = _isSalesMode

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSalesMode(isSales: Boolean) {
        _isSalesMode.value = isSales
    }

    fun toggleSalesMode() {
        _isSalesMode.value = !(_isSalesMode.value ?: true)
    }
}
