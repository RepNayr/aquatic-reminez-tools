package com.repp.aquaticremineztools.data.local.history

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val toolId: String,
    val toolName: String,
    val category: String,
    val activityType: String, // "OPENED" or "EXECUTED"
    val status: String? = null, // "RUNNING", "SUCCESS", "FAILED", "CANCELLED", "INTERRUPTED"
    val timestamp: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val durationMs: Long? = null,
    val resultSummary: String? = null,
    val iconResName: String? = null
) : Serializable
