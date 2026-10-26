package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

class StockWatchlistAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<StockWatchlistAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stock_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStockTickerBadge: TextView = itemView.findViewById(R.id.tvStockTickerBadge)
        private val tvStockName: TextView = itemView.findViewById(R.id.tvStockName)
        private val tvStockExchange: TextView = itemView.findViewById(R.id.tvStockExchange)
        private val tvStockPrice: TextView = itemView.findViewById(R.id.tvStockPrice)
        private val tvStockChange: TextView = itemView.findViewById(R.id.tvStockChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvStockTickerBadge.text = instrument.symbol
            tvStockName.text = instrument.name
            tvStockExchange.text = "${instrument.exchange} · ${instrument.currency}"

            val price = instrument.currentPrice
            if (price != null) {
                val formatted = when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.0f", price)
                    else -> String.format(Locale.US, "$%,.2f", price)
                }
                tvStockPrice.text = formatted
            } else {
                tvStockPrice.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvStockChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvStockChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvStockChange.text = "Feed Pending"
                tvStockChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
