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
import com.repp.aquaticremineztools.presentation.home.SparklineView
import java.util.Locale

class CryptoDashboardAdapter(
    private var items: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<CryptoDashboardAdapter.ViewHolder>() {

    fun updateData(newItems: List<MarketInstrument>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_crypto_dashboard_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCryptoLogo: ImageView = itemView.findViewById(R.id.ivCryptoLogo)
        private val tvCryptoFallback: TextView = itemView.findViewById(R.id.tvCryptoFallback)
        private val tvCryptoSymbol: TextView = itemView.findViewById(R.id.tvCryptoSymbol)
        private val tvCryptoExchange: TextView = itemView.findViewById(R.id.tvCryptoExchange)
        private val sparklineCrypto: SparklineView = itemView.findViewById(R.id.sparklineCrypto)
        private val tvCryptoPrice: TextView = itemView.findViewById(R.id.tvCryptoPrice)
        private val tvCryptoChange: TextView = itemView.findViewById(R.id.tvCryptoChange)

        fun bind(instrument: MarketInstrument, onClick: (MarketInstrument) -> Unit) {
            tvCryptoSymbol.text = "${instrument.symbol} · ${instrument.name}"
            tvCryptoExchange.text = "${instrument.exchange} · ${instrument.currency}"

            if (instrument.id.equals("travala", ignoreCase = true)) {
                tvCryptoFallback.visibility = View.GONE
                ivCryptoLogo.visibility = View.VISIBLE
                ivCryptoLogo.setImageResource(R.drawable.ic_crypto_travala)
            } else if (!instrument.imageUrl.isNullOrEmpty()) {
                tvCryptoFallback.visibility = View.GONE
                ivCryptoLogo.visibility = View.VISIBLE
                ivCryptoLogo.load(instrument.imageUrl) {
                    crossfade(true)
                    listener(
                        onError = { _, _ ->
                            ivCryptoLogo.visibility = View.GONE
                            tvCryptoFallback.visibility = View.VISIBLE
                            tvCryptoFallback.text = instrument.symbol.take(3)
                        }
                    )
                }
            } else {
                ivCryptoLogo.visibility = View.GONE
                tvCryptoFallback.visibility = View.VISIBLE
                tvCryptoFallback.text = instrument.symbol.take(3)
            }

            val price = instrument.currentPrice
            if (price != null) {
                tvCryptoPrice.text = String.format(Locale.US, "$%,.2f", price)
            } else {
                tvCryptoPrice.text = "--"
            }

            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvCryptoChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvCryptoChange.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            } else {
                tvCryptoChange.text = "No Feed"
                tvCryptoChange.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
            }

            val sparkline = instrument.sparklinePrices
            if (!sparkline.isNullOrEmpty()) {
                sparklineCrypto.visibility = View.VISIBLE
                val isPos = (pctChange ?: 0.0) >= 0
                sparklineCrypto.setSparklineData(sparkline, isPos)
            } else {
                sparklineCrypto.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
