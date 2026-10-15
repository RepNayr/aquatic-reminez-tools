package com.repp.aquaticremineztools.core.history

import android.content.Context
import com.repp.aquaticremineztools.data.local.history.HistoryEntity
import com.repp.aquaticremineztools.data.local.history.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object HistoryLogger {

    fun logOpened(context: Context, toolId: String, toolName: String, category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = HistoryRepository.getInstance(context)
            val entry = HistoryEntity(
                toolId = toolId,
                toolName = toolName,
                category = category,
                activityType = "OPENED",
                status = null,
                timestamp = System.currentTimeMillis(),
                resultSummary = "Opened tool view."
            )
            repository.insert(entry)
        }
    }

    fun logExecutionStart(
        context: Context,
        toolId: String,
        toolName: String,
        category: String,
        initialSummary: String = "Execution started..."
    ): Long {
        var id = -1L
        val repository = HistoryRepository.getInstance(context)
        val entry = HistoryEntity(
            toolId = toolId,
            toolName = toolName,
            category = category,
            activityType = "EXECUTED",
            status = "RUNNING",
            timestamp = System.currentTimeMillis(),
            resultSummary = initialSummary
        )
        kotlin.runCatching {
            kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                id = repository.insert(entry)
            }
        }
        return id
    }

    fun logExecutionFinish(
        context: Context,
        entryId: Long,
        status: String,
        summary: String,
        durationMs: Long
    ) {
        if (entryId <= 0) return
        CoroutineScope(Dispatchers.IO).launch {
            val db = com.repp.aquaticremineztools.data.local.history.HistoryDatabase.getInstance(context)
            val existing = db.historyDao().getHistoryById(entryId)
            if (existing != null) {
                val now = System.currentTimeMillis()
                val updated = existing.copy(
                    status = status,
                    endTime = now,
                    durationMs = durationMs,
                    resultSummary = summary
                )
                db.historyDao().update(updated)
            }
        }
    }
}
