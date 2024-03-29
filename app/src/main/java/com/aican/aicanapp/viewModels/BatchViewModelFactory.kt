package com.aican.aicanapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aican.aicanapp.roomDatabase.daoObjects.BatchListDao

class BatchViewModelFactory(private val dao: BatchListDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BatchViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}