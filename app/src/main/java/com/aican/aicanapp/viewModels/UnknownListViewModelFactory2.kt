package com.aican.aicanapp.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao2

class UnknownListViewModelFactory2(private val dao: UnknownListDao2) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UnknownListViewModel2::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UnknownListViewModel2(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}