package com.repp.aquaticremineztools.data.model

data class CryptoAsset(
    val id: String,
    val name: String,
    val symbol: String,
    val imageUrl: String?,
    val currentPriceUsd: Double?,
    val priceChange24h: Double?,
    val sparklinePrices: List<Float>?,
    val isDemoData: Boolean = false
)
