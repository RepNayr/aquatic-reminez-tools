package com.repp.aquaticremineztools.presentation.market

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.EconomicEvent

class EconomicEventAdapter(
    private var items: List<EconomicEvent> = emptyList(),
    private val onItemClick: ((EconomicEvent) -> Unit)? = null
) : RecyclerView.Adapter<EconomicEventAdapter.ViewHolder>() {

    fun updateData(newItems: List<EconomicEvent>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_economic_event_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvEventFlag: TextView = itemView.findViewById(R.id.tvEventFlag)
        private val tvEventTitle: TextView = itemView.findViewById(R.id.tvEventTitle)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventActual: TextView = itemView.findViewById(R.id.tvEventActual)
        private val tvEventForecast: TextView = itemView.findViewById(R.id.tvEventForecast)

        fun bind(event: EconomicEvent, onClick: ((EconomicEvent) -> Unit)?) {
            tvEventFlag.text = event.countryFlag
            tvEventTitle.text = event.title
            tvEventDate.text = "${event.eventDate} · ${event.country}"
            tvEventActual.text = event.actualValue ?: "--"
            tvEventForecast.text = if (!event.forecastValue.isNullOrEmpty()) "Exp: ${event.forecastValue}" else ""

            itemView.setOnClickListener { onClick?.invoke(event) }
        }
    }
}
