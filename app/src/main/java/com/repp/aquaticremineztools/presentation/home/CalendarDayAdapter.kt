package com.repp.aquaticremineztools.presentation.home

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.CalendarDay

class CalendarDayAdapter(
    private var days: List<CalendarDay> = emptyList(),
    private val onDaySelected: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder>() {

    fun submitList(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val containerDay: FrameLayout = itemView.findViewById(R.id.containerDay)
        private val tvGregorianDay: TextView = itemView.findViewById(R.id.tvGregorianDay)
        private val tvHijriDay: TextView = itemView.findViewById(R.id.tvHijriDay)

        fun bind(day: CalendarDay) {
            if (!day.isInCurrentMonth) {
                // Blank cell outside active month
                containerDay.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_calendar_day_normal)
                containerDay.isClickable = false
                containerDay.isFocusable = false
                tvGregorianDay.visibility = View.INVISIBLE
                tvHijriDay.visibility = View.INVISIBLE
                itemView.setOnClickListener(null)
                return
            }

            // Valid day in current month
            tvGregorianDay.visibility = View.VISIBLE
            tvHijriDay.visibility = View.VISIBLE
            containerDay.isClickable = true
            containerDay.isFocusable = true

            tvGregorianDay.text = day.dayOfMonth.toString()
            tvHijriDay.text = day.hijriDay

            // Background drawable based on states
            val bgRes = when {
                day.isToday && day.isSelected -> R.drawable.bg_calendar_day_today_selected
                day.isToday -> R.drawable.bg_calendar_day_today
                day.isSelected -> R.drawable.bg_calendar_day_selected
                else -> R.drawable.bg_calendar_day_normal
            }
            containerDay.background = ContextCompat.getDrawable(itemView.context, bgRes)

            // Text colors
            if (day.isToday) {
                // Today uses solid cyan fill, so text must be dark #090B0F
                tvGregorianDay.setTextColor(Color.parseColor("#090B0F"))
                tvHijriDay.setTextColor(Color.parseColor("#1B232A"))
            } else {
                if (day.isSunday) {
                    tvGregorianDay.setTextColor(Color.parseColor("#FF6B6B"))
                } else {
                    tvGregorianDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                }
                tvHijriDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
            }

            itemView.setOnClickListener {
                onDaySelected(day)
            }
        }
    }
}
