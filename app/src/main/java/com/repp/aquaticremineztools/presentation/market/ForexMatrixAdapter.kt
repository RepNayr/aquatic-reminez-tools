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
        private val tvForexPair: TextView = itemView.findViewById(R.id.tvForexPair)
        private val tvForexRate: TextView = itemView.findViewById(R.id.tvForexRate)
        private val tvForexChange: TextView = itemView.findViewById(R.id.tvForexChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvForexPair.text = instrument.symbol

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
                tvForexChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvForexChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvForexChange.text = "Feed Pending"
                tvForexChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
