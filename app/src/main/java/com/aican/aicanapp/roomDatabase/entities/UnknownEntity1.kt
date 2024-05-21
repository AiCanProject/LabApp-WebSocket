package com.aican.aicanapp.roomDatabase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "unknown_one_list")
data class UnknownEntity1(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String
)