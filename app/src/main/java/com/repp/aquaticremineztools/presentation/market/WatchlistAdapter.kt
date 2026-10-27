package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.presentation.home.SparklineView
import java.util.Locale

class WatchlistAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_watchlist_glass_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivWlLogo: ImageView = itemView.findViewById(R.id.ivWlLogo)
        private val tvWlFallback: TextView = itemView.findViewById(R.id.tvWlFallback)
        private val tvWlSymbol: TextView = itemView.findViewById(R.id.tvWlSymbol)
        private val tvWlName: TextView = itemView.findViewById(R.id.tvWlName)
        private val tvWlStatus: TextView = itemView.findViewById(R.id.tvWlStatus)
        private val sparklineWl: SparklineView = itemView.findViewById(R.id.sparklineWl)
        private val tvWlPrice: TextView = itemView.findViewById(R.id.tvWlPrice)
        private val tvWlChange: TextView = itemView.findViewById(R.id.tvWlChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvWlSymbol.text = instrument.symbol
            tvWlName.text = instrument.name

            // Logo
            if (!instrument.imageUrl.isNullOrEmpty()) {
                tvWlFallback.visibility = View.GONE
                ivWlLogo.visibility = View.VISIBLE
                ivWlLogo.load(instrument.imageUrl) {
                    crossfade(true)
                    listener(
                        onError = { _, _ ->
                            ivWlLogo.visibility = View.GONE
                            tvWlFallback.visibility = View.VISIBLE
                            tvWlFallback.text = instrument.symbol.take(3)
                        }
                    )
                }
            } else {
                ivWlLogo.visibility = View.GONE
                tvWlFallback.visibility = View.VISIBLE
                tvWlFallback.text = instrument.symbol.take(3)
            }

            // Status Badge
            tvWlStatus.text = when (instrument.dataStatus) {
                MarketDataStatus.LIVE -> "LIVE"
                MarketDataStatus.CACHED -> "CACHED"
                MarketDataStatus.SIMULATED -> "DEMO"
                MarketDataStatus.UNAVAILABLE -> "N/A"
            }

            // Price Formatting
            val price = instrument.currentPrice
            if (price != null) {
                val formattedPrice = when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                    "%" -> String.format(Locale.US, "%.2f%%", price)
                    "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                    "EUR" -> String.format(Locale.US, "€%,.4f", price)
                    else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
                }
                tvWlPrice.text = formattedPrice
            } else {
                tvWlPrice.text = "--"
            }

            // 24h Change Formatting
            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvWlChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvWlChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvWlChange.text = "--"
            }

            // Sparkline
            val sparkline = instrument.sparklinePrices
            if (!sparkline.isNullOrEmpty()) {
                sparklineWl.visibility = View.VISIBLE
                val isPositive = (pctChange ?: 0.0) >= 0
                sparklineWl.setSparklineData(sparkline, isPositive)
            } else {
                sparklineWl.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
