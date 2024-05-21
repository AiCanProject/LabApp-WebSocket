package com.aican.aicanapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao2
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity2

class UnknownListViewModel2(private val productsListDao: UnknownListDao2) : ViewModel() {

    val productListLiveData: LiveData<List<UnknownEntity2>> = productsListDao.getAllProducts()


    fun insertProduct(product: UnknownEntity2) {
        productsListDao.insertProductData(product)

    }

    suspend fun fetchProducts(): List<UnknownEntity2> {
        return productsListDao.getAllProductsData()
    }

}