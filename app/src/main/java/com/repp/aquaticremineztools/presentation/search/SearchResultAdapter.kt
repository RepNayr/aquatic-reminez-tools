package com.repp.aquaticremineztools.presentation.search

import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolData
import com.repp.aquaticremineztools.data.ToolIconRegistry
import java.util.Locale

class SearchResultAdapter(
    private var results: List<ToolData.Tool>,
    private val onToolClick: (ToolData.Tool) -> Unit,
) : RecyclerView.Adapter<SearchResultAdapter.ResultViewHolder>() {

    private var currentQuery: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val tool = results[position]
        val context = holder.itemView.context

        holder.ivIcon.setImageResource(ToolIconRegistry.getIconRes(tool))
        holder.tvCategory.text = tool.category.displayName

        if (currentQuery.isNotEmpty()) {
            holder.tvName.text = getHighlightedText(tool.name, currentQuery, context)
            holder.tvDesc.text = getHighlightedText(tool.description, currentQuery, context)
        } else {
            holder.tvName.text = tool.name
            holder.tvDesc.text = tool.description
        }

        holder.cardContent.setOnClickListener {
            onToolClick(tool)
        }
    }

    private fun getHighlightedText(text: String, query: String, context: android.content.Context): SpannableString {
        val spannable = SpannableString(text)
        val lowerText = text.lowercase(Locale.getDefault())
        val lowerQuery = query.lowercase(Locale.getDefault())
        var startIndex = lowerText.indexOf(lowerQuery)

        val highlightColor = ContextCompat.getColor(context, R.color.accent_cyan)

        while (startIndex >= 0) {
            val endIndex = startIndex + query.length
            spannable.setSpan(
                ForegroundColorSpan(highlightColor),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            startIndex = lowerText.indexOf(lowerQuery, endIndex)
        }
        return spannable
    }

    override fun getItemCount(): Int = results.size

    fun updateResults(newResults: List<ToolData.Tool>, query: String = "") {
        results = newResults
        currentQuery = query
        notifyDataSetChanged()
    }

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardContent: View = itemView.findViewById(R.id.cardContent)
        val ivIcon: ImageView = itemView.findViewById(R.id.ivToolIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvToolName)
        val tvCategory: TextView = itemView.findViewById(R.id.tvToolCategory)
        val tvDesc: TextView = itemView.findViewById(R.id.tvToolDesc)
    }
}