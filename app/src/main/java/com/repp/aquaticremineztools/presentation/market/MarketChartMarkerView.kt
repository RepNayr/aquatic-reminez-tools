package com.repp.aquaticremineztools.presentation.market

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.repp.aquaticremineztools.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketChartMarkerView(
    context: Context,
    layoutResource: Int,
    private val currencySymbol: String = "$"
) : MarkerView(context, layoutResource) {

    private val tvMarkerDate: TextView = findViewById(R.id.tvMarkerDate)
    private val tvMarkerPrice: TextView = findViewById(R.id.tvMarkerPrice)
    private val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.US)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e == null) return

        val price = e.y
        val formattedPrice = when (currencySymbol) {
            "IDR", "Rp" -> String.format(Locale.US, "Rp %,.2f", price)
            "JPY", "¥" -> String.format(Locale.US, "¥%,.2f", price)
            "%" -> String.format(Locale.US, "%.2f%%", price)
            else -> if (price >= 1.0) String.format(Locale.US, "$%,.2f", price) else String.format(Locale.US, "$%,.4f", price)
        }
        tvMarkerPrice.text = formattedPrice

        val data = e.data
        if (data is Long && data > 0L) {
            tvMarkerDate.text = dateFormat.format(Date(data))
        } else if (data is String) {
            tvMarkerDate.text = data
        } else {
            tvMarkerDate.text = "Point #${e.x.toInt()}"
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat() - 10f)
    }
}
