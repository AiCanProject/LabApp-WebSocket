package com.aican.aicanapp.roomDatabase.daoObjects

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.aican.aicanapp.roomDatabase.entities.AllLogsEntity

@Dao
interface AllLogsDataDao {

    @Insert
    fun insertLogData(user: AllLogsEntity)

    @Query("SELECT * FROM all_logs_data")
    fun getAllLogs(): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime " +
                "AND (arnum = :arnum) AND (batchnum = :batchnum) AND (compound = :compound)"
    )
    fun getLogsBetweenDateTimeAndAllThree(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        arnum: String,
        batchnum: String,
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime "
    )
    fun getAllLogBy_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE (arnum = :arnum) AND (batchnum = :batchnum) AND (compound = :compound)"
    )
    fun getLogByBAC(
        arnum: String,
        batchnum: String,
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE (batchnum = :batchnum)"
    )
    fun getLogByBatchnum(
        batchnum: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE (arnum = :arnum)"
    )
    fun getLogByArnum(
        arnum: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE (compound = :compound)"
    )
    fun getLogByProduct(
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE (arnum = :arnum) AND (compound = :compound)"
    )
    fun getLogByArnumAndProduct(
        arnum: String,
        compound: String
    ): List<AllLogsEntity>


    @Query(
        "SELECT * FROM all_logs_data WHERE (batchnum = :batchnum) AND (compound = :compound)"
    )
    fun getLogByBatchnumAndProduct(
        batchnum: String,
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE (arnum = :arnum) AND (batchnum = :batchnum)"
    )
    fun getLogByArnumAndBatchnum(
        arnum: String,
        batchnum: String
    ): List<AllLogsEntity>


    //////// with date and time


    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (arnum = :arnum) AND (batchnum = :batchnum) AND (compound = :compound)"
    )
    fun getLogByBAC_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        arnum: String,
        batchnum: String,
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (batchnum = :batchnum)"
    )
    fun getLogByBatchnum_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        batchnum: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (arnum = :arnum)"
    )
    fun getLogByArnum_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        arnum: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (compound = :compound)"
    )
    fun getLogByProduct_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (arnum = :arnum) AND (compound = :compound)"
    )
    fun getLogByArnumAndProduct_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        arnum: String,
        compound: String
    ): List<AllLogsEntity>


    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (batchnum = :batchnum) AND (compound = :compound)"
    )
    fun getLogByBatchnumAndProduct_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        batchnum: String,
        compound: String
    ): List<AllLogsEntity>

    @Query(
        "SELECT * FROM all_logs_data WHERE date BETWEEN :startDate AND :endDate AND time BETWEEN :startTime AND :endTime AND" +
                " (arnum = :arnum) AND (batchnum = :batchnum)"
    )
    fun getLogByArnumAndBatchnum_DNT(
        startDate: String,
        endDate: String,
        startTime: String,
        endTime: String,
        arnum: String,
        batchnum: String
    ): List<AllLogsEntity>


}