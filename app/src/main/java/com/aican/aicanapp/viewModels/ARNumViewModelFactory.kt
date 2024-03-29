package com.aican.aicanapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aican.aicanapp.roomDatabase.daoObjects.ARNumDao
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao

class ARNumViewModelFactory(private val dao: ARNumDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ARNumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ARNumViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}