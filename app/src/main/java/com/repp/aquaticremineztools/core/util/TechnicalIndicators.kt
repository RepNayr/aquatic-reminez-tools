package com.repp.aquaticremineztools.core.util

import kotlin.math.max

object TechnicalIndicators {

    fun calculateSMA(prices: List<Float>, period: Int = 20): List<Float?> {
        if (prices.size < period) return List(prices.size) { null }
        val result = ArrayList<Float?>(prices.size)
        var sum = 0f

        for (i in prices.indices) {
            sum += prices[i]
            if (i >= period) {
                sum -= prices[i - period]
            }
            if (i >= period - 1) {
                result.add(sum / period)
            } else {
                result.add(null)
            }
        }
        return result
    }

    fun calculateEMA(prices: List<Float>, period: Int = 50): List<Float?> {
        if (prices.size < period) return List(prices.size) { null }
        val result = ArrayList<Float?>(prices.size)
        val k = 2f / (period + 1f)

        var ema = prices.take(period).average().toFloat()
        for (i in 0 until period - 1) {
            result.add(null)
        }
        result.add(ema)

        for (i in period until prices.size) {
            ema = (prices[i] * k) + (ema * (1f - k))
            result.add(ema)
        }
        return result
    }

    fun calculateRSI(prices: List<Float>, period: Int = 14): Float? {
        if (prices.size <= period) return null
        var gains = 0f
        var losses = 0f

        for (i in 1..period) {
            val change = prices[i] - prices[i - 1]
            if (change >= 0) gains += change else losses += Math.abs(change)
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        for (i in (period + 1) until prices.size) {
            val change = prices[i] - prices[i - 1]
            val gain = if (change >= 0) change else 0f
            val loss = if (change < 0) Math.abs(change) else 0f

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        if (avgLoss == 0f) return 100f
        val rs = avgGain / avgLoss
        return 100f - (100f / (1f + rs))
    }

    fun calculateMACD(prices: List<Float>): Pair<Float, Float>? {
        if (prices.size < 26) return null
        val ema12List = calculateEMA(prices, 12)
        val ema26List = calculateEMA(prices, 26)

        val macdList = mutableListOf<Float>()
        for (i in prices.indices) {
            val e12 = ema12List[i]
            val e26 = ema26List[i]
            if (e12 != null && e26 != null) {
                macdList.add(e12 - e26)
            }
        }
        if (macdList.size < 9) return null
        val latestMacd = macdList.last()
        val signalList = calculateEMA(macdList, 9)
        val latestSignal = signalList.last() ?: 0f
        return Pair(latestMacd, latestSignal)
    }
}
