package com.repp.aquaticremineztools.presentation.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolData

class SearchResultAdapter(
    private var results: List<ToolData.Tool>,
    private val onToolClick: (ToolData.Tool) -> Unit,
) : RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val tool = results[position]

        holder.tvEmoji.text = tool.emoji
        holder.tvName.text = tool.name
        holder.tvCategory.text = tool.category.displayName
        holder.tvDesc.text = tool.description

        holder.itemView.setOnClickListener {
            onToolClick(tool)
        }
    }

    override fun getItemCount(): Int = results.size

    fun updateResults(newResults: List<ToolData.Tool>) {
        results = newResults
        notifyDataSetChanged()
    }

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmoji: TextView = itemView.findViewById(R.id.tvToolEmoji)
        val tvName: TextView = itemView.findViewById(R.id.tvToolName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvToolCategory)
        val tvDesc: TextView = itemView.findViewById(R.id.tvToolDesc)
    }
}