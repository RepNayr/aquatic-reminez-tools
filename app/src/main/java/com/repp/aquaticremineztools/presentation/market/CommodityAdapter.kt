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

class CommodityAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<CommodityAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_commodity_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCommSymbol: TextView = itemView.findViewById(R.id.tvCommSymbol)
        private val tvCommUnit: TextView = itemView.findViewById(R.id.tvCommUnit)
        private val tvCommName: TextView = itemView.findViewById(R.id.tvCommName)
        private val tvCommPrice: TextView = itemView.findViewById(R.id.tvCommPrice)
        private val tvCommChange: TextView = itemView.findViewById(R.id.tvCommChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvCommSymbol.text = instrument.symbol
            tvCommUnit.text = instrument.measurementUnit ?: instrument.currency
            tvCommName.text = instrument.name

            val price = instrument.currentPrice
            if (price != null) {
                tvCommPrice.text = String.format(Locale.US, "$%,.2f", price)
            } else {
                tvCommPrice.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvCommChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvCommChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvCommChange.text = "Feed Pending"
                tvCommChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
