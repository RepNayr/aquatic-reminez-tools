package com.repp.aquaticremineztools.presentation.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.repp.aquaticremineztools.R

class NewsCategoryAdapter(
    private var categories: List<String>,
    private var selectedCategory: String,
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<NewsCategoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardCategoryChip: MaterialCardView = view.findViewById(R.id.cardCategoryChip)
        val tvCategoryName: TextView = view.findViewById(R.id.tvCategoryName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news_category_chip, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.tvCategoryName.text = category

        val isSelected = category.equals(selectedCategory, ignoreCase = true)
        val context = holder.itemView.context

        if (isSelected) {
            holder.cardCategoryChip.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent_primary_transparent))
            holder.cardCategoryChip.strokeColor = ContextCompat.getColor(context, R.color.accent_cyan)
            holder.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.accent_cyan))
        } else {
            holder.cardCategoryChip.setCardBackgroundColor(ContextCompat.getColor(context, R.color.bg_card))
            holder.cardCategoryChip.strokeColor = ContextCompat.getColor(context, R.color.border)
            holder.tvCategoryName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.itemView.setOnClickListener {
            if (!isSelected) {
                selectedCategory = category
                notifyDataSetChanged()
                onCategorySelected(category)
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    fun updateCategories(newCategories: List<String>, newSelected: String) {
        this.categories = newCategories
        this.selectedCategory = newSelected
        notifyDataSetChanged()
    }
}
