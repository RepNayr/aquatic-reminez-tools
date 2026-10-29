package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

class MarketInstrumentAdapter(
    private val onItemClick: (MarketInstrument) -> Unit,
) : ListAdapter<MarketInstrument, MarketInstrumentAdapter.ViewHolder>(DiffCallback) {

    constructor(initialList: List<MarketInstrument>, onItemClick: (MarketInstrument) -> Unit) : this(onItemClick) {
        submitList(initialList)
    }

    fun updateData(newInstruments: List<MarketInstrument>) {
        submitList(newInstruments)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_market_instrument, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivInstrumentLogo = itemView.findViewById<ImageView>(R.id.ivInstrumentLogo)
        private val tvInstrumentFallback = itemView.findViewById<TextView>(R.id.tvInstrumentFallback)
        private val tvInstrumentName = itemView.findViewById<TextView>(R.id.tvInstrumentName)
        private val tvInstrumentSymbol = itemView.findViewById<TextView>(R.id.tvInstrumentSymbol)
        private val tvInstrumentExchange = itemView.findViewById<TextView>(R.id.tvInstrumentExchange)
        private val tvInstrumentPrice = itemView.findViewById<TextView>(R.id.tvInstrumentPrice)
        private val tvInstrumentChange = itemView.findViewById<TextView>(R.id.tvInstrumentChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvInstrumentName.text = instrument.name
            tvInstrumentSymbol.text = instrument.symbol
            tvInstrumentExchange.text = instrument.exchange

            // Logo or Fallback using MarketAssetIconHelper
            MarketAssetIconHelper.bindIcon(ivInstrumentLogo, tvInstrumentFallback, instrument)

            // Price formatting
            val price = instrument.currentPrice
            if (price != null) {
                val formattedPrice = when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                    "%" -> String.format(Locale.US, "%.2f%%", price)
                    "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                    "EUR" -> String.format(Locale.US, "€%,.4f", price)
                    else -> if (price >= 1.0) {
                        String.format(Locale.US, "$%,.2f", price)
                    } else {
                        String.format(Locale.US, "$%,.4f", price)
                    }
                }
                tvInstrumentPrice.text = formattedPrice
                tvInstrumentPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            } else {
                tvInstrumentPrice.text = when (instrument.dataStatus) {
                    MarketDataStatus.UNAVAILABLE -> "N/A"
                    else -> "--"
                }
                tvInstrumentPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            // Change formatting
            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPositive = pctChange >= 0
                val changeText = if (isPositive) {
                    String.format(Locale.US, "+%.2f%%", pctChange)
                } else {
                    String.format(Locale.US, "%.2f%%", pctChange)
                }
                tvInstrumentChange.text = changeText
                val colorRes = if (isPositive) R.color.accent_teal else R.color.accent_muted_red
                tvInstrumentChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
                tvInstrumentChange.visibility = View.VISIBLE
            } else {
                tvInstrumentChange.text = if (instrument.currentPrice == null) "No Feed" else "--"
                tvInstrumentChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
                tvInstrumentChange.visibility = View.VISIBLE
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<MarketInstrument>() {
        override fun areItemsTheSame(oldItem: MarketInstrument, newItem: MarketInstrument): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: MarketInstrument, newItem: MarketInstrument): Boolean =
            oldItem == newItem
    }
}
