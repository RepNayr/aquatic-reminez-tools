package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.MarketDataStatus
import com.repp.aquaticremineztools.data.model.MarketInstrument
import java.util.Locale

sealed class MacroAdapterItem {
    data class CountryHeader(
        val countryCode: String,
        val countryName: String,
        val subtitle: String,
        val flagRes: Int,
        val count: Int
    ) : MacroAdapterItem()

    data class IndicatorCard(
        val instrument: MarketInstrument,
        val isFeatured: Boolean,
        val isExpanded: Boolean
    ) : MacroAdapterItem()

    data class NoticeCard(
        val title: String,
        val message: String
    ) : MacroAdapterItem()
}

class EconomicIndicatorAdapter(
    private var rawInstruments: List<MarketInstrument> = emptyList(),
    private val onItemClick: (MarketInstrument) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CARD = 1
        private const val VIEW_TYPE_NOTICE = 2
    }

    private var adapterItems: List<MacroAdapterItem> = emptyList()
    private var expandedIdId: String? = null
    private var expandedUsId: String? = null

    init {
        rebuildAdapterItems()
    }

    fun updateData(newItems: List<MarketInstrument>) {
        rawInstruments = newItems
        rebuildAdapterItems()
        notifyDataSetChanged()
    }

    fun getItemSpanSize(position: Int): Int {
        if (position !in adapterItems.indices) return 1
        return when (adapterItems[position]) {
            is MacroAdapterItem.CountryHeader -> 2
            is MacroAdapterItem.NoticeCard -> 2
            is MacroAdapterItem.IndicatorCard -> 2
        }
    }

    private fun rebuildAdapterItems() {
        val itemsList = mutableListOf<MacroAdapterItem>()

        val indonesiaList = rawInstruments.filter {
            it.countryCode == "ID" || it.exchange == "BI" || it.exchange == "BPS" || it.id.startsWith("id_") || it.id == "bi_rate"
        }

        val usList = rawInstruments.filter {
            it.countryCode == "US" || it.exchange == "FED" || it.exchange == "BOND" || it.id.startsWith("us_") || it.id == "fed_rate"
        }

        val otherList = rawInstruments.filter { inst ->
            inst !in indonesiaList && inst !in usList
        }

        // 1. INDONESIA GROUP
        if (indonesiaList.isNotEmpty()) {
            itemsList.add(
                MacroAdapterItem.CountryHeader(
                    countryCode = "ID",
                    countryName = "INDONESIA · MACROECONOMIC",
                    subtitle = "Central Bank Policy Rate & Economic Growth",
                    flagRes = R.drawable.ic_asset_forex_usdidr,
                    count = indonesiaList.size
                )
            )

            indonesiaList.forEach { inst ->
                val isFeatured = inst.id == "bi_rate" || inst.symbol == "BI7DRR"
                val isExpanded = (expandedIdId == inst.id)
                itemsList.add(MacroAdapterItem.IndicatorCard(inst, isFeatured, isExpanded))
            }
        }

        // 2. UNITED STATES GROUP
        if (usList.isNotEmpty()) {
            itemsList.add(
                MacroAdapterItem.CountryHeader(
                    countryCode = "US",
                    countryName = "UNITED STATES · MACROECONOMIC",
                    subtitle = "Federal Reserve Target Rate & Bond Yields",
                    flagRes = R.drawable.ic_asset_forex_eurusd,
                    count = usList.size
                )
            )

            usList.forEach { inst ->
                val isFeatured = inst.id == "fed_rate" || inst.symbol == "FEDFUNDS"
                val isExpanded = (expandedUsId == inst.id)
                itemsList.add(MacroAdapterItem.IndicatorCard(inst, isFeatured, isExpanded))
            }

            itemsList.add(
                MacroAdapterItem.NoticeCard(
                    title = "US COVERAGE NOTICE",
                    message = "US CPI, US GDP & Unemployment data pending verified provider API integration."
                )
            )
        }

        // 3. OTHER COUNTRIES / GENERAL (if any)
        if (otherList.isNotEmpty()) {
            itemsList.add(
                MacroAdapterItem.CountryHeader(
                    countryCode = "GLOBAL",
                    countryName = "GLOBAL MACROECONOMIC INDICATORS",
                    subtitle = "International Policy & Economic Data",
                    flagRes = R.drawable.ic_asset_fallback_eco,
                    count = otherList.size
                )
            )

            otherList.forEach { inst ->
                val isExpanded = (expandedUsId == inst.id || expandedIdId == inst.id)
                itemsList.add(MacroAdapterItem.IndicatorCard(inst, isFeatured = false, isExpanded = isExpanded))
            }
        }

        adapterItems = itemsList
    }

    override fun getItemViewType(position: Int): Int {
        return when (adapterItems[position]) {
            is MacroAdapterItem.CountryHeader -> VIEW_TYPE_HEADER
            is MacroAdapterItem.IndicatorCard -> VIEW_TYPE_CARD
            is MacroAdapterItem.NoticeCard -> VIEW_TYPE_NOTICE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_macro_country_header, parent, false))
            VIEW_TYPE_CARD -> CardViewHolder(inflater.inflate(R.layout.item_economic_indicator_card, parent, false))
            else -> NoticeViewHolder(inflater.inflate(R.layout.item_macro_notice_card, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = adapterItems[position]) {
            is MacroAdapterItem.CountryHeader -> (holder as HeaderViewHolder).bind(item)
            is MacroAdapterItem.NoticeCard -> (holder as NoticeViewHolder).bind(item)
            is MacroAdapterItem.IndicatorCard -> (holder as CardViewHolder).bind(item) { instrument ->
                onIndicatorTapped(instrument)
                onItemClick(instrument)
            }
        }
    }

    override fun getItemCount(): Int = adapterItems.size

    private fun onIndicatorTapped(instrument: MarketInstrument) {
        val isIndonesia = instrument.countryCode == "ID" || instrument.exchange == "BI" || instrument.exchange == "BPS" || instrument.id.startsWith("id_") || instrument.id == "bi_rate"

        if (isIndonesia) {
            expandedIdId = if (expandedIdId == instrument.id) null else instrument.id
        } else {
            expandedUsId = if (expandedUsId == instrument.id) null else instrument.id
        }

        rebuildAdapterItems()
        notifyDataSetChanged()
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMacroCountryFlag: ImageView = itemView.findViewById(R.id.ivMacroCountryFlag)
        private val tvMacroCountryTitle: TextView = itemView.findViewById(R.id.tvMacroCountryTitle)
        private val tvMacroCountrySubtitle: TextView = itemView.findViewById(R.id.tvMacroCountrySubtitle)
        private val tvMacroCountryCount: TextView = itemView.findViewById(R.id.tvMacroCountryCount)

        fun bind(item: MacroAdapterItem.CountryHeader) {
            ivMacroCountryFlag.setImageResource(item.flagRes)
            tvMacroCountryTitle.text = item.countryName
            tvMacroCountrySubtitle.text = item.subtitle
            tvMacroCountryCount.text = "${item.count} INDICATORS"
        }
    }

    class NoticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNoticeMessage: TextView = itemView.findViewById(R.id.tvNoticeMessage)

        fun bind(item: MacroAdapterItem.NoticeCard) {
            tvNoticeMessage.text = item.message
        }
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivEconIcon: ImageView = itemView.findViewById(R.id.ivEconIcon)
        private val tvEconSymbol: TextView = itemView.findViewById(R.id.tvEconSymbol)
        private val tvEconCountry: TextView = itemView.findViewById(R.id.tvEconCountry)
        private val tvEconStatusBadge: TextView = itemView.findViewById(R.id.tvEconStatusBadge)
        private val ivExpandChevron: ImageView = itemView.findViewById(R.id.ivExpandChevron)
        private val tvEconName: TextView = itemView.findViewById(R.id.tvEconName)
        private val tvEconRate: TextView = itemView.findViewById(R.id.tvEconRate)
        private val tvEconPeriod: TextView = itemView.findViewById(R.id.tvEconPeriod)

        private val layoutExpandedPanel: LinearLayout = itemView.findViewById(R.id.layoutExpandedPanel)
        private val tvDetailCategory: TextView = itemView.findViewById(R.id.tvDetailCategory)
        private val tvDetailAuthority: TextView = itemView.findViewById(R.id.tvDetailAuthority)
        private val tvDetailPrevRate: TextView = itemView.findViewById(R.id.tvDetailPrevRate)
        private val tvDetailDelta: TextView = itemView.findViewById(R.id.tvDetailDelta)
        private val tvDetailIntelligence: TextView = itemView.findViewById(R.id.tvDetailIntelligence)

        fun bind(item: MacroAdapterItem.IndicatorCard, onClick: (MarketInstrument) -> Unit) {
            val instrument = item.instrument
            tvEconSymbol.text = instrument.symbol
            tvEconCountry.text = instrument.countryCode ?: instrument.exchange
            tvEconName.text = instrument.name

            val iconRes = when (instrument.id) {
                "bi_rate", "fed_rate" -> R.drawable.ic_asset_eco_rate
                "id_inflation", "us_inflation" -> R.drawable.ic_asset_eco_inflation
                "id_gdp", "us_gdp" -> R.drawable.ic_asset_eco_gdp
                "us_10y" -> R.drawable.ic_asset_eco_yield
                else -> R.drawable.ic_asset_fallback_eco
            }
            ivEconIcon.setImageResource(iconRes)

            val price = instrument.currentPrice
            if (price != null) {
                tvEconRate.text = String.format(Locale.US, "%.2f%%", price)
            } else {
                tvEconRate.text = "--"
            }

            tvEconPeriod.text = instrument.macroPeriod ?: "Indicator"

            val status = instrument.dataStatus
            when (status) {
                MarketDataStatus.LIVE -> {
                    tvEconStatusBadge.text = "LIVE"
                    tvEconStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_live)
                    tvEconStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_online))
                }
                MarketDataStatus.CACHED -> {
                    tvEconStatusBadge.text = "CACHED"
                    tvEconStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_cached)
                    tvEconStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_blue))
                }
                MarketDataStatus.SIMULATED -> {
                    tvEconStatusBadge.text = "SIMULATED"
                    tvEconStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_simulated)
                    tvEconStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_gold))
                }
                MarketDataStatus.UNAVAILABLE -> {
                    tvEconStatusBadge.text = "UNAVAILABLE"
                    tvEconStatusBadge.setBackgroundResource(R.drawable.bg_terminal_badge_unavailable)
                    tvEconStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_muted))
                }
            }

            // Expand state UI
            if (item.isExpanded) {
                layoutExpandedPanel.visibility = View.VISIBLE
                ivExpandChevron.rotation = 180f

                // Fill details based on instrument
                when (instrument.id) {
                    "bi_rate" -> {
                        tvDetailCategory.text = "Central Bank Policy Rate"
                        tvDetailAuthority.text = "Bank Indonesia"
                        tvDetailPrevRate.text = "6.25%"
                        tvDetailDelta.text = "-0.25% (Rate Cut)"
                        tvDetailDelta.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_teal))
                        tvDetailIntelligence.text = "Provider: Bank Indonesia · 7-Day Reverse Repo Target Rate · Published Monthly"
                    }
                    "fed_rate" -> {
                        tvDetailCategory.text = "Central Bank Target Rate"
                        tvDetailAuthority.text = "Federal Reserve"
                        tvDetailPrevRate.text = "5.00%"
                        tvDetailDelta.text = "-0.25% (Rate Cut)"
                        tvDetailDelta.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_teal))
                        tvDetailIntelligence.text = "Provider: Federal Reserve · Federal Funds Target Rate Range · FOMC Policy"
                    }
                    "id_inflation" -> {
                        tvDetailCategory.text = "Consumer Price Index"
                        tvDetailAuthority.text = "BPS Indonesia"
                        tvDetailPrevRate.text = "2.61%"
                        tvDetailDelta.text = "-0.04% (Disinflation)"
                        tvDetailDelta.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_cyan))
                        tvDetailIntelligence.text = "Provider: BPS Indonesia · Headline Inflation Rate YoY · Monthly Index"
                    }
                    "us_10y" -> {
                        tvDetailCategory.text = "Sovereign Debt Yield"
                        tvDetailAuthority.text = "U.S. Treasury"
                        tvDetailPrevRate.text = "4.08%"
                        tvDetailDelta.text = "+0.16% (Yield Rise)"
                        tvDetailDelta.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_gold))
                        tvDetailIntelligence.text = "Provider: U.S. Department of the Treasury · 10-Year Benchmark Yield"
                    }
                    "id_gdp" -> {
                        tvDetailCategory.text = "Gross Domestic Product"
                        tvDetailAuthority.text = "BPS Indonesia"
                        tvDetailPrevRate.text = "5.05%"
                        tvDetailDelta.text = "Unchanged (Stable)"
                        tvDetailDelta.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_teal))
                        tvDetailIntelligence.text = "Provider: BPS Indonesia · Real GDP Growth YoY · Quarterly Report"
                    }
                    else -> {
                        tvDetailCategory.text = "Economic Metric"
                        tvDetailAuthority.text = instrument.exchange
                        tvDetailPrevRate.text = "N/A"
                        tvDetailDelta.text = "N/A"
                        tvDetailIntelligence.text = "Provider: ${instrument.exchange} · Market Instrument Feed"
                    }
                }

            } else {
                layoutExpandedPanel.visibility = View.GONE
                ivExpandChevron.rotation = 0f
            }

            itemView.setOnClickListener { onClick(instrument) }
        }
    }
}
