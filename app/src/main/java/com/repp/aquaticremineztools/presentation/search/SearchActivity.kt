package com.repp.aquaticremineztools.presentation.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.data.ToolData

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnBack: FrameLayout
    private lateinit var btnClear: FrameLayout
    private lateinit var rvResults: RecyclerView
    private lateinit var emptyState: LinearLayout

    private lateinit var adapter: SearchResultAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        bindViews()
        setupRecyclerView()
        setupListeners()

        // Focus search input on open
        etSearch.requestFocus()

        // Initial: tampilkan semua tools
        updateResults("")
    }

    private fun bindViews() {
        etSearch = findViewById(R.id.etSearch)
        btnBack = findViewById(R.id.btnBackSearch)
        btnClear = findViewById(R.id.btnClearSearch)
        rvResults = findViewById(R.id.rvSearchResults)
        emptyState = findViewById(R.id.emptyState)
    }

    private fun setupRecyclerView() {
        adapter = SearchResultAdapter(
            results = emptyList(),
        ) { tool ->
            Toast.makeText(this, "Tool: ${tool.name}\nComing soon!", Toast.LENGTH_SHORT).show()
        }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnClear.setOnClickListener {
            etSearch.setText("")
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                updateResults(query)

                // Show/hide clear button
                btnClear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
            }
        })
    }

    private fun updateResults(query: String) {
        val results = ToolData.searchTools(query)
        adapter.updateResults(results)

        if (results.isEmpty()) {
            rvResults.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvResults.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }
}