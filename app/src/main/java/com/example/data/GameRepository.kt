package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(private val dao: GameProgressDao) {

    suspend fun getProgress(): GameProgress? = withContext(Dispatchers.IO) {
        dao.getProgress()
    }

    suspend fun saveProgress(progress: GameProgress) = withContext(Dispatchers.IO) {
        dao.saveProgress(progress)
    }

    suspend fun clearProgress() = withContext(Dispatchers.IO) {
        dao.clearProgress()
    }
}
