package com.aican.aicanapp.roomDatabase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

//         sqLiteDatabase.execSQL("create Table Userdetails(name TEXT,role TEXT,id TEXT,passcode
//         TEXT,expiryDate TEXT,dateCreated TEXT)");
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val password: String,
    val role: String,
    val dateOfCreation: String,
    val dateOfExpiry: String,
    val dateOfModification: String,
    val isActive: Boolean
)