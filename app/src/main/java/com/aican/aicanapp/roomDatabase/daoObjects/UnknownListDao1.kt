package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity1

@Dao
interface UnknownListDao1 {

    @Insert
    fun insertProductData(product: UnknownEntity1)

    @Query("SELECT * FROM unknown_one_list")
    fun getAllProducts(): LiveData<List<UnknownEntity1>>

    @Query("SELECT * FROM unknown_one_list")
    fun getAllProductsData(): List<UnknownEntity1>



    @Update
    fun updateProduct(product: UnknownEntity1)

    @Delete
    fun deleteProduct(product: UnknownEntity1)
}