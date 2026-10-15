package com.repp.aquaticremineztools.presentation.home

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.QuickCategory
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class QuickCategoryAdapter(
    private val categories: List<QuickCategory>,
    private val blurTarget: BlurTarget,
    private val frameClearDrawable: Drawable? = null,
    private val onCategoryClick: (QuickCategory) -> Unit,
) : RecyclerView.Adapter<QuickCategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(
        itemView: View,
        blurTarget: BlurTarget,
        frameClearDrawable: Drawable?
    ) : RecyclerView.ViewHolder(itemView) {
        val cardRoot: MaterialCardView = itemView.findViewById(R.id.cardCategory)
        val blurCategory: BlurView = itemView.findViewById(R.id.blurCategory)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvCount: TextView = itemView.findViewById(R.id.tvCount)

        init {
            GlassBlurHelper.setupBlur(blurCategory, blurTarget, frameClearDrawable)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_category, parent, false)
        return CategoryViewHolder(view, blurTarget, frameClearDrawable)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val context = holder.itemView.context

        holder.ivIcon.setImageResource(category.iconRes)
        holder.tvName.setText(category.nameRes)
        holder.tvCount.text = "${category.count} tools"

        // Accent color
        val accentColor = ContextCompat.getColor(context, category.accentColor)

        // Tint icon with category accent color
        holder.ivIcon.setColorFilter(accentColor)

        holder.cardRoot.setOnClickListener {
            onCategoryClick(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}
