package com.repp.aquaticremineztools.presentation.market

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketInstrument

object MarketAssetIconHelper {

    private val localDrawableMap = mapOf(
        // Crypto
        "bitcoin" to R.drawable.ic_asset_crypto_btc,
        "btc" to R.drawable.ic_asset_crypto_btc,
        "ethereum" to R.drawable.ic_asset_crypto_eth,
        "eth" to R.drawable.ic_asset_crypto_eth,
        "solana" to R.drawable.ic_asset_crypto_sol,
        "sol" to R.drawable.ic_asset_crypto_sol,
        "binancecoin" to R.drawable.ic_asset_crypto_bnb,
        "bnb" to R.drawable.ic_asset_crypto_bnb,
        "travala" to R.drawable.ic_crypto_travala,
        "ava" to R.drawable.ic_crypto_travala,

        // Indonesian Stocks
        "bbca" to R.drawable.ic_asset_stock_bbca,
        "bbri" to R.drawable.ic_asset_stock_bbri,
        "bmri" to R.drawable.ic_asset_stock_bmri,

        // Global Stocks
        "aapl" to R.drawable.ic_asset_stock_aapl,
        "nvda" to R.drawable.ic_asset_stock_nvda,
        "tsla" to R.drawable.ic_asset_stock_tsla,

        // Indices
        "ihsg" to R.drawable.ic_asset_index_ihsg,
        "sp500" to R.drawable.ic_asset_index_sp500,
        "spx" to R.drawable.ic_asset_index_sp500,
        "nasdaq" to R.drawable.ic_asset_index_nasdaq,
        "ixic" to R.drawable.ic_asset_index_nasdaq,
        "nikkei" to R.drawable.ic_asset_index_nikkei,
        "ni225" to R.drawable.ic_asset_index_nikkei,

        // Forex
        "usd_idr" to R.drawable.ic_asset_forex_usdidr,
        "usd/idr" to R.drawable.ic_asset_forex_usdidr,
        "eur_usd" to R.drawable.ic_asset_forex_eurusd,
        "eur/usd" to R.drawable.ic_asset_forex_eurusd,
        "gbp_usd" to R.drawable.ic_asset_forex_gbpusd,
        "gbp/usd" to R.drawable.ic_asset_forex_gbpusd,
        "usd_jpy" to R.drawable.ic_asset_forex_usdjpy,
        "usd/jpy" to R.drawable.ic_asset_forex_usdjpy,
        "aud_usd" to R.drawable.ic_asset_forex_audusd,
        "aud/usd" to R.drawable.ic_asset_forex_audusd,
        "usd_chf" to R.drawable.ic_asset_forex_usdchf,
        "usd/chf" to R.drawable.ic_asset_forex_usdchf,

        // ETFs
        "spy" to R.drawable.ic_asset_etf_spy,
        "qqq" to R.drawable.ic_asset_etf_qqq,

        // Commodities
        "gold" to R.drawable.ic_asset_commodity_gold,
        "xau/usd" to R.drawable.ic_asset_commodity_gold,
        "silver" to R.drawable.ic_asset_commodity_silver,
        "xag/usd" to R.drawable.ic_asset_commodity_silver,
        "oil_wti" to R.drawable.ic_asset_commodity_oil,
        "wti" to R.drawable.ic_asset_commodity_oil,
        "nat_gas" to R.drawable.ic_asset_commodity_gas,
        "ng" to R.drawable.ic_asset_commodity_gas,

        // Economics
        "bi_rate" to R.drawable.ic_asset_eco_rate,
        "bi7drr" to R.drawable.ic_asset_eco_rate,
        "fed_rate" to R.drawable.ic_asset_eco_rate,
        "fedfunds" to R.drawable.ic_asset_eco_rate,
        "id_inflation" to R.drawable.ic_asset_eco_inflation,
        "id-cpi" to R.drawable.ic_asset_eco_inflation,
        "us_10y" to R.drawable.ic_asset_eco_yield,
        "us10y" to R.drawable.ic_asset_eco_yield,
        "id_gdp" to R.drawable.ic_asset_eco_gdp,
        "id-gdp" to R.drawable.ic_asset_eco_gdp
    )

    fun getCategoryFallbackDrawable(category: MarketCategory): Int {
        return when (category) {
            MarketCategory.CRYPTO -> R.drawable.ic_asset_fallback_crypto
            MarketCategory.STOCKS -> R.drawable.ic_asset_fallback_stock
            MarketCategory.INDICES -> R.drawable.ic_asset_fallback_index
            MarketCategory.FOREX -> R.drawable.ic_asset_fallback_forex
            MarketCategory.ETFS -> R.drawable.ic_asset_fallback_etf
            MarketCategory.COMMODITIES -> R.drawable.ic_asset_fallback_commodity
            MarketCategory.ECONOMICS -> R.drawable.ic_asset_fallback_eco
            else -> R.drawable.ic_asset_fallback_stock
        }
    }

    fun bindIcon(
        imageView: ImageView,
        fallbackTextView: TextView?,
        instrument: MarketInstrument
    ) {
        val keyId = instrument.id.lowercase()
        val keySymbol = instrument.symbol.lowercase()

        val localResId = localDrawableMap[keyId] ?: localDrawableMap[keySymbol]

        if (localResId != null) {
            fallbackTextView?.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(localResId)
        } else if (!instrument.imageUrl.isNullOrEmpty()) {
            fallbackTextView?.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.load(instrument.imageUrl) {
                crossfade(enable = true)
                listener(
                    onError = { _, _ ->
                        applyFallback(imageView, fallbackTextView, instrument)
                    }
                )
            }
        } else {
            applyFallback(imageView, fallbackTextView, instrument)
        }
    }

    private fun applyFallback(
        imageView: ImageView,
        fallbackTextView: TextView?,
        instrument: MarketInstrument
    ) {
        val fallbackDrawableRes = getCategoryFallbackDrawable(instrument.category)
        if (fallbackTextView != null) {
            imageView.visibility = View.GONE
            fallbackTextView.visibility = View.VISIBLE
            fallbackTextView.text = instrument.symbol.take(3).uppercase()
        } else {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(fallbackDrawableRes)
        }
    }
}
