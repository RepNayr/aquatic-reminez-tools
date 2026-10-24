package com.repp.aquaticremineztools.data.model

data class MarketInstrument(
    val id: String,
    val symbol: String,
    val name: String,
    val category: MarketCategory,
    val currentPrice: Double?,
    val priceChange24h: Double?,
    val priceChangePercentage24h: Double?,
    val currency: String = "USD",
    val exchange: String = "GLOBAL",
    val imageUrl: String? = null,
    val sparklinePrices: List<Float>? = null,
    val historicalCandles: List<CandleData>? = null,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val isCached: Boolean = false,
    val isDemo: Boolean = false
)

data class CandleData(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float = 0f
)
