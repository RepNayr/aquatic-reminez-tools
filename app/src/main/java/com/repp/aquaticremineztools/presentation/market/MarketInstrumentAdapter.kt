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
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

class MarketInstrumentAdapter(
    private var instruments: List<MarketInstrument>,
    private val onItemClick: (MarketInstrument) -> Unit,
) : RecyclerView.Adapter<MarketInstrumentAdapter.ViewHolder>() {

    fun updateData(newInstruments: List<MarketInstrument>) {
        instruments = newInstruments
        notifyItemRangeChanged(0, instruments.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_market_instrument, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val instrument = instruments[position]
        holder.bind(instrument, onItemClick)
    }

    override fun getItemCount(): Int = instruments.size

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

            // Logo or Fallback
            if (!instrument.imageUrl.isNullOrEmpty()) {
                tvInstrumentFallback.visibility = View.GONE
                ivInstrumentLogo.visibility = View.VISIBLE
                ivInstrumentLogo.load(instrument.imageUrl) {
                    crossfade(enable = true)
                    listener(
                        onError = { _, _ ->
                            ivInstrumentLogo.visibility = View.GONE
                            tvInstrumentFallback.visibility = View.VISIBLE
                            tvInstrumentFallback.text = instrument.symbol.take(3)
                        },
                    )
                }
            } else {
                ivInstrumentLogo.visibility = View.GONE
                tvInstrumentFallback.visibility = View.VISIBLE
                tvInstrumentFallback.text = instrument.symbol.take(3)
            }

            // Price formatting
            if (instrument.currentPrice != null) {
                val formattedPrice = if (instrument.currency == "IDR") {
                    String.format(Locale.US, "Rp %,.2f", instrument.currentPrice)
                } else if (instrument.currency == "%") {
                    String.format(Locale.US, "%.2f%%", instrument.currentPrice)
                } else if (instrument.currentPrice >= 1.0) {
                    String.format(Locale.US, "$%,.2f", instrument.currentPrice)
                } else {
                    String.format(Locale.US, "$%,.4f", instrument.currentPrice)
                }
                tvInstrumentPrice.text = formattedPrice
                tvInstrumentPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            } else {
                tvInstrumentPrice.text = "--"
                tvInstrumentPrice.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            // Change formatting
            if (instrument.priceChangePercentage24h != null) {
                val isPositive = instrument.priceChangePercentage24h >= 0
                val changeText = if (isPositive) {
                    String.format(Locale.US, "+%.2f%%", instrument.priceChangePercentage24h)
                } else {
                    String.format(Locale.US, "%.2f%%", instrument.priceChangePercentage24h)
                }
                tvInstrumentChange.text = changeText
                val colorRes = if (isPositive) R.color.accent_teal else R.color.accent_muted_red
                tvInstrumentChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
                tvInstrumentChange.visibility = View.VISIBLE
            } else {
                tvInstrumentChange.text = "--"
                tvInstrumentChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
