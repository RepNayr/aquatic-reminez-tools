package com.repp.aquaticremineztools.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.repp.aquaticremineztools.data.model.CandleData
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
import com.repp.aquaticremineztools.data.model.WatchlistEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

class MarketRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("market_terminal_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val cryptoRepository = CryptoRepository.getInstance(appContext)

    suspend fun getAllMarketInstruments(forceRefresh: Boolean = false): Result<List<MarketInstrument>> = withContext(Dispatchers.IO) {
        try {
            val list = mutableListOf<MarketInstrument>()

            // 1. Fetch live Crypto from CoinGecko via CryptoRepository
            val cryptoResult = cryptoRepository.getMarketData(forceRefresh)
            cryptoResult.onSuccess { marketResult ->
                marketResult.assets.forEach { asset ->
                    val priceChangePct = asset.priceChange24h ?: 0.0
                    val currentPrice = asset.currentPriceUsd ?: 0.0
                    val absChange = currentPrice * (priceChangePct / 100.0)
                    list.add(
                        MarketInstrument(
                            id = asset.id,
                            symbol = asset.symbol,
                            name = asset.name,
                            category = MarketCategory.CRYPTO,
                            currentPrice = asset.currentPriceUsd,
                            priceChange24h = absChange,
                            priceChangePercentage24h = priceChangePct,
                            currency = "USD",
                            exchange = "COINGECKO",
                            imageUrl = asset.imageUrl,
                            sparklinePrices = asset.sparklinePrices,
                            historicalCandles = generateMockCandles(currentPrice.takeIf { it > 0.0 } ?: 1000.0, priceChangePct),
                            isCached = marketResult.isCached,
                            isDemo = marketResult.isDemo,
                        ),
                    )
                }
            }.onFailure { e ->
                Log.w("MarketRepository", "Crypto market fetch failed: ${e.message}")
            }

            // 2. Add Regional & Global Indices (Demo / Simulated Market Data)
            list.addAll(getIndicesInstruments())

            // 3. Add Forex Instruments (Demo / Simulated Market Data)
            list.addAll(getForexInstruments())

            // 4. Add Commodities (Demo / Simulated Market Data)
            list.addAll(getCommoditiesInstruments())

            // 5. Add Economic Indicators (Demo / Simulated Market Data)
            list.addAll(getEconomicsInstruments())

            // 6. Add ETFs & Sectors (Demo / Simulated Market Data)
            list.addAll(getEtfsInstruments())

            return@withContext Result.success(list)
        } catch (e: Exception) {
            Log.e("MarketRepository", "Error assembling market instruments: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    private fun getIndicesInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "ihsg",
                symbol = "IHSG",
                name = "Jakarta Composite Index",
                category = MarketCategory.INDICES,
                currentPrice = 7324.50,
                priceChange24h = 45.20,
                priceChangePercentage24h = 0.62,
                currency = "IDR",
                exchange = "IDX",
                historicalCandles = generateMockCandles(7324.50, 0.62),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "sp500",
                symbol = "SPX",
                name = "S&P 500",
                category = MarketCategory.INDICES,
                currentPrice = 5815.20,
                priceChange24h = 32.40,
                priceChangePercentage24h = 0.56,
                currency = "USD",
                exchange = "NYSE",
                historicalCandles = generateMockCandles(5815.20, 0.56),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "nasdaq",
                symbol = "IXIC",
                name = "Nasdaq Composite",
                category = MarketCategory.INDICES,
                currentPrice = 18518.60,
                priceChange24h = 142.10,
                priceChangePercentage24h = 0.77,
                currency = "USD",
                exchange = "NASDAQ",
                historicalCandles = generateMockCandles(18518.60, 0.77),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "nikkei",
                symbol = "NI225",
                name = "Nikkei 225",
                category = MarketCategory.INDICES,
                currentPrice = 38981.75,
                priceChange24h = -125.50,
                priceChangePercentage24h = -0.32,
                currency = "JPY",
                exchange = "TSE",
                historicalCandles = generateMockCandles(38981.75, -0.32),
                timestampEpoch = now,
                isDemo = true,
            ),
        )
    }

    private fun getForexInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "usd_idr",
                symbol = "USD/IDR",
                name = "US Dollar / Indonesian Rupiah",
                category = MarketCategory.FOREX,
                currentPrice = 15680.0,
                priceChange24h = 35.0,
                priceChangePercentage24h = 0.22,
                currency = "IDR",
                exchange = "FX",
                historicalCandles = generateMockCandles(15680.0, 0.22),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "eur_usd",
                symbol = "EUR/USD",
                name = "Euro / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = 1.0852,
                priceChange24h = -0.0015,
                priceChangePercentage24h = -0.14,
                currency = "USD",
                exchange = "FX",
                historicalCandles = generateMockCandles(1.0852, -0.14),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "gbp_usd",
                symbol = "GBP/USD",
                name = "British Pound / US Dollar",
                category = MarketCategory.FOREX,
                currentPrice = 1.2985,
                priceChange24h = 0.0024,
                priceChangePercentage24h = 0.19,
                currency = "USD",
                exchange = "FX",
                historicalCandles = generateMockCandles(1.2985, 0.19),
                timestampEpoch = now,
                isDemo = true,
            ),
        )
    }

    private fun getCommoditiesInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "gold",
                symbol = "XAU/USD",
                name = "Gold Spot",
                category = MarketCategory.COMMODITIES,
                currentPrice = 2735.80,
                priceChange24h = 18.40,
                priceChangePercentage24h = 0.68,
                currency = "USD",
                exchange = "COMEX",
                historicalCandles = generateMockCandles(2735.80, 0.68),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "oil_wti",
                symbol = "WTI",
                name = "Crude Oil WTI",
                category = MarketCategory.COMMODITIES,
                currentPrice = 71.42,
                priceChange24h = -1.12,
                priceChangePercentage24h = -1.54,
                currency = "USD",
                exchange = "NYMEX",
                historicalCandles = generateMockCandles(71.42, -1.54),
                timestampEpoch = now,
                isDemo = true,
            ),
        )
    }

    private fun getEconomicsInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "us_10y",
                symbol = "US10Y",
                name = "US 10-Year Treasury Yield",
                category = MarketCategory.ECONOMICS,
                currentPrice = 4.24,
                priceChange24h = 0.03,
                priceChangePercentage24h = 0.71,
                currency = "%",
                exchange = "BOND",
                historicalCandles = generateMockCandles(4.24, 0.71),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "id_inflation",
                symbol = "ID-CPI",
                name = "Indonesia Inflation Rate (YoY)",
                category = MarketCategory.ECONOMICS,
                currentPrice = 2.57,
                priceChange24h = -0.05,
                priceChangePercentage24h = -1.91,
                currency = "%",
                exchange = "BPS",
                historicalCandles = generateMockCandles(2.57, -1.91),
                timestampEpoch = now,
                isDemo = true,
            ),
        )
    }

    private fun getEtfsInstruments(): List<MarketInstrument> {
        val now = System.currentTimeMillis()
        return listOf(
            MarketInstrument(
                id = "spy",
                symbol = "SPY",
                name = "SPDR S&P 500 ETF Trust",
                category = MarketCategory.ETFS,
                currentPrice = 580.40,
                priceChange24h = 3.10,
                priceChangePercentage24h = 0.54,
                currency = "USD",
                exchange = "NYSE",
                historicalCandles = generateMockCandles(580.40, 0.54),
                timestampEpoch = now,
                isDemo = true,
            ),
            MarketInstrument(
                id = "qqq",
                symbol = "QQQ",
                name = "Invesco QQQ Trust",
                category = MarketCategory.ETFS,
                currentPrice = 498.20,
                priceChange24h = 4.50,
                priceChangePercentage24h = 0.91,
                currency = "USD",
                exchange = "NASDAQ",
                historicalCandles = generateMockCandles(498.20, 0.91),
                timestampEpoch = now,
                isDemo = true,
            ),
        )
    }

    private fun generateMockCandles(basePrice: Double, change24h: Double): List<CandleData> {
        val candles = mutableListOf<CandleData>()
        val count = 30
        val startTime = System.currentTimeMillis() - (count * 24 * 60 * 60 * 1000L)
        var price = basePrice * (1.0 - (change24h / 100.0))

        for (i in 0 until count) {
            val time = startTime + (i * 24 * 60 * 60 * 1000L)
            val variation = price * 0.01 * sin(i.toDouble())
            val open = price
            val close = price + variation
            val high = maxOf(open, close) + (abs(variation) * 0.5)
            val low = minOf(open, close) - (abs(variation) * 0.5)
            val volume = 1000000f + (abs(variation) * 50000f).toFloat()

            candles.add(
                CandleData(
                    timestamp = time,
                    open = open.toFloat(),
                    high = high.toFloat(),
                    low = low.toFloat(),
                    close = close.toFloat(),
                    volume = volume,
                ),
            )
            price = close
        }
        return candles
    }

    private fun abs(d: Double): Double = if (d < 0) -d else d

    // Watchlist Management
    fun getWatchlist(): List<WatchlistEntry> {
        val json = prefs.getString("watchlist_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchlistEntry>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toggleWatchlist(instrument: MarketInstrument): Boolean {
        val current = getWatchlist().toMutableList()
        val existing = current.find { it.instrumentId == instrument.id }
        val isAdded = if (existing != null) {
            current.remove(existing)
            false
        } else {
            current.add(
                WatchlistEntry(
                    instrumentId = instrument.id,
                    symbol = instrument.symbol,
                    name = instrument.name,
                    category = instrument.category,
                ),
            )
            true
        }
        prefs.edit().putString("watchlist_json", gson.toJson(current)).apply()
        return isAdded
    }

    fun isInWatchlist(instrumentId: String): Boolean {
        return getWatchlist().any { it.instrumentId == instrumentId }
    }

    // Portfolio Management
    fun getPortfolioHoldings(): List<PortfolioHolding> {
        val json = prefs.getString("portfolio_json", null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PortfolioHolding>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun addPortfolioHolding(holding: PortfolioHolding) {
        val current = getPortfolioHoldings().toMutableList()
        current.add(holding)
        prefs.edit().putString("portfolio_json", gson.toJson(current)).apply()
    }

    @Suppress("unused")
    fun removePortfolioHolding(holdingId: String) {
        val current = getPortfolioHoldings().toMutableList()
        current.removeIf { it.id == holdingId }
        prefs.edit().putString("portfolio_json", gson.toJson(current)).apply()
    }

    companion object {
        @Volatile
        private var INSTANCE: MarketRepository? = null

        fun getInstance(context: Context): MarketRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MarketRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
