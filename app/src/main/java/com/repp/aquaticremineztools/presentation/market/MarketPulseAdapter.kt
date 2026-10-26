package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.presentation.home.SparklineView
import java.util.Locale

class MarketPulseAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<MarketPulseAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_market_pulse_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPulseSymbol: TextView = itemView.findViewById(R.id.tvPulseSymbol)
        private val tvPulseExchange: TextView = itemView.findViewById(R.id.tvPulseExchange)
        private val tvPulseName: TextView = itemView.findViewById(R.id.tvPulseName)
        private val tvPulsePrice: TextView = itemView.findViewById(R.id.tvPulsePrice)
        private val tvPulseChange: TextView = itemView.findViewById(R.id.tvPulseChange)
        private val sparklinePulse: SparklineView = itemView.findViewById(R.id.sparklinePulse)
        private val tvPulseNoFeed: TextView = itemView.findViewById(R.id.tvPulseNoFeed)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvPulseSymbol.text = instrument.symbol
            tvPulseExchange.text = instrument.exchange
            tvPulseName.text = instrument.name

            val price = instrument.currentPrice
            if (price != null) {
                val formatted = when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                    "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                    else -> String.format(Locale.US, "$%,.2f", price)
                }
                tvPulsePrice.text = formatted
            } else {
                tvPulsePrice.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvPulseChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvPulseChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvPulseChange.text = "No Feed"
                tvPulseChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            val sparkline = instrument.sparklinePrices
            if (!sparkline.isNullOrEmpty()) {
                sparklinePulse.visibility = View.VISIBLE
                tvPulseNoFeed.visibility = View.GONE
                val isPos = (pctChange ?: 0.0) >= 0
                sparklinePulse.setSparklineData(sparkline, isPos)
            } else {
                sparklinePulse.visibility = View.GONE
                tvPulseNoFeed.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
