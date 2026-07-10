package com.example.data

import kotlinx.coroutines.flow.Flow

class WheelRepository(private val wheelDao: WheelDao) {
    val allWheels: Flow<List<WheelEntity>> = wheelDao.getAllWheels()

    suspend fun getWheelById(id: Int): WheelEntity? {
        return wheelDao.getWheelById(id)
    }

    suspend fun insertWheel(wheel: WheelEntity): Long {
        return wheelDao.insertWheel(wheel)
    }

    suspend fun updateWheel(wheel: WheelEntity) {
        wheelDao.updateWheel(wheel)
    }

    suspend fun deleteWheel(wheel: WheelEntity) {
        wheelDao.deleteWheel(wheel)
    }

    suspend fun deleteWheelById(id: Int) {
        wheelDao.deleteWheelById(id)
    }
}
