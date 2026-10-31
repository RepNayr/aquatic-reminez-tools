package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

class ForexMatrixAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<ForexMatrixAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_forex_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivForexIcon: ImageView = itemView.findViewById(R.id.ivForexIcon)
        private val tvForexPair: TextView = itemView.findViewById(R.id.tvForexPair)
        private val tvForexStatusBadge: TextView = itemView.findViewById(R.id.tvForexStatusBadge)
        private val tvForexQuoteConvention: TextView = itemView.findViewById(R.id.tvForexQuoteConvention)
        private val tvForexRate: TextView = itemView.findViewById(R.id.tvForexRate)
        private val tvForexChange: TextView = itemView.findViewById(R.id.tvForexChange)
        private val tvForexProviderInfo: TextView = itemView.findViewById(R.id.tvForexProviderInfo)
        private val tvForexBidAsk: TextView = itemView.findViewById(R.id.tvForexBidAsk)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            val symbol = instrument.symbol
            val pairKey = symbol.replace("/", "_").lowercase(Locale.ROOT)

            tvForexPair.text = symbol

            val iconRes = when (pairKey) {
                "usd_idr", "usdidr" -> R.drawable.ic_asset_forex_usdidr
                "eur_usd", "eurusd" -> R.drawable.ic_asset_forex_eurusd
                "gbp_usd", "gbpusd" -> R.drawable.ic_asset_forex_gbpusd
                "usd_jpy", "usdjpy" -> R.drawable.ic_asset_forex_usdjpy
                "aud_usd", "audusd" -> R.drawable.ic_asset_forex_audusd
                "usd_chf", "usdchf" -> R.drawable.ic_asset_forex_usdchf
                else -> R.drawable.ic_asset_fallback_forex
            }
            ivForexIcon.setImageResource(iconRes)

            val quoteConvention = when (pairKey) {
                "usd_idr", "usdidr" -> "IDR per USD"
                "eur_usd", "eurusd" -> "USD per EUR"
                "gbp_usd", "gbpusd" -> "USD per GBP"
                "usd_jpy", "usdjpy" -> "JPY per USD"
                "aud_usd", "audusd" -> "USD per AUD"
                "usd_chf", "usdchf" -> "CHF per USD"
                else -> "Exchange Rate"
            }
            tvForexQuoteConvention.text = quoteConvention

            val price = instrument.currentPrice
            if (price != null) {
                val formatted = if (price >= 100) {
                    String.format(Locale.US, "%,.2f", price)
                } else {
                    String.format(Locale.US, "%,.4f", price)
                }
                tvForexRate.text = formatted
            } else {
                tvForexRate.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvForexChange.text = String.format(Locale.US, "%s%.2f%% (24h)", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvForexChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else if (price != null) {
                tvForexChange.text = "Rate Only (Delta N/A)"
                tvForexChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            } else {
                tvForexChange.text = "Feed Unavailable"
                tvForexChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_muted_red))
            }

            val status = instrument.dataStatus
            when (status) {
                MarketDataStatus.LIVE -> {
                    tvForexStatusBadge.text = "LIVE"
                    tvForexStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_live)
                    tvForexStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_online))
                }
                MarketDataStatus.CACHED -> {
                    tvForexStatusBadge.text = "CACHED"
                    tvForexStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_cached)
                    tvForexStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_blue))
                }
                MarketDataStatus.SIMULATED -> {
                    tvForexStatusBadge.text = "SIMULATED"
                    tvForexStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_simulated)
                    tvForexStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_gold))
                }
                MarketDataStatus.UNAVAILABLE -> {
                    tvForexStatusBadge.text = "UNAVAILABLE"
                    tvForexStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_unavailable)
                    tvForexStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
                }
            }

            tvForexProviderInfo.text = "Spot · ExchangeRate-API"
            tvForexBidAsk.text = "Bid/Ask: N/A"

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
