package com.repp.aquaticremineztools.core.history

import android.content.Context
import com.repp.aquaticremineztools.data.local.history.HistoryDatabase
import com.repp.aquaticremineztools.data.local.history.HistoryEntity
import com.repp.aquaticremineztools.data.local.history.HistoryRepository
import com.repp.aquaticremineztools.data.ToolImplementationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object HistoryLogger {

    fun logOpened(context: Context, toolId: String, toolName: String, category: String) {
        if (!ToolImplementationHelper.isToolImplemented(toolId)) return
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
        if (!ToolImplementationHelper.isToolImplemented(toolId)) return -1L
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
            runBlocking(Dispatchers.IO) {
                id = repository.insert(entry)
            }
        }.onFailure { e ->
            e.printStackTrace()
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
            val db = HistoryDatabase.getInstance(context)
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