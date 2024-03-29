package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aican.aicanapp.roomDatabase.entities.ARNumEntity
import com.aican.aicanapp.roomDatabase.entities.BatchEntity

@Dao
interface ARNumDao {
    @Insert
    fun insertARNumData(product: ARNumEntity)

    @Query("SELECT * FROM arnum_table")
    fun getAllARNum(): LiveData<List<ARNumEntity>>

    @Query("SELECT * FROM arnum_table")
    fun getAllARNumData(): List<ARNumEntity>


    @Update
    fun updateARNum(product: ARNumEntity)

    @Delete
    fun deleteARNum(product: ARNumEntity)
}