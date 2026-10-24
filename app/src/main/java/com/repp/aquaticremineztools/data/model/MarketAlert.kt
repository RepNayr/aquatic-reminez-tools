package com.repp.aquaticremineztools.data.model

enum class AlertType(val displayName: String) {
    PRICE_ABOVE("Price Above"),
    PRICE_BELOW("Price Below"),
    PCT_CHANGE_24H("24h % Change")
}

data class MarketAlert(
    val id: String = System.currentTimeMillis().toString(),
    val instrumentId: String,
    val symbol: String,
    val name: String,
    val targetValue: Double,
    val alertType: AlertType,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val lastTriggeredEpoch: Long? = null
)
