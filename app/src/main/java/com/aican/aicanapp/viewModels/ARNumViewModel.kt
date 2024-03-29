package com.aican.aicanapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.aican.aicanapp.roomDatabase.daoObjects.ARNumDao
import com.aican.aicanapp.roomDatabase.entities.ARNumEntity
import com.aican.aicanapp.roomDatabase.entities.BatchEntity

class ARNumViewModel(private val productsListDao: ARNumDao) : ViewModel() {

    val productListLiveData: LiveData<List<ARNumEntity>> = productsListDao.getAllARNum()


    fun insertProduct(product: ARNumEntity) {
        productsListDao.insertARNumData(product)

    }


    suspend fun fetchARs(): List<ARNumEntity> {
        return productsListDao.getAllARNumData()
    }
}