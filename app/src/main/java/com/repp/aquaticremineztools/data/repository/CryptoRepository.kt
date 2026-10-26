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
                ids = "bitcoin,ethereum,solana,binancecoin,travala",
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
        val targetOrder = listOf("bitcoin", "ethereum", "solana", "binancecoin", "travala")
        val dtoMap = dtos.associateBy { it.id.lowercase(Locale.ROOT) }

        return targetOrder.map { id ->
            val dto = dtoMap[id]
            val defaultFallback = getDefaultAsset(id)
            if (dto == null) {
                return@map defaultFallback
            }

            val sparklineFloats = dto.sparklineIn7d?.price?.map { it.toFloat() }
            val img = if (!dto.image.isNullOrBlank()) dto.image else defaultFallback.imageUrl

            CryptoAsset(
                id = dto.id,
                name = when (dto.id.lowercase(Locale.ROOT)) {
                    "bitcoin" -> "Bitcoin"
                    "ethereum" -> "Ethereum"
                    "solana" -> "Solana"
                    "binancecoin" -> "BNB"
                    "travala" -> "Travala"
                    else -> dto.name
                },
                symbol = dto.symbol.uppercase(Locale.ROOT),
                imageUrl = img,
                currentPriceUsd = dto.currentPrice ?: defaultFallback.currentPriceUsd,
                priceChange24h = dto.priceChangePercentage24h ?: defaultFallback.priceChange24h,
                sparklinePrices = if (!sparklineFloats.isNullOrEmpty()) sparklineFloats else defaultFallback.sparklinePrices,
                isDemoData = false
            )
        }
    }

    private fun getDefaultAsset(id: String): CryptoAsset {
        return when (id.lowercase(Locale.ROOT)) {
            "bitcoin" -> CryptoAsset(
                id = "bitcoin",
                name = "Bitcoin",
                symbol = "BTC",
                imageUrl = "https://assets.coingecko.com/coins/images/1/large/bitcoin.png",
                currentPriceUsd = 67450.00,
                priceChange24h = 2.45,
                sparklinePrices = listOf(66100f, 66350f, 66800f, 66500f, 67100f, 67250f, 67450f),
                isDemoData = true
            )
            "ethereum" -> CryptoAsset(
                id = "ethereum",
                name = "Ethereum",
                symbol = "ETH",
                imageUrl = "https://assets.coingecko.com/coins/images/279/large/ethereum.png",
                currentPriceUsd = 3520.50,
                priceChange24h = 1.82,
                sparklinePrices = listOf(3450f, 3480f, 3510f, 3490f, 3515f, 3510f, 3520.5f),
                isDemoData = true
            )
            "solana" -> CryptoAsset(
                id = "solana",
                name = "Solana",
                symbol = "SOL",
                imageUrl = "https://assets.coingecko.com/coins/images/4128/large/solana.png",
                currentPriceUsd = 178.20,
                priceChange24h = 4.15,
                sparklinePrices = listOf(169f, 172f, 175f, 173f, 176f, 177f, 178.2f),
                isDemoData = true
            )
            "binancecoin" -> CryptoAsset(
                id = "binancecoin",
                name = "BNB",
                symbol = "BNB",
                imageUrl = "https://assets.coingecko.com/coins/images/825/large/bnb-icon2_2x.png",
                currentPriceUsd = 585.40,
                priceChange24h = 0.95,
                sparklinePrices = listOf(578f, 580f, 582f, 581f, 583f, 584f, 585.4f),
                isDemoData = true
            )
            else -> CryptoAsset(
                id = "travala",
                name = "Travala",
                symbol = "AVA",
                imageUrl = "https://assets.coingecko.com/coins/images/2972/large/AVA-logo.png",
                currentPriceUsd = 0.54,
                priceChange24h = 3.20,
                sparklinePrices = listOf(0.51f, 0.52f, 0.525f, 0.52f, 0.53f, 0.535f, 0.54f),
                isDemoData = true
            )
        }
    }

    private fun getDemoFallbackData(): List<CryptoAsset> {
        return listOf(
            getDefaultAsset("bitcoin"),
            getDefaultAsset("ethereum"),
            getDefaultAsset("solana"),
            getDefaultAsset("binancecoin"),
            getDefaultAsset("travala")
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
