package com.aican.aicanapp.roomDatabase.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.aican.aicanapp.roomDatabase.daoObjects.ARNumDao
import com.aican.aicanapp.roomDatabase.daoObjects.AllLogsDataDao
import com.aican.aicanapp.roomDatabase.daoObjects.BatchListDao
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao1
import com.aican.aicanapp.roomDatabase.daoObjects.UnknownListDao2
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.entities.ARNumEntity
import com.aican.aicanapp.roomDatabase.entities.AllLogsEntity
import com.aican.aicanapp.roomDatabase.entities.BatchEntity
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity1
import com.aican.aicanapp.roomDatabase.entities.UnknownEntity2
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.roomDatabase.entities.UserEntity

@Database(
    entities = [UserEntity::class, UserActionEntity::class, AllLogsEntity::class,
        ProductEntity::class, ARNumEntity::class, BatchEntity::class, UnknownEntity1::class, UnknownEntity2::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userActionDao(): UserActionDao
    abstract fun allLogsDao(): AllLogsDataDao
    abstract fun productsDao(): ProductsListDao
    abstract fun arNumDao(): ARNumDao
    abstract fun batchDao(): BatchListDao
    abstract fun unknown1Dao(): UnknownListDao1
    abstract fun unknown2Dao(): UnknownListDao2

}