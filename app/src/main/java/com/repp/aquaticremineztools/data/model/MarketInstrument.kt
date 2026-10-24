package com.repp.aquaticremineztools.data.model

enum class MarketDataStatus {
    LIVE,
    CACHED,
    SIMULATED,
    UNAVAILABLE
}

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
    val isDemo: Boolean = false,
    val dataStatus: MarketDataStatus = if (isDemo) MarketDataStatus.SIMULATED else if (isCached) MarketDataStatus.CACHED else MarketDataStatus.LIVE
)

data class CandleData(
    val timestamp: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float = 0f
)
