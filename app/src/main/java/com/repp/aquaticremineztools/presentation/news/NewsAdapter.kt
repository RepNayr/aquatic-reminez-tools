package com.repp.aquaticremineztools.presentation.news

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.model.NewsArticle
import java.util.Locale

class NewsAdapter(
    private val onArticleClick: (NewsArticle) -> Unit
) : ListAdapter<NewsArticle, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val TYPE_FEATURED = 0
        private const val TYPE_COMPACT = 1

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<NewsArticle>() {
            override fun areItemsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
                return oldItem.articleUrl == newItem.articleUrl
            }

            override fun areContentsTheSame(oldItem: NewsArticle, newItem: NewsArticle): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val article = getItem(position)
        return if (position == 0 && article.isFeatured) TYPE_FEATURED else TYPE_COMPACT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_FEATURED) {
            val view = inflater.inflate(R.layout.item_news_featured, parent, false)
            FeaturedViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_news_compact, parent, false)
            CompactViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val article = getItem(position)
        if (holder is FeaturedViewHolder) {
            holder.bind(article, onArticleClick)
        } else if (holder is CompactViewHolder) {
            holder.bind(article, onArticleClick)
        }
    }

    class FeaturedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumbnail: ImageView = view.findViewById(R.id.ivFeaturedThumbnail)
        private val tvCategory: TextView = view.findViewById(R.id.tvFeaturedCategory)
        private val tvHeadline: TextView = view.findViewById(R.id.tvFeaturedHeadline)
        private val tvMeta: TextView = view.findViewById(R.id.tvFeaturedMeta)
        private val tvSummary: TextView = view.findViewById(R.id.tvFeaturedSummary)

        fun bind(article: NewsArticle, onClick: (NewsArticle) -> Unit) {
            tvCategory.text = article.category.uppercase(Locale.ROOT)
            tvHeadline.text = article.headline
            tvSummary.text = article.summary

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                article.publishedAtEpoch,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            tvMeta.text = "${article.publisher} · $relativeTime"

            if (!article.thumbnailUrl.isNullOrBlank()) {
                ivThumbnail.visibility = View.VISIBLE
                ivThumbnail.load(article.thumbnailUrl) {
                    crossfade(true)
                }
            } else {
                ivThumbnail.setImageDrawable(null)
            }

            itemView.setOnClickListener { onClick(article) }
        }
    }

    class CompactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivThumbnail: ImageView = view.findViewById(R.id.ivCompactThumbnail)
        private val tvCategory: TextView = view.findViewById(R.id.tvCompactCategory)
        private val tvMeta: TextView = view.findViewById(R.id.tvCompactMeta)
        private val tvHeadline: TextView = view.findViewById(R.id.tvCompactHeadline)
        private val tvSummary: TextView = view.findViewById(R.id.tvCompactSummary)

        fun bind(article: NewsArticle, onClick: (NewsArticle) -> Unit) {
            tvCategory.text = article.category.uppercase(Locale.ROOT)
            tvHeadline.text = article.headline
            tvSummary.text = article.summary

            val relativeTime = DateUtils.getRelativeTimeSpanString(
                article.publishedAtEpoch,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            tvMeta.text = "${article.publisher} · $relativeTime"

            if (!article.thumbnailUrl.isNullOrBlank()) {
                ivThumbnail.visibility = View.VISIBLE
                ivThumbnail.load(article.thumbnailUrl) {
                    crossfade(true)
                }
            } else {
                ivThumbnail.setImageDrawable(null)
            }

            itemView.setOnClickListener { onClick(article) }
        }
    }
}
