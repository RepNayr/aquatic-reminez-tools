package com.repp.aquaticremineztools.presentation.market

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.data.repository.MarketRepository
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class PortfolioActivity : AppCompatActivity() {

    private lateinit var tvPortfolioTotalValue: TextView
    private lateinit var tvPortfolioProfitLoss: TextView
    private lateinit var rvHoldings: RecyclerView
    private lateinit var btnBack: ImageView
    private lateinit var btnAddHolding: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_portfolio)

        bindViews()
        setupListeners()
        loadPortfolio()
    }

    private fun bindViews() {
        tvPortfolioTotalValue = findViewById(R.id.tvPortfolioTotalValue)
        tvPortfolioProfitLoss = findViewById(R.id.tvPortfolioProfitLoss)
        rvHoldings = findViewById(R.id.rvHoldings)
        btnBack = findViewById(R.id.btnBack)
        btnAddHolding = findViewById(R.id.btnAddHolding)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAddHolding.setOnClickListener {
            finish() // Return to market to pick instrument and add holding
        }
    }

    private fun loadPortfolio() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@PortfolioActivity)
            val holdings = repository.getPortfolioHoldings()
            val marketResult = repository.getAllMarketInstruments(forceRefresh = false)
            val instrumentsMap = marketResult.getOrNull()?.associateBy { it.id } ?: emptyMap()

            var totalValuation = 0.0
            var totalCost = 0.0

            val instrumentsList = holdings.mapNotNull { holding ->
                val inst = instrumentsMap[holding.instrumentId]
                val currentPrice = inst?.currentPrice ?: holding.averageCost
                val value = holding.quantity * currentPrice
                val cost = holding.quantity * holding.averageCost
                totalValuation += value
                totalCost += cost

                inst?.copy(
                    currentPrice = value,
                    priceChangePercentage24h = if (cost > 0.0) ((value - cost) / cost) * 100.0 else 0.0,
                )
            }

            withContext(Dispatchers.Main) {
                tvPortfolioTotalValue.text = String.format(Locale.US, "$%,.2f", totalValuation)
                val pl = totalValuation - totalCost
                val plPct = if (totalCost > 0.0) (pl / totalCost) * 100.0 else 0.0
                tvPortfolioProfitLoss.text = String.format(Locale.US, "Unrealized P/L: %s$%,.2f (%.2f%%)", if (pl >= 0) "+" else "", pl, plPct)

                val adapter = MarketInstrumentAdapter(instrumentsList) { _ -> }
                rvHoldings.layoutManager = LinearLayoutManager(this@PortfolioActivity)
                rvHoldings.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
