package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketInstrument

class EconomicIndicatorAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<EconomicIndicatorAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_economic_indicator_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEconSymbol: TextView = itemView.findViewById(R.id.tvEconSymbol)
        private val tvEconCountry: TextView = itemView.findViewById(R.id.tvEconCountry)
        private val tvEconName: TextView = itemView.findViewById(R.id.tvEconName)
        private val tvEconRate: TextView = itemView.findViewById(R.id.tvEconRate)
        private val tvEconPeriod: TextView = itemView.findViewById(R.id.tvEconPeriod)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvEconSymbol.text = instrument.symbol
            tvEconCountry.text = instrument.countryCode ?: instrument.exchange
            tvEconName.text = instrument.name

            val price = instrument.currentPrice
            if (price != null) {
                tvEconRate.text = "${price}%"
            } else {
                tvEconRate.text = "--"
            }

            tvEconPeriod.text = instrument.macroPeriod ?: "Indicator"

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
