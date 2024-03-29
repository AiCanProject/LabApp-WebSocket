package com.aican.aicanapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.aican.aicanapp.roomDatabase.daoObjects.BatchListDao
import com.aican.aicanapp.roomDatabase.entities.BatchEntity
import com.aican.aicanapp.roomDatabase.entities.ProductEntity

class BatchViewModel(private val productsListDao: BatchListDao) : ViewModel() {

    val productListLiveData: LiveData<List<BatchEntity>> = productsListDao.getAllBatches()


    fun insertProduct(product: BatchEntity) {
        productsListDao.insertBatchData(product)

    }

    suspend fun fetchBatches(): List<BatchEntity> {
        return productsListDao.getAllBatchesData()
    }
}