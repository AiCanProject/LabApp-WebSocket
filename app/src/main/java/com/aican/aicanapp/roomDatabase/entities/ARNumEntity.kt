package com.aican.aicanapp.roomDatabase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arnum_table")
data class ARNumEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val arNum: String
)