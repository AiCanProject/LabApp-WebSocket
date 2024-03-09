package com.aican.aicanapp.roomDatabase.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aican.aicanapp.roomDatabase.daoObjects.AllLogsDataDao
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.entities.AllLogsEntity
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.roomDatabase.entities.UserEntity

@Database(entities = [UserEntity::class, UserActionEntity::class,  AllLogsEntity::class
                     ,ProductEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userActionDao(): UserActionDao
    abstract fun allLogsDao(): AllLogsDataDao
    abstract fun productsDao() : ProductsListDao

}