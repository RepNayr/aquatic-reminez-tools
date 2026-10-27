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

class MarketMoversAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<MarketMoversAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_market_mover, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position + 1, onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMoverRank: TextView = itemView.findViewById(R.id.tvMoverRank)
        private val tvMoverSymbol: TextView = itemView.findViewById(R.id.tvMoverSymbol)
        private val tvMoverName: TextView = itemView.findViewById(R.id.tvMoverName)
        private val tvMoverPrice: TextView = itemView.findViewById(R.id.tvMoverPrice)
        private val tvMoverChange: TextView = itemView.findViewById(R.id.tvMoverChange)

        fun bind(instrument: MarketInstrument, rank: Int, onClick: (MarketInstrument) -> Unit) {
            tvMoverRank.text = rank.toString()
            tvMoverSymbol.text = instrument.symbol
            tvMoverName.text = instrument.name

            val price = instrument.currentPrice
            if (price != null) {
                val formatted = when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                    "%" -> String.format(Locale.US, "%.2f%%", price)
                    "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                    "EUR" -> String.format(Locale.US, "€%,.4f", price)
                    else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
                }
                tvMoverPrice.text = formatted
            } else {
                tvMoverPrice.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvMoverChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvMoverChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvMoverChange.text = "--"
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
