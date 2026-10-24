package com.repp.aquaticremineztools.data.model

data class PortfolioHolding(
    val id: String = System.currentTimeMillis().toString(),
    val instrumentId: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val averageCost: Double,
    val currency: String = "USD",
    val dateAddedEpoch: Long = System.currentTimeMillis()
)
