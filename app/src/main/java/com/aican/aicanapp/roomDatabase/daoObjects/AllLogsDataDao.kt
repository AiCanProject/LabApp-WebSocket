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

    //                "SELECT * FROM LogUserdetails WHERE (DATE(date) BETWEEN '$startDateString'
//                AND '$endDateString') AND (time BETWEEN '$startTimeString' AND '$endTimeString')
//                AND (arnum = '$compoundName') AND (batchnum = '$batchNumString') AND (compound = '$arNumString')",


}