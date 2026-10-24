package com.repp.aquaticremineztools.presentation.news

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.NewsPreferences
import com.repp.aquaticremineztools.core.util.ActivityTransition
import com.repp.aquaticremineztools.core.util.DrawerMenuHelper
import com.repp.aquaticremineztools.data.model.NewsArticle
import com.repp.aquaticremineztools.presentation.about.AboutActivity
import com.repp.aquaticremineztools.presentation.history.HistoryActivity
import com.repp.aquaticremineztools.presentation.home.HomeActivity
import com.repp.aquaticremineztools.presentation.search.SearchActivity
import com.repp.aquaticremineztools.presentation.settings.SettingsActivity

class NewsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var newsViewModel: NewsViewModel
    private lateinit var newsAdapter: NewsAdapter
    private lateinit var categoryAdapter: NewsCategoryAdapter

    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var pbNewsFeed: ProgressBar
    private lateinit var rvNewsFeed: RecyclerView
    private lateinit var rvCategoryTabs: RecyclerView
    private lateinit var btnLoadMoreNews: MaterialButton
    private lateinit var tvNewsOfflineBanner: TextView
    private lateinit var layoutNewsEmpty: LinearLayout
    private lateinit var btnRetryNews: MaterialButton

    private var selectedCategory: String = "All"
    private var currentSearchQuery: String = ""

    // Bottom Nav Pills
    private lateinit var tabHome: LinearLayout
    private lateinit var tabSearch: LinearLayout
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabAbout: LinearLayout
    private lateinit var tabSettings: LinearLayout

    private lateinit var pillHome: View
    private lateinit var pillSearch: View
    private lateinit var pillHistory: View
    private lateinit var pillAbout: View
    private lateinit var pillSettings: View

    private lateinit var iconHome: ImageView
    private lateinit var iconSearch: ImageView
    private lateinit var iconHistory: ImageView
    private lateinit var iconAbout: ImageView
    private lateinit var iconSettings: ImageView

    private lateinit var textHome: TextView
    private lateinit var textSearch: TextView
    private lateinit var textHistory: TextView
    private lateinit var textAbout: TextView
    private lateinit var textSettings: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        newsViewModel = ViewModelProvider(this)[NewsViewModel::class.java]

        bindViews()
        setupDrawerAndHeader()
        setupCategoryTabs()
        setupSearch()
        setupNewsFeed()
        setupBottomNav()
        observeViewModel()

        newsViewModel.loadNewsFeed(this, selectedCategory, currentSearchQuery)
    }

    override fun onResume() {
        super.onResume()
        updateCategoryTabs()
        DrawerMenuHelper.updatePasswordLabel(this)
    }

    private fun bindViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        etSearch = findViewById(R.id.etNewsSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        pbNewsFeed = findViewById(R.id.pbNewsFeed)
        rvNewsFeed = findViewById(R.id.rvNewsFeed)
        rvCategoryTabs = findViewById(R.id.rvCategoryTabs)
        btnLoadMoreNews = findViewById(R.id.btnLoadMoreNews)
        tvNewsOfflineBanner = findViewById(R.id.tvNewsOfflineBanner)
        layoutNewsEmpty = findViewById(R.id.layoutNewsEmpty)
        btnRetryNews = findViewById(R.id.btnRetryNews)

        // Bottom Nav
        tabHome = findViewById(R.id.tabHome)
        tabSearch = findViewById(R.id.tabSearch)
        tabHistory = findViewById(R.id.tabHistory)
        tabAbout = findViewById(R.id.tabAbout)
        tabSettings = findViewById(R.id.tabSettings)

        pillHome = findViewById(R.id.pillHome)
        pillSearch = findViewById(R.id.pillSearch)
        pillHistory = findViewById(R.id.pillHistory)
        pillAbout = findViewById(R.id.pillAbout)
        pillSettings = findViewById(R.id.pillSettings)

        iconHome = findViewById(R.id.iconHome)
        iconSearch = findViewById(R.id.iconSearch)
        iconHistory = findViewById(R.id.iconHistory)
        iconAbout = findViewById(R.id.iconAbout)
        iconSettings = findViewById(R.id.iconSettings)

        textHome = findViewById(R.id.textHome)
        textSearch = findViewById(R.id.textSearch)
        textHistory = findViewById(R.id.textHistory)
        textAbout = findViewById(R.id.textAbout)
        textSettings = findViewById(R.id.textSettings)
    }

    private fun setupDrawerAndHeader() {
        DrawerMenuHelper.setup(this, drawerLayout)

        findViewById<FrameLayout>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<FrameLayout>(R.id.btnFilterNews).setOnClickListener {
            val sheet = NewsFilterBottomSheet.newInstance(selectedCategory, onApply = { category ->
                selectedCategory = category
                updateCategoryTabs()
                newsViewModel.loadNewsFeed(this, selectedCategory, currentSearchQuery, forceRefresh = true)
            }, onReset = {
                selectedCategory = "All"
                updateCategoryTabs()
                newsViewModel.loadNewsFeed(this, "All", currentSearchQuery, forceRefresh = true)
            })
            sheet.show(supportFragmentManager, "NewsFilterBottomSheet")
        }

        findViewById<FrameLayout>(R.id.btnManageCategories).setOnClickListener {
            val intent = Intent(this, ManageNewsCategoriesActivity::class.java)
            ActivityTransition.startActivitySmooth(this, intent)
        }
    }

    private fun setupCategoryTabs() {
        val categories = buildCategoryList()
        categoryAdapter = NewsCategoryAdapter(categories, selectedCategory) { category ->
            selectedCategory = category
            newsViewModel.loadNewsFeed(this, selectedCategory, currentSearchQuery)
        }
        rvCategoryTabs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvCategoryTabs.adapter = categoryAdapter
    }

    private fun updateCategoryTabs() {
        val categories = buildCategoryList()
        categoryAdapter.updateCategories(categories, selectedCategory)
    }

    private fun buildCategoryList(): List<String> {
        val result = mutableListOf("All")
        val favorites = NewsPreferences.getFavoriteCategories(this)
        val allCats = NewsPreferences.DEFAULT_CATEGORIES

        // Favorites first
        val favoriteList = allCats.filter { favorites.contains(it) }
        val remainingList = allCats.filter { !favorites.contains(it) }

        result.addAll(favoriteList)
        result.addAll(remainingList)
        return result
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                if (query != currentSearchQuery) {
                    currentSearchQuery = query
                    newsViewModel.loadNewsFeed(this@NewsActivity, selectedCategory, currentSearchQuery)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            currentSearchQuery = ""
            newsViewModel.loadNewsFeed(this, selectedCategory, currentSearchQuery)
        }
    }

    private fun setupNewsFeed() {
        newsAdapter = NewsAdapter { article ->
            openNewsDetail(article)
        }
        rvNewsFeed.adapter = newsAdapter

        btnLoadMoreNews.setOnClickListener {
            newsViewModel.loadMoreNews()
        }

        btnRetryNews.setOnClickListener {
            newsViewModel.loadNewsFeed(this, selectedCategory, currentSearchQuery, forceRefresh = true)
        }
    }

    private fun observeViewModel() {
        newsViewModel.newsFeedUiState.observe(this) { state ->
            when (state) {
                is NewsUiState.Loading -> {
                    pbNewsFeed.visibility = View.VISIBLE
                    layoutNewsEmpty.visibility = View.GONE
                    tvNewsOfflineBanner.visibility = View.GONE
                }
                is NewsUiState.Success -> {
                    pbNewsFeed.visibility = View.GONE
                    newsAdapter.submitList(state.articles)

                    if (state.articles.isEmpty()) {
                        layoutNewsEmpty.visibility = View.VISIBLE
                        rvNewsFeed.visibility = View.GONE
                        btnLoadMoreNews.visibility = View.GONE
                    } else {
                        layoutNewsEmpty.visibility = View.GONE
                        rvNewsFeed.visibility = View.VISIBLE
                        btnLoadMoreNews.visibility = if (state.hasMore) View.VISIBLE else View.GONE
                    }

                    if (state.isOffline || state.isCached) {
                        tvNewsOfflineBanner.text = getString(R.string.news_offline_notice)
                        tvNewsOfflineBanner.visibility = View.VISIBLE
                    } else {
                        tvNewsOfflineBanner.visibility = View.GONE
                    }
                }
                is NewsUiState.Error -> {
                    pbNewsFeed.visibility = View.GONE
                    rvNewsFeed.visibility = View.GONE
                    btnLoadMoreNews.visibility = View.GONE
                    layoutNewsEmpty.visibility = View.VISIBLE
                    tvNewsOfflineBanner.visibility = View.GONE
                }
            }
        }
    }

    private fun openNewsDetail(article: NewsArticle) {
        val intent = Intent(this, NewsDetailActivity::class.java).apply {
            putExtra("article_url", article.articleUrl)
            putExtra("headline", article.headline)
            putExtra("summary", article.summary)
            putExtra("content", article.content)
            putExtra("thumbnail_url", article.thumbnailUrl)
            putExtra("publisher", article.publisher)
            putExtra("category", article.category)
            putExtra("published_epoch", article.publishedAtEpoch)
        }
        ActivityTransition.startActivitySmooth(this, intent)
    }

    private fun setupBottomNav() {
        tabHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }

        tabSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            ActivityTransition.startActivitySmooth(this, intent)
        }
    }
}
