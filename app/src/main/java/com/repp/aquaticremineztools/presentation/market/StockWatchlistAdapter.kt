package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketCategory
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

sealed class StockAdapterItem {
    data class GroupHeader(
        val title: String,
        val exchange: String,
        val count: Int,
        val accentColorRes: Int
    ) : StockAdapterItem()

    data class InstrumentRow(
        val instrument: MarketInstrument
    ) : StockAdapterItem()
}

class StockWatchlistAdapter(
    private var rawItems: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_GROUP_HEADER = 0
        private const val VIEW_TYPE_INSTRUMENT_ROW = 1
    }

    private val adapterItems = mutableListOf<StockAdapterItem>()
    private var expandedSymbol: String? = null

    init {
        buildAdapterItems(rawItems)
    }

    fun updateData(newItems: List<MarketInstrument>) {
        rawItems = newItems
        buildAdapterItems(newItems)
        notifyDataSetChanged()
    }

    private fun buildAdapterItems(instruments: List<MarketInstrument>) {
        adapterItems.clear()
        if (instruments.isEmpty()) return

        // 1. Group A: Indonesia Stocks
        val indonesiaStocks = instruments.filter {
            it.countryCode == "ID" || it.currency == "IDR" || it.exchange.equals("IDX", ignoreCase = true)
        }

        // 2. Group B: United States Stocks
        val usStocks = instruments.filter {
            it.category == MarketCategory.STOCKS &&
                    it.countryCode != "ID" &&
                    it.currency != "IDR" &&
                    !it.exchange.equals("IDX", ignoreCase = true)
        }

        // 3. Group C: Exchange Traded Funds
        val etfs = instruments.filter {
            it.category == MarketCategory.ETFS
        }

        if (indonesiaStocks.isNotEmpty()) {
            adapterItems.add(
                StockAdapterItem.GroupHeader(
                    title = "INDONESIA STOCKS",
                    exchange = "IDX · INDONESIA STOCK EXCHANGE",
                    count = indonesiaStocks.size,
                    accentColorRes = R.color.accent_teal
                )
            )
            indonesiaStocks.forEach { adapterItems.add(StockAdapterItem.InstrumentRow(it)) }
        }

        if (usStocks.isNotEmpty()) {
            adapterItems.add(
                StockAdapterItem.GroupHeader(
                    title = "UNITED STATES STOCKS",
                    exchange = "NASDAQ / NYSE · UNITED STATES",
                    count = usStocks.size,
                    accentColorRes = R.color.accent_cyan
                )
            )
            usStocks.forEach { adapterItems.add(StockAdapterItem.InstrumentRow(it)) }
        }

        if (etfs.isNotEmpty()) {
            adapterItems.add(
                StockAdapterItem.GroupHeader(
                    title = "EXCHANGE TRADED FUNDS",
                    exchange = "GLOBAL EQUITIES · INDEX & SECTOR FUNDS",
                    count = etfs.size,
                    accentColorRes = R.color.accent_violet
                )
            )
            etfs.forEach { adapterItems.add(StockAdapterItem.InstrumentRow(it)) }
        }

        if (adapterItems.isEmpty() && instruments.isNotEmpty()) {
            adapterItems.add(
                StockAdapterItem.GroupHeader(
                    title = "EQUITIES & ETFS",
                    exchange = "GLOBAL MARKETS",
                    count = instruments.size,
                    accentColorRes = R.color.accent_cyan
                )
            )
            instruments.forEach { adapterItems.add(StockAdapterItem.InstrumentRow(it)) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (adapterItems[position]) {
            is StockAdapterItem.GroupHeader -> VIEW_TYPE_GROUP_HEADER
            is StockAdapterItem.InstrumentRow -> VIEW_TYPE_INSTRUMENT_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_GROUP_HEADER) {
            val view = inflater.inflate(R.layout.item_stock_group_header, parent, false)
            GroupHeaderViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_stock_row, parent, false)
            InstrumentRowViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = adapterItems[position]) {
            is StockAdapterItem.GroupHeader -> (holder as GroupHeaderViewHolder).bind(item)
            is StockAdapterItem.InstrumentRow -> (holder as InstrumentRowViewHolder).bind(
                item.instrument,
                isExpanded = (item.instrument.symbol == expandedSymbol),
                onToggleExpand = { instrument ->
                    expandedSymbol = if (expandedSymbol == instrument.symbol) null else instrument.symbol
                    notifyDataSetChanged()
                },
                onViewDetails = { instrument ->
                    onItemClick(instrument)
                }
            )
        }
    }

    override fun getItemCount(): Int = adapterItems.size

    class GroupHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewGroupAccent: View = itemView.findViewById(R.id.viewGroupAccent)
        private val tvGroupTitle: TextView = itemView.findViewById(R.id.tvGroupTitle)
        private val tvGroupExchange: TextView = itemView.findViewById(R.id.tvGroupExchange)
        private val tvGroupCount: TextView = itemView.findViewById(R.id.tvGroupCount)

        fun bind(header: StockAdapterItem.GroupHeader) {
            viewGroupAccent.setBackgroundColor(ContextCompat.getColor(itemView.context, header.accentColorRes))
            tvGroupTitle.text = header.title
            tvGroupExchange.text = header.exchange
            tvGroupCount.text = String.format(Locale.US, "%02d INSTRUMENTS", header.count)
        }
    }

    class InstrumentRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutCollapsedRow: View = itemView.findViewById(R.id.layoutCollapsedRow)
        private val ivStockLogo: ImageView = itemView.findViewById(R.id.ivStockLogo)
        private val tvStockTicker: TextView = itemView.findViewById(R.id.tvStockTicker)
        private val tvStockStatusBadge: TextView = itemView.findViewById(R.id.tvStockStatusBadge)
        private val tvStockName: TextView = itemView.findViewById(R.id.tvStockName)
        private val tvStockExchange: TextView = itemView.findViewById(R.id.tvStockExchange)
        private val tvStockPrice: TextView = itemView.findViewById(R.id.tvStockPrice)
        private val tvStockChange: TextView = itemView.findViewById(R.id.tvStockChange)
        private val ivExpandChevron: ImageView = itemView.findViewById(R.id.ivExpandChevron)

        // Expanded panel
        private val layoutExpandedPanel: View = itemView.findViewById(R.id.layoutExpandedPanel)
        private val viewExpandedAccent: View = itemView.findViewById(R.id.viewExpandedAccent)
        private val tvExpandedCategory: TextView = itemView.findViewById(R.id.tvExpandedCategory)
        private val tvDetailFullName: TextView = itemView.findViewById(R.id.tvDetailFullName)
        private val tvDetailExchangeCurrency: TextView = itemView.findViewById(R.id.tvDetailExchangeCurrency)
        private val tvDetailLastPrice: TextView = itemView.findViewById(R.id.tvDetailLastPrice)
        private val tvDetailMovement: TextView = itemView.findViewById(R.id.tvDetailMovement)
        private val tvHistoryStatusText: TextView = itemView.findViewById(R.id.tvHistoryStatusText)
        private val tvHistoryStatusSubtext: TextView = itemView.findViewById(R.id.tvHistoryStatusSubtext)
        private val tvFeedSource: TextView = itemView.findViewById(R.id.tvFeedSource)
        private val btnViewFullDetails: TextView = itemView.findViewById(R.id.btnViewFullDetails)

        fun bind(
            instrument: MarketInstrument,
            isExpanded: Boolean,
            onToggleExpand: (MarketInstrument) -> Unit,
            onViewDetails: (MarketInstrument) -> Unit
        ) {
            val context = itemView.context

            // 1. Authentic Logo
            MarketAssetIconHelper.bindIcon(ivStockLogo, null, instrument)
            ivStockLogo.contentDescription = "${instrument.name} Logo"

            // 2. Identity
            tvStockTicker.text = instrument.symbol
            tvStockName.text = instrument.name
            tvStockExchange.text = "${instrument.exchange} · ${instrument.currency}"

            // 3. Status Badge
            val (statusText, statusBg, statusTextColor) = when (instrument.dataStatus) {
                MarketDataStatus.LIVE -> Triple("LIVE", R.drawable.bg_terminal_badge_live, R.color.accent_teal)
                MarketDataStatus.CACHED -> Triple("CACHED", R.drawable.bg_terminal_badge_cached, R.color.accent_blue)
                MarketDataStatus.SIMULATED -> Triple("SIMULATED", R.drawable.bg_terminal_badge_simulated, R.color.accent_gold)
                MarketDataStatus.UNAVAILABLE -> Triple("UNAVAILABLE", R.drawable.bg_terminal_badge_unavailable, R.color.text_muted)
            }
            tvStockStatusBadge.text = statusText
            tvStockStatusBadge.setBackgroundResource(statusBg)
            tvStockStatusBadge.setTextColor(ContextCompat.getColor(context, statusTextColor))

            // 4. Price
            val price = instrument.currentPrice
            val formattedPrice = if (price != null) {
                when (instrument.currency) {
                    "IDR" -> String.format(Locale.US, "Rp %,.0f", price)
                    else -> String.format(Locale.US, "$%,.2f", price)
                }
            } else {
                "--"
            }
            tvStockPrice.text = formattedPrice

            // 5. Price Change
            val pctChange = instrument.priceChangePercentage24h
            if (pctChange != null) {
                val isPos = pctChange >= 0
                val sign = if (isPos) "+" else ""
                tvStockChange.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                val colorRes = if (isPos) R.color.accent_teal else R.color.accent_muted_red
                tvStockChange.setTextColor(ContextCompat.getColor(context, colorRes))
            } else {
                tvStockChange.text = "--"
                tvStockChange.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
            }

            // 6. Expanded State
            layoutExpandedPanel.visibility = if (isExpanded) View.VISIBLE else View.GONE
            ivExpandChevron.rotation = if (isExpanded) 180f else 0f

            if (isExpanded) {
                val accentRes = when {
                    instrument.countryCode == "ID" || instrument.currency == "IDR" -> R.color.accent_teal
                    instrument.category == MarketCategory.ETFS -> R.color.accent_violet
                    else -> R.color.accent_cyan
                }
                viewExpandedAccent.setBackgroundColor(ContextCompat.getColor(context, accentRes))

                val catText = if (instrument.category == MarketCategory.ETFS) "EXCHANGE TRADED FUND" else "EQUITY STOCK"
                tvExpandedCategory.text = catText

                tvDetailFullName.text = instrument.name
                tvDetailExchangeCurrency.text = "${instrument.symbol} · ${instrument.exchange} (${instrument.currency})"
                tvDetailLastPrice.text = formattedPrice

                if (pctChange != null) {
                    val sign = if (pctChange >= 0) "+" else ""
                    tvDetailMovement.text = String.format(Locale.US, "%s%.2f%%", sign, pctChange)
                    val colorRes = if (pctChange >= 0) R.color.accent_teal else R.color.accent_muted_red
                    tvDetailMovement.setTextColor(ContextCompat.getColor(context, colorRes))
                } else {
                    tvDetailMovement.text = "N/A"
                    tvDetailMovement.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                }

                if (instrument.dataStatus == MarketDataStatus.LIVE && !instrument.sparklinePrices.isNullOrEmpty()) {
                    tvHistoryStatusText.text = "LIVE TICK HISTORY"
                    tvHistoryStatusText.setTextColor(ContextCompat.getColor(context, R.color.accent_teal))
                    tvHistoryStatusSubtext.text = "Historical tick series active for ${instrument.symbol}."
                } else {
                    tvHistoryStatusText.text = "HISTORICAL DATA UNAVAILABLE"
                    tvHistoryStatusText.setTextColor(ContextCompat.getColor(context, R.color.accent_gold))
                    tvHistoryStatusSubtext.text = "Simulated reference feed does not include exchange tick history."
                }

                tvFeedSource.text = "Provider: ${instrument.exchange} Reference Engine · ${instrument.dataStatus.name}"

                btnViewFullDetails.setOnClickListener {
                    onViewDetails(instrument)
                }
            }

            layoutCollapsedRow.setOnClickListener {
                onToggleExpand(instrument)
            }

            itemView.setOnClickListener {
                onToggleExpand(instrument)
            }
        }
    }
}
