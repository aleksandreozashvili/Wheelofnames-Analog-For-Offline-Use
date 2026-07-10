package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WheelDao {
    @Query("SELECT * FROM wheels ORDER BY lastModified DESC")
    fun getAllWheels(): Flow<List<WheelEntity>>

    @Query("SELECT * FROM wheels WHERE id = :id")
    suspend fun getWheelById(id: Int): WheelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWheel(wheel: WheelEntity): Long

    @Update
    suspend fun updateWheel(wheel: WheelEntity)

    @Delete
    suspend fun deleteWheel(wheel: WheelEntity)

    @Query("DELETE FROM wheels WHERE id = :id")
    suspend fun deleteWheelById(id: Int)
}
