package com.repp.aquaticremineztools.presentation.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolData

data class SearchCategoryItem(
    val label: String,
    val iconRes: Int,
    val categories: Set<ToolData.Category>?
)

class SearchCategoryAdapter(
    private val items: List<SearchCategoryItem>,
    private val onCategorySelected: (Set<ToolData.Category>?) -> Unit
) : RecyclerView.Adapter<SearchCategoryAdapter.CategoryViewHolder>() {

    private var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_category_pill, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val isSelected = position == selectedPosition

        holder.tvCatName.text = item.label
        holder.ivCatIcon.setImageResource(item.iconRes)

        val density = context.resources.displayMetrics.density

        if (isSelected) {
            holder.cardCategoryPill.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent_primary_transparent))
            holder.cardCategoryPill.strokeColor = ContextCompat.getColor(context, R.color.accent_primary)
            holder.cardCategoryPill.strokeWidth = (2f * density).toInt()
            holder.tvCatName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            holder.ivCatIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_primary))
        } else {
            holder.cardCategoryPill.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_card))
            holder.cardCategoryPill.strokeColor = ContextCompat.getColor(context, R.color.border)
            holder.cardCategoryPill.strokeWidth = (1f * density).toInt()
            holder.tvCatName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            holder.ivCatIcon.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onCategorySelected(item.categories)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardCategoryPill: MaterialCardView = itemView.findViewById(R.id.cardCategoryPill)
        val ivCatIcon: ImageView = itemView.findViewById(R.id.ivCatIcon)
        val tvCatName: TextView = itemView.findViewById(R.id.tvCatName)
    }
}