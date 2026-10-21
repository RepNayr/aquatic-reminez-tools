package com.repp.aquaticremineztools.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.repp.aquaticremineztools.data.model.CryptoAsset
import com.repp.aquaticremineztools.data.remote.crypto.CoinGeckoApiClient
import com.repp.aquaticremineztools.data.remote.crypto.CoinMarketDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class CryptoMarketResult(
    val assets: List<CryptoAsset>,
    val isCached: Boolean,
    val isDemo: Boolean
)

class CryptoRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("crypto_market_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getMarketData(forceRefresh: Boolean = false): Result<CryptoMarketResult> = withContext(Dispatchers.IO) {
        val lastFetchTime = prefs.getLong("last_fetch_timestamp", 0L)
        val currentTime = System.currentTimeMillis()
        val cacheAgeMs = currentTime - lastFetchTime
        val cacheValidMs = 3 * 60 * 1000L // 3 minutes cache to avoid rate limits

        val cachedJson = prefs.getString("cached_market_json", null)

        // If cache is fresh and not forceRefresh, load cache directly
        if (!forceRefresh && cacheAgeMs < cacheValidMs && !cachedJson.isNullOrEmpty()) {
            val cachedResult = parseCachedJson(cachedJson, isCached = true)
            if (cachedResult != null) {
                return@withContext Result.success(cachedResult)
            }
        }

        // Network Fetch
        try {
            val response = CoinGeckoApiClient.service.getMarkets(
                vsCurrency = "usd",
                ids = "bitcoin,ethereum,solana,binancecoin",
                sparkline = true,
                priceChangePercentage = "24h"
            )

            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val dtoList = response.body()!!
                val jsonString = gson.toJson(dtoList)

                prefs.edit()
                    .putString("cached_market_json", jsonString)
                    .putLong("last_fetch_timestamp", currentTime)
                    .apply()

                val cryptoList = processMarketDtos(dtoList)
                return@withContext Result.success(
                    CryptoMarketResult(
                        assets = cryptoList,
                        isCached = false,
                        isDemo = false
                    )
                )
            } else {
                Log.w("CryptoRepository", "CoinGecko API returned HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Error fetching CoinGecko data: ${e.message}")
        }

        // Fallback to local cache if available
        if (!cachedJson.isNullOrEmpty()) {
            val cachedResult = parseCachedJson(cachedJson, isCached = true)
            if (cachedResult != null) {
                return@withContext Result.success(cachedResult)
            }
        }

        // Fallback to offline demo structure (without claiming to be live data)
        val demoList = getDemoFallbackData()
        return@withContext Result.success(
            CryptoMarketResult(
                assets = demoList,
                isCached = false,
                isDemo = true
            )
        )
    }

    private fun parseCachedJson(json: String, isCached: Boolean): CryptoMarketResult? {
        return try {
            val type = object : TypeToken<List<CoinMarketDto>>() {}.type
            val dtoList: List<CoinMarketDto> = gson.fromJson(json, type)
            if (!dtoList.isNullOrEmpty()) {
                val cryptoList = processMarketDtos(dtoList)
                CryptoMarketResult(
                    assets = cryptoList,
                    isCached = isCached,
                    isDemo = false
                )
            } else null
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Failed to parse cached JSON: ${e.message}")
            null
        }
    }

    private fun processMarketDtos(dtos: List<CoinMarketDto>): List<CryptoAsset> {
        val targetOrder = listOf("bitcoin", "ethereum", "solana", "binancecoin")
        val dtoMap = dtos.associateBy { it.id.lowercase(Locale.ROOT) }

        return targetOrder.mapNotNull { id ->
            val dto = dtoMap[id] ?: return@mapNotNull null
            val sparklineFloats = dto.sparklineIn7d?.price?.map { it.toFloat() }

            CryptoAsset(
                id = dto.id,
                name = when (dto.id.lowercase(Locale.ROOT)) {
                    "bitcoin" -> "Bitcoin"
                    "ethereum" -> "Ethereum"
                    "solana" -> "Solana"
                    "binancecoin" -> "BNB"
                    else -> dto.name
                },
                symbol = dto.symbol.uppercase(Locale.ROOT),
                imageUrl = dto.image,
                currentPriceUsd = dto.currentPrice,
                priceChange24h = dto.priceChangePercentage24h,
                sparklinePrices = sparklineFloats,
                isDemoData = false
            )
        }
    }

    private fun getDemoFallbackData(): List<CryptoAsset> {
        return listOf(
            CryptoAsset(
                id = "bitcoin",
                name = "Bitcoin",
                symbol = "BTC",
                imageUrl = null,
                currentPriceUsd = null,
                priceChange24h = null,
                sparklinePrices = null,
                isDemoData = true
            ),
            CryptoAsset(
                id = "ethereum",
                name = "Ethereum",
                symbol = "ETH",
                imageUrl = null,
                currentPriceUsd = null,
                priceChange24h = null,
                sparklinePrices = null,
                isDemoData = true
            ),
            CryptoAsset(
                id = "solana",
                name = "Solana",
                symbol = "SOL",
                imageUrl = null,
                currentPriceUsd = null,
                priceChange24h = null,
                sparklinePrices = null,
                isDemoData = true
            ),
            CryptoAsset(
                id = "binancecoin",
                name = "BNB",
                symbol = "BNB",
                imageUrl = null,
                currentPriceUsd = null,
                priceChange24h = null,
                sparklinePrices = null,
                isDemoData = true
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: CryptoRepository? = null

        fun getInstance(context: Context): CryptoRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CryptoRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
