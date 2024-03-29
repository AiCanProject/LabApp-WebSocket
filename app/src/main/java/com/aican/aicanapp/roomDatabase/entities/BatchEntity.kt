package com.aican.aicanapp.roomDatabase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "batch_table")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchName: String
)
