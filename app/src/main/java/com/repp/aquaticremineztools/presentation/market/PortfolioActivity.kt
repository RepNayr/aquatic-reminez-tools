package com.repp.aquaticremineztools.presentation.market

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.data.model.MarketInstrument
import com.repp.aquaticremineztools.data.model.PortfolioHolding
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
    private lateinit var viewTopSpacer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_portfolio)

        bindViews()
        setupWindowInsets()
        setupListeners()
        loadPortfolio()
    }

    private fun bindViews() {
        tvPortfolioTotalValue = findViewById(R.id.tvPortfolioTotalValue)
        tvPortfolioProfitLoss = findViewById(R.id.tvPortfolioProfitLoss)
        rvHoldings = findViewById(R.id.rvHoldings)
        btnBack = findViewById(R.id.btnBack)
        btnAddHolding = findViewById(R.id.btnAddHolding)
        viewTopSpacer = findViewById(R.id.viewTopSpacer)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContentContainer)) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val lpSpacer = viewTopSpacer.layoutParams
            lpSpacer.height = statusBarHeight
            viewTopSpacer.layoutParams = lpSpacer
            insets
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAddHolding.setOnClickListener {
            showAddHoldingDialog()
        }
    }

    private fun loadPortfolio() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@PortfolioActivity)
            val holdings = repository.getPortfolioHoldings()
            val marketResult = repository.getAllMarketInstruments(forceRefresh = false)
            val instrumentsMap = marketResult.getOrNull()?.associateBy { it.id } ?: emptyMap()

            val valuationResult = repository.calculatePortfolioValuation(holdings, instrumentsMap)

            val holdingsDisplayList = holdings.map { holding ->
                val inst = instrumentsMap[holding.instrumentId]
                val currentPrice = inst?.currentPrice ?: holding.averageCost
                val value = holding.quantity * currentPrice
                val cost = holding.quantity * holding.averageCost

                MarketInstrument(
                    id = holding.id,
                    symbol = holding.symbol,
                    name = "${holding.name} (${holding.quantity} units)",
                    category = inst?.category ?: com.repp.aquaticremineztools.data.model.MarketCategory.OVERVIEW,
                    currentPrice = value,
                    priceChange24h = value - cost,
                    priceChangePercentage24h = if (cost > 0.0) ((value - cost) / cost) * 100.0 else 0.0,
                    currency = holding.currency,
                    exchange = "HOLDING",
                    imageUrl = inst?.imageUrl
                )
            }

            withContext(Dispatchers.Main) {
                val totalValStr = if (valuationResult.totalValuationIdr > 0) {
                    String.format(
                        Locale.US,
                        "$%,.2f USD  |  Rp %,.2f IDR",
                        valuationResult.totalValuationUsd,
                        valuationResult.totalValuationIdr
                    )
                } else {
                    String.format(Locale.US, "$%,.2f", valuationResult.totalValuationUsd)
                }
                tvPortfolioTotalValue.text = totalValStr

                val plUsdSign = if (valuationResult.unrealizedPlUsd >= 0) "+" else ""
                val plText = if (valuationResult.hasUnpricedHoldings) {
                    String.format(
                        Locale.US,
                        "Unrealized P/L: %s$%,.2f (%.2f%%) · Contains Cost-Basis Fallbacks",
                        plUsdSign,
                        valuationResult.unrealizedPlUsd,
                        valuationResult.unrealizedPlPctUsd
                    )
                } else {
                    String.format(
                        Locale.US,
                        "Unrealized P/L: %s$%,.2f (%.2f%%)",
                        plUsdSign,
                        valuationResult.unrealizedPlUsd,
                        valuationResult.unrealizedPlPctUsd
                    )
                }
                tvPortfolioProfitLoss.text = plText

                val adapter = MarketInstrumentAdapter(holdingsDisplayList) { displayInst ->
                    val foundHolding = holdings.find { it.id == displayInst.id }
                    if (foundHolding != null) {
                        showDeleteHoldingDialog(foundHolding)
                    }
                }
                rvHoldings.layoutManager = LinearLayoutManager(this@PortfolioActivity)
                rvHoldings.adapter = adapter
            }
        }
    }

    private fun showAddHoldingDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@PortfolioActivity)
            val result = repository.getAllMarketInstruments(forceRefresh = false)
            val instruments = result.getOrNull() ?: emptyList()

            withContext(Dispatchers.Main) {
                if (instruments.isEmpty()) {
                    Toast.makeText(this@PortfolioActivity, "No market instruments available to add to portfolio", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val dialogView = layoutInflater.inflate(R.layout.dialog_add_holding, null)
                val etQuantity = dialogView.findViewById<EditText>(R.id.etHoldingQuantity)
                val etCost = dialogView.findViewById<EditText>(R.id.etHoldingCost)

                val instrumentNames = instruments.map { "${it.symbol} - ${it.name}" }.toTypedArray()
                var selectedInstrumentIndex = 0

                AlertDialog.Builder(this@PortfolioActivity, R.style.Theme_AquaticReminezTools_Dialog)
                    .setTitle("Select Instrument")
                    .setSingleChoiceItems(instrumentNames, 0) { _, which ->
                        selectedInstrumentIndex = which
                    }
                    .setPositiveButton("Next") { _, _ ->
                        val targetInstrument = instruments.getOrNull(selectedInstrumentIndex) ?: return@setPositiveButton
                        etCost.setText(targetInstrument.currentPrice?.toString() ?: "0.0")

                        AlertDialog.Builder(this@PortfolioActivity, R.style.Theme_AquaticReminezTools_Dialog)
                            .setTitle("Add Holding: ${targetInstrument.symbol}")
                            .setView(dialogView)
                            .setPositiveButton("Save Holding") { _, _ ->
                                val qty = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                                val cost = etCost.text.toString().toDoubleOrNull() ?: 0.0

                                if (qty > 0 && cost >= 0) {
                                    val newHolding = PortfolioHolding(
                                        instrumentId = targetInstrument.id,
                                        symbol = targetInstrument.symbol,
                                        name = targetInstrument.name,
                                        quantity = qty,
                                        averageCost = cost,
                                        currency = targetInstrument.currency
                                    )
                                    repository.addPortfolioHolding(newHolding)
                                    Toast.makeText(this@PortfolioActivity, "Added holding for ${targetInstrument.symbol}", Toast.LENGTH_SHORT).show()
                                    loadPortfolio()
                                } else {
                                    Toast.makeText(this@PortfolioActivity, "Invalid quantity or cost", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun showDeleteHoldingDialog(holding: PortfolioHolding) {
        AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
            .setTitle("Manage Holding")
            .setMessage("Remove holding for ${holding.symbol} (${holding.quantity} units)?")
            .setPositiveButton("Remove") { _, _ ->
                MarketRepository.getInstance(this).removePortfolioHolding(holding.id)
                Toast.makeText(this, "Holding removed", Toast.LENGTH_SHORT).show()
                loadPortfolio()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }
}
