package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.aican.aicanapp.roomDatabase.entities.UserEntity

@Dao
interface UserDao {
    @Insert
    fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): List<UserEntity>?



    @Update
    fun updateUser(user: UserEntity)
}