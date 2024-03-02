package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity

@Dao
interface UserActionDao {

    @Insert
    fun insertUserAction(user: UserActionEntity)

    @Query("SELECT * FROM user_action")
    fun getAllUsersActions(): List<UserActionEntity>


}