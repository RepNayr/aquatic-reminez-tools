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
        private val tvJavanesePasaran: TextView = itemView.findViewById(R.id.tvJavanesePasaran)
        private val viewUnderline: View = itemView.findViewById(R.id.viewUnderline)

        fun bind(day: CalendarDay) {
            tvGregorianDay.visibility = View.VISIBLE
            tvHijriDay.visibility = View.VISIBLE
            tvJavanesePasaran.visibility = View.VISIBLE
            containerDay.isClickable = true
            containerDay.isFocusable = true

            tvGregorianDay.text = day.dayOfMonth.toString()
            tvHijriDay.text = day.hijriDay
            tvJavanesePasaran.text = day.javanesePasaran

            val hasHoliday = !day.observance.isNullOrBlank()

            if (!day.isInCurrentMonth) {
                // Neighbor month days rendered dimmed
                containerDay.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_calendar_day_normal)
                tvGregorianDay.setTextColor(Color.parseColor("#55FFFFFF"))
                tvHijriDay.setTextColor(Color.parseColor("#33FFFFFF"))
                tvJavanesePasaran.setTextColor(Color.parseColor("#33FFFFFF"))
                viewUnderline.visibility = View.GONE
            } else {
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
                    tvGregorianDay.setTextColor(Color.parseColor("#090B0F"))
                    tvHijriDay.setTextColor(Color.parseColor("#1B232A"))
                    tvJavanesePasaran.setTextColor(Color.parseColor("#1B232A"))
                } else {
                    when {
                        hasHoliday || day.isSunday -> {
                            tvGregorianDay.setTextColor(Color.parseColor("#FF6B6B"))
                        }
                        day.isFriday -> {
                            tvGregorianDay.setTextColor(Color.parseColor("#4ECCA3"))
                        }
                        else -> {
                            tvGregorianDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                        }
                    }
                    val secondaryColor = ContextCompat.getColor(itemView.context, R.color.text_secondary)
                    tvHijriDay.setTextColor(secondaryColor)
                    tvJavanesePasaran.setTextColor(secondaryColor)
                }

                if (day.isSelected || day.isToday) {
                    viewUnderline.visibility = View.VISIBLE
                } else {
                    viewUnderline.visibility = View.GONE
                }
            }

            itemView.setOnClickListener {
                onDaySelected(day)
            }
        }
    }
}
