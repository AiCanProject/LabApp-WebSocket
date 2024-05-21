package com.aican.aicanapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao1

class UnknownListViewModelFactory1(private val dao: UnknownListDao1) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnknownListViewModel1::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UnknownListViewModel1(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}