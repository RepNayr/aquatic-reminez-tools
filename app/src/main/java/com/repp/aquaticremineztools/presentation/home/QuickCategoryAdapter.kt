package com.repp.aquaticremineztools.presentation.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.QuickCategory

class QuickCategoryAdapter(
    private val categories: List<QuickCategory>,
    private val onCategoryClick: (QuickCategory) -> Unit,
) : RecyclerView.Adapter<QuickCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCount: TextView = itemView.findViewById(R.id.tvCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val context = holder.itemView.context

        holder.tvEmoji.text = category.emoji
        holder.tvName.setText(category.nameRes)
        holder.tvCount.text = "${category.count} tools"

        // Set stroke color sesuai kategori (warna aksen)
        val accentColor = ContextCompat.getColor(context, category.accentColor)
        holder.cardRoot.strokeColor = accentColor

        holder.cardRoot.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}