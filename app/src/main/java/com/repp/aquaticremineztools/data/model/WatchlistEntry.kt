package com.repp.aquaticremineztools.data.model

data class WatchlistEntry(
    val instrumentId: String,
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val addedAtEpoch: Long = System.currentTimeMillis()
)
