package com.aican.aicanapp.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import kotlinx.coroutines.launch

class ProductViewModel(private val productsListDao: ProductsListDao) : ViewModel() {

    val productListLiveData: LiveData<List<ProductEntity>> = productsListDao.getAllProducts()


    fun insertProduct(product: ProductEntity) {
            productsListDao.insertProductData(product)

    }

    suspend fun fetchProducts(): List<ProductEntity> {
        return productsListDao.getAllProductsData()
    }

}