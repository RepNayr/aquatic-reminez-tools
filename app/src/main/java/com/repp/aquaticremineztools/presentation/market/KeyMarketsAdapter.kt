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
import com.repp.aquaticremineztools.presentation.home.SparklineView
import java.util.Locale

class KeyMarketsAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<KeyMarketsAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_key_market_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivKeyLogo: ImageView = itemView.findViewById(R.id.ivKeyLogo)
        private val tvKeyFallback: TextView = itemView.findViewById(R.id.tvKeyFallback)
        private val tvKeySymbol: TextView = itemView.findViewById(R.id.tvKeySymbol)
        private val tvKeyCountry: TextView = itemView.findViewById(R.id.tvKeyCountry)
        private val tvKeyName: TextView = itemView.findViewById(R.id.tvKeyName)
        private val tvKeyStatus: TextView = itemView.findViewById(R.id.tvKeyStatus)
        private val sparklineKey: SparklineView = itemView.findViewById(R.id.sparklineKey)
        private val tvKeyPrice: TextView = itemView.findViewById(R.id.tvKeyPrice)
        private val tvKeyChange: TextView = itemView.findViewById(R.id.tvKeyChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvKeySymbol.text = instrument.symbol
            tvKeyCountry.text = instrument.countryCode ?: instrument.exchange
            tvKeyName.text = instrument.name

            // Bind Asset Icon using MarketAssetIconHelper
            MarketAssetIconHelper.bindIcon(ivKeyLogo, tvKeyFallback, instrument)

            // Status Badge
            val statusText = when (instrument.dataStatus) {
                MarketDataStatus.LIVE -> "LIVE"
                MarketDataStatus.CACHED -> "CACHED"
                MarketDataStatus.SIMULATED -> "SIM"
                MarketDataStatus.UNAVAILABLE -> "N/A"
            }
            tvKeyStatus.text = statusText
            val statusColor = when (instrument.dataStatus) {
                MarketDataStatus.LIVE -> R.color.accent_teal
                MarketDataStatus.CACHED -> R.color.accent_blue
                MarketDataStatus.SIMULATED -> R.color.accent_gold
                MarketDataStatus.UNAVAILABLE -> R.color.text_muted
            }
            tvKeyStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))

            val price = instrument.currentPrice
            if (price != null) {
                val formatted = when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                    "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                    "EUR" -> String.format(Locale.US, "€%,.2f", price)
                    else -> String.format(Locale.US, "$%,.2f", price)
                }
                tvKeyPrice.text = formatted
            } else {
                tvKeyPrice.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvKeyChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvKeyChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvKeyChange.text = "No Feed"
                tvKeyChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            // Sparkline
            val sparkline = instrument.sparklinePrices
            if (!sparkline.isNullOrEmpty()) {
                sparklineKey.visibility = View.VISIBLE
                val isPos = (pctChange ?: 0.0) >= 0
                sparklineKey.setSparklineData(sparkline, isPos)
            } else {
                sparklineKey.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
