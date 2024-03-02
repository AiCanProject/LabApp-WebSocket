package com.aican.aicanapp.roomDatabase.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_action")
data class UserActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "time")
    val time: String,

    @ColumnInfo(name = "date")
    val date: String,

    @ColumnInfo(name = "useraction")
    val userAction: String,

    @ColumnInfo(name = "ph")
    val ph: String,

    @ColumnInfo(name = "temperature")
    val temperature: String,

    @ColumnInfo(name = "mv")
    val mv: String,

    @ColumnInfo(name = "compound")
    val compound: String,

    @ColumnInfo(name = "deviceID")
    val deviceID: String
)