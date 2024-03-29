package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aican.aicanapp.roomDatabase.entities.ProductEntity

@Dao
interface ProductsListDao {

    @Insert
    fun insertProductData(product: ProductEntity)

    @Query("SELECT * FROM products_list")
    fun getAllProducts(): LiveData<List<ProductEntity>>

    @Query("SELECT * FROM products_list")
    fun getAllProductsData(): List<ProductEntity>



    @Update
    fun updateProduct(product: ProductEntity)

    @Delete
    fun deleteProduct(product: ProductEntity)
}