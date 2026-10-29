package com.repp.aquaticremineztools.presentation.market

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

class HeroInstrumentSpinnerAdapter(
    context: Context,
    private var items: List<MarketInstrument>
) : ArrayAdapter<MarketInstrument>(context, 0, items) {

    var selectedBenchmarkId: String = ""

    fun updateData(newItems: List<MarketInstrument>, currentSelectedId: String) {
        this.items = newItems
        this.selectedBenchmarkId = currentSelectedId
        clear()
        addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): MarketInstrument? = items.getOrNull(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position) ?: return View(context)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dropdown_hero_selected, parent, false)

        val ivSelectedLogo = view.findViewById<ImageView>(R.id.ivSelectedLogo)
        val tvSelectedFallback = view.findViewById<TextView>(R.id.tvSelectedFallback)
        val tvSelectedSymbolName = view.findViewById<TextView>(R.id.tvSelectedSymbolName)
        val tvSelectedSubtitle = view.findViewById<TextView>(R.id.tvSelectedSubtitle)

        tvSelectedSymbolName.text = "${item.symbol} — ${item.name}"
        tvSelectedSubtitle.text = buildSubtitle(item)

        MarketAssetIconHelper.bindIcon(ivSelectedLogo, tvSelectedFallback, item)

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position) ?: return View(context)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dropdown_hero_row, parent, false)

        val layoutRow = view.findViewById<View>(R.id.layoutDropdownRow)
        val ivRowLogo = view.findViewById<ImageView>(R.id.ivRowLogo)
        val tvRowFallback = view.findViewById<TextView>(R.id.tvRowFallback)
        val tvRowSymbolName = view.findViewById<TextView>(R.id.tvRowSymbolName)
        val tvRowSubtitle = view.findViewById<TextView>(R.id.tvRowSubtitle)
        val tvRowPrice = view.findViewById<TextView>(R.id.tvRowPrice)
        val tvRowChange = view.findViewById<TextView>(R.id.tvRowChange)
        val ivRowCheck = view.findViewById<ImageView>(R.id.ivRowCheck)

        tvRowSymbolName.text = "${item.symbol} — ${item.name}"
        tvRowSubtitle.text = buildSubtitle(item)

        MarketAssetIconHelper.bindIcon(ivRowLogo, tvRowFallback, item)

        val isSelected = item.id == selectedBenchmarkId
        if (isSelected) {
            layoutRow.setBackgroundResource(R.drawable.bg_dropdown_item_selected)
            ivRowCheck.visibility = View.VISIBLE
        } else {
            layoutRow.setBackgroundResource(R.drawable.bg_dropdown_item_normal)
            ivRowCheck.visibility = View.GONE
        }

        val price = item.currentPrice
        if (price != null) {
            val formattedPrice = when (item.currency) {
                "IDR" -> String.format(Locale.US, "Rp %,.2f", price)
                "%" -> String.format(Locale.US, "%.2f%%", price)
                "JPY" -> String.format(Locale.US, "¥%,.2f", price)
                "EUR" -> String.format(Locale.US, "€%,.4f", price)
                else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
            }
            tvRowPrice.text = formattedPrice
            tvRowPrice.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        } else {
            tvRowPrice.text = when (item.dataStatus) {
                MarketDataStatus.UNAVAILABLE -> "N/A"
                else -> "--"
            }
            tvRowPrice.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
        }

        val pctChange = item.priceChangePercentage24h
        if (pctChange != null) {
            val isPositive = pctChange >= 0
            val sign = if (isPositive) "+" else ""
            tvRowChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
            val colorRes = if (isPositive) R.color.accent_teal else R.color.accent_muted_red
            tvRowChange.setTextColor(ContextCompat.getColor(context, colorRes))
            tvRowChange.visibility = View.VISIBLE
        } else {
            val statusLabel = when (item.dataStatus) {
                MarketDataStatus.SIMULATED -> "SIMULATED"
                MarketDataStatus.CACHED -> "CACHED"
                MarketDataStatus.LIVE -> "LIVE"
                MarketDataStatus.UNAVAILABLE -> "NO FEED"
            }
            tvRowChange.text = statusLabel
            tvRowChange.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            tvRowChange.visibility = View.VISIBLE
        }

        return view
    }

    private fun buildSubtitle(instrument: MarketInstrument): String {
        val parts = mutableListOf<String>()
        if (instrument.exchange.isNotBlank()) parts.add(instrument.exchange)
        instrument.countryCode?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        parts.add(instrument.category.displayName)
        return parts.joinToString(" · ")
    }
}
