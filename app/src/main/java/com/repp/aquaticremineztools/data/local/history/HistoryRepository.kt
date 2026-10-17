package com.repp.aquaticremineztools.data.local.history

import android.content.Context
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
            cleanupOldRecords()
            seedInitialDataIfEmpty()
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

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val current = dao.getAllHistoryList()
        if (current.isEmpty()) {
            val now = System.currentTimeMillis()
            val hourMs = 3600_000L
            val dayMs = 86400_000L

            val seedItems = listOf(
                HistoryEntity(
                    toolId = "net_ping",
                    toolName = "Network Diagnostics",
                    category = "Cyber Tools",
                    activityType = "EXECUTED",
                    status = "SUCCESS",
                    timestamp = now - 10 * 60 * 1000,
                    endTime = now - 9 * 60 * 1000 - 55 * 1000,
                    durationMs = 4200,
                    resultSummary = "Ping: 43ms · Packet Loss: 0% · Target: 1.1.1.1 reachable."
                ),
                HistoryEntity(
                    toolId = "net_wifi_analyzer",
                    toolName = "Wi-Fi Analyzer",
                    category = "Cyber Tools",
                    activityType = "EXECUTED",
                    status = "RUNNING",
                    timestamp = now - 2 * 60 * 1000,
                    durationMs = null,
                    resultSummary = "Scanning nearby 2.4GHz / 5GHz channels..."
                ),
                HistoryEntity(
                    toolId = "net_port_scanner",
                    toolName = "Port Scanner",
                    category = "Cyber Tools",
                    activityType = "EXECUTED",
                    status = "FAILED",
                    timestamp = now - 2 * hourMs,
                    durationMs = 12400,
                    resultSummary = "Connection refused at 192.168.1.1:8080 (Timeout)."
                ),
                HistoryEntity(
                    toolId = "secgen_hash",
                    toolName = "Hash Calculator",
                    category = "Security & Generate",
                    activityType = "OPENED",
                    status = null,
                    timestamp = now - 4 * hourMs,
                    resultSummary = "Opened tool view."
                ),
                HistoryEntity(
                    toolId = "net_dns",
                    toolName = "DNS Lookup",
                    category = "Cyber Tools",
                    activityType = "EXECUTED",
                    status = "SUCCESS",
                    timestamp = now - dayMs + hourMs,
                    durationMs = 850,
                    resultSummary = "Resolved A record: 104.21.55.2"
                ),
                HistoryEntity(
                    toolId = "net_subdomain",
                    toolName = "Subdomain Enumerator",
                    category = "Cyber Tools",
                    activityType = "EXECUTED",
                    status = "CANCELLED",
                    timestamp = now - dayMs - 3 * hourMs,
                    durationMs = 18200,
                    resultSummary = "Execution cancelled by user at candidate 42."
                ),
                HistoryEntity(
                    toolId = "net_traceroute",
                    toolName = "Packet Capture",
                    category = "Cyber Tools",
                    activityType = "EXECUTED",
                    status = "INTERRUPTED",
                    timestamp = now - 2 * dayMs,
                    durationMs = 35000,
                    resultSummary = "Capture service interrupted due to network interface disconnect."
                ),
                HistoryEntity(
                    toolId = "secgen_qr_gen",
                    toolName = "QR Code Generator",
                    category = "Security & Generate",
                    activityType = "OPENED",
                    status = null,
                    timestamp = now - 3 * dayMs,
                    resultSummary = "Opened tool view."
                )
            )

            seedItems.forEach { dao.insert(it) }
        }
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
