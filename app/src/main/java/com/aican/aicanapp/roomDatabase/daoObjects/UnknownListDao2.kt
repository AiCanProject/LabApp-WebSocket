package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity2

@Dao
interface UnknownListDao2 {

    @Insert
    fun insertProductData(product: UnknownEntity2)

    @Query("SELECT * FROM unknown_two_list")
    fun getAllProducts(): LiveData<List<UnknownEntity2>>

    @Query("SELECT * FROM unknown_two_list")
    fun getAllProductsData(): List<UnknownEntity2>



    @Update
    fun updateProduct(product: UnknownEntity2)

    @Delete
    fun deleteProduct(product: UnknownEntity2)
}