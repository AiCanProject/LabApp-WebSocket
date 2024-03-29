package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aican.aicanapp.roomDatabase.entities.BatchEntity

@Dao
interface BatchListDao {

    @Insert
    fun insertBatchData(product: BatchEntity)

    @Query("SELECT * FROM batch_table")
    fun getAllBatches(): LiveData<List<BatchEntity>>


    @Query("SELECT * FROM batch_table")
    fun getAllBatchesData(): List<BatchEntity>

    @Update
    fun updateBatch(product: BatchEntity)

    @Delete
    fun deleteBatch(product: BatchEntity)
}