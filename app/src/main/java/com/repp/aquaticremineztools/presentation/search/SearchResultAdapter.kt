package com.repp.aquaticremineztools.presentation.search

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolData
import com.repp.aquaticremineztools.data.ToolIconRegistry
import com.repp.aquaticremineztools.presentation.home.GlassBlurHelper
import eightbitlab.com.blurview.BlurTarget
import eightbitlab.com.blurview.BlurView

class SearchResultAdapter(
    private val blurTarget: BlurTarget,
    private val frameClearDrawable: Drawable?,
    private var results: List<ToolData.Tool>,
    private val onToolClick: (ToolData.Tool) -> Unit,
) : RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ResultViewHolder(view, blurTarget, frameClearDrawable)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val tool = results[position]

        holder.ivIcon.setImageResource(ToolIconRegistry.getIconRes(tool))
        holder.tvName.text = tool.name
        holder.tvCategory.text = tool.category.displayName
        holder.tvDesc.text = tool.description

        holder.cardContent.setOnClickListener {
            onToolClick(tool)
        }
    }

    override fun getItemCount(): Int = results.size

    fun updateResults(newResults: List<ToolData.Tool>) {
        results = newResults
        notifyDataSetChanged()
    }

    class ResultViewHolder(
        itemView: View,
        blurTarget: BlurTarget,
        frameClearDrawable: Drawable?
    ) : RecyclerView.ViewHolder(itemView) {
        val blurCard: BlurView = itemView.findViewById(R.id.blurCard)
        val cardContent: View = itemView.findViewById(R.id.cardContent)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivToolIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvToolName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvToolCategory)
        val tvDesc: TextView = itemView.findViewById(R.id.tvToolDesc)

        init {
            GlassBlurHelper.setupBlur(blurCard, blurTarget, frameClearDrawable)
        }
    }
}
