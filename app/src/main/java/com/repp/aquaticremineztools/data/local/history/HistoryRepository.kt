package com.repp.aquaticremineztools.data.local.history

import android.content.Context
import com.repp.aquaticremineztools.data.ToolImplementationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistoryRepository private constructor(context: Context) {

    private val dao = HistoryDatabase.getInstance(context).historyDao()

    companion object {
        @Volatile
        private var INSTANCE: HistoryRepository? = null

        fun getInstance(context: Context): HistoryRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = HistoryRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            recoverStaleRunningExecutions()
            cleanupNonImplementedRecords()
            cleanupOldRecords()
        }
    }

    private suspend fun cleanupNonImplementedRecords() = withContext(Dispatchers.IO) {
        val allItems = dao.getAllHistoryList()
        val nonImplementedIds = allItems.filter { !ToolImplementationHelper.isToolImplemented(it.toolId) }.map { it.id }
        if (nonImplementedIds.isNotEmpty()) {
            dao.deleteByIds(nonImplementedIds)
        }
    }

    fun getAllHistory(): Flow<List<HistoryEntity>> = dao.getAllHistoryFlow()

    suspend fun insert(history: HistoryEntity): Long = withContext(Dispatchers.IO) {
        dao.insert(history)
    }

    suspend fun update(history: HistoryEntity) = withContext(Dispatchers.IO) {
        dao.update(history)
    }

    suspend fun delete(history: HistoryEntity) = withContext(Dispatchers.IO) {
        dao.delete(history)
    }

    suspend fun deleteByIds(ids: List<Long>) = withContext(Dispatchers.IO) {
        dao.deleteByIds(ids)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        dao.deleteAll()
    }

    suspend fun cleanupOldRecords() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        dao.deleteOlderThan(cutoff)
    }

    private suspend fun recoverStaleRunningExecutions() = withContext(Dispatchers.IO) {
        val runningItems = dao.getRunningExecutions()
        val now = System.currentTimeMillis()
        for (item in runningItems) {
            val updated = item.copy(
                status = "INTERRUPTED",
                endTime = now,
                resultSummary = "Execution interrupted unexpectedly (process terminated or app closed)."
            )
            dao.update(updated)
        }
    }
}