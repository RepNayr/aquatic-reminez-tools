package com.repp.aquaticremineztools.presentation.news

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.settings.NewsPreferences

class ManageNewsCategoriesActivity : AppCompatActivity() {

    private val favoriteSet = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_news_categories)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        favoriteSet.addAll(NewsPreferences.getFavoriteCategories(this))

        setupCategoryList()

        findViewById<MaterialButton>(R.id.btnSaveCategories).setOnClickListener {
            NewsPreferences.setFavoriteCategories(this, favoriteSet)
            Toast.makeText(this, "Category preferences saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupCategoryList() {
        val container = findViewById<LinearLayout>(R.id.layoutCategoryList)
        container.removeAllViews()

        val categories = NewsPreferences.DEFAULT_CATEGORIES

        categories.forEach { category ->
            val cardView = LayoutInflater.from(this).inflate(
                R.layout.item_manage_category_row,
                container,
                false
            )

            val tvName = cardView.findViewById<TextView>(R.id.tvCategoryRowName)
            val switchFav = cardView.findViewById<MaterialSwitch>(R.id.switchCategoryFavorite)

            tvName.text = category
            switchFav.isChecked = favoriteSet.contains(category)

            switchFav.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    favoriteSet.add(category)
                } else {
                    favoriteSet.remove(category)
                }
            }

            container.addView(cardView)
        }
    }
}
