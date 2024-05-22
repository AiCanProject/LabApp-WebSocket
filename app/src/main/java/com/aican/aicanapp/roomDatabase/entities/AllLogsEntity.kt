package com.aican.aicanapp.roomDatabase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

//sqLiteDatabase.execSQL("create Table LogUserdetails(date TEXT, time TEXT, ph TEXT, temperature TEXT,
// batchnum TEXT, arnum TEXT, compound TEXT,deviceID TEXT)");

@Entity(tableName = "all_logs_data")
data class AllLogsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val date: String,
    val time: String,
    val ph: String,
    val temperature: String,
    val batchnum: String,
    val arnum: String,
    val compound: String,
    val deviceID: String,
    val unknown_one: String,
    val unknown_two: String,
)