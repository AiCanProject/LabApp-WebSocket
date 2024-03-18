package com.aican.aicanapp.roomDatabase.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

//         sqLiteDatabase.execSQL("create Table Userdetails(name TEXT,role TEXT,id TEXT,passcode
//         TEXT,expiryDate TEXT,dateCreated TEXT)");
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey var id: String,
    var name: String,
    var password: String,
    var role: String,
    var dateOfCreation: String,
    var dateOfExpiry: String,
    var dateOfModification: String,
    var isActive: Boolean
)