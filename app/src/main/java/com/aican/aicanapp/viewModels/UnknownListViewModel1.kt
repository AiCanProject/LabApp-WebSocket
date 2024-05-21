package com.aican.aicanapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao1
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity1

class UnknownListViewModel1(private val productsListDao: UnknownListDao1) : ViewModel() {

    val productListLiveData: LiveData<List<UnknownEntity1>> = productsListDao.getAllProducts()


    fun insertProduct(product: UnknownEntity1) {
        productsListDao.insertProductData(product)

    }

    suspend fun fetchProducts(): List<UnknownEntity1> {
        return productsListDao.getAllProductsData()
    }

}