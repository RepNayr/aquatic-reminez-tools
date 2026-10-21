package com.repp.aquaticremineztools.data.remote.crypto

import com.google.gson.annotations.SerializedName

data class CoinMarketDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("image")
    val image: String?,

    @SerializedName("current_price")
    val currentPrice: Double?,

    @SerializedName("price_change_percentage_24h")
    val priceChangePercentage24h: Double?,

    @SerializedName("sparkline_in_7d")
    val sparklineIn7d: SparklineDataDto?
)

data class SparklineDataDto(
    @SerializedName("price")
    val price: List<Double>?
)
