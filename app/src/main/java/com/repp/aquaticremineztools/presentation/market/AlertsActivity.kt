package com.repp.aquaticremineztools.presentation.market

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
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
import com.repp.aquaticremineztools.data.model.AlertType
import com.repp.aquaticremineztools.data.model.MarketAlert
import com.repp.aquaticremineztools.data.repository.MarketRepository
import com.repp.aquaticremineztools.presentation.security.PinVerifyActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AlertsActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var layoutAlertsEmptyState: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var btnAddAlert: Button
    private lateinit var viewTopSpacer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
            return
        }

        setContentView(R.layout.activity_alerts)

        bindViews()
        setupWindowInsets()
        setupListeners()
        loadAlerts()
    }

    private fun bindViews() {
        rvAlerts = findViewById(R.id.rvAlerts)
        layoutAlertsEmptyState = findViewById(R.id.layoutAlertsEmptyState)
        btnBack = findViewById(R.id.btnBack)
        btnAddAlert = findViewById(R.id.btnAddAlert)
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

        btnAddAlert.setOnClickListener {
            showAddAlertDialog()
        }
    }

    private fun loadAlerts() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@AlertsActivity)
            val alerts = repository.getAlerts()

            withContext(Dispatchers.Main) {
                if (alerts.isEmpty()) {
                    rvAlerts.visibility = View.GONE
                    layoutAlertsEmptyState.visibility = View.VISIBLE
                } else {
                    rvAlerts.visibility = View.VISIBLE
                    layoutAlertsEmptyState.visibility = View.GONE
                    rvAlerts.layoutManager = LinearLayoutManager(this@AlertsActivity)
                    rvAlerts.adapter = AlertsAdapter(alerts) { alertToDelete ->
                        repository.deleteAlert(alertToDelete.id)
                        loadAlerts()
                    }
                }
            }
        }
    }

    private fun showAddAlertDialog() {
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = MarketRepository.getInstance(this@AlertsActivity)
            val result = repository.getAllMarketInstruments(forceRefresh = false)
            val instruments = result.getOrNull() ?: emptyList()

            withContext(Dispatchers.Main) {
                if (instruments.isEmpty()) {
                    Toast.makeText(this@AlertsActivity, "No market instruments available to create alerts", Toast.LENGTH_SHORT).show()
                    return@withContext
                }

                val dialogView = layoutInflater.inflate(R.layout.dialog_add_alert, null)
                val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerAlertType)
                val etValue = dialogView.findViewById<EditText>(R.id.etTargetValue)

                val adapter = ArrayAdapter(
                    this@AlertsActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    AlertType.entries.map { it.displayName }
                )
                spinnerType.adapter = adapter

                val instrumentNames = instruments.map { "${it.symbol} - ${it.name}" }.toTypedArray()
                var selectedInstrumentIndex = 0

                AlertDialog.Builder(this@AlertsActivity, R.style.Theme_AquaticReminezTools_Dialog)
                    .setTitle("Select Instrument for Alert")
                    .setSingleChoiceItems(instrumentNames, 0) { _, which ->
                        selectedInstrumentIndex = which
                    }
                    .setPositiveButton("Next") { _, _ ->
                        val targetInstrument = instruments.getOrNull(selectedInstrumentIndex) ?: return@setPositiveButton

                        AlertDialog.Builder(this@AlertsActivity, R.style.Theme_AquaticReminezTools_Dialog)
                            .setTitle("Configure Alert: ${targetInstrument.symbol}")
                            .setView(dialogView)
                            .setPositiveButton("Save Alert") { _, _ ->
                                val targetVal = etValue.text.toString().toDoubleOrNull() ?: 0.0
                                val selectedType = AlertType.entries.getOrNull(spinnerType.selectedItemPosition) ?: AlertType.PRICE_ABOVE

                                if (targetVal > 0) {
                                    val newAlert = MarketAlert(
                                        instrumentId = targetInstrument.id,
                                        symbol = targetInstrument.symbol,
                                        name = targetInstrument.name,
                                        targetValue = targetVal,
                                        alertType = selectedType,
                                        isActive = true
                                    )
                                    repository.addAlert(newAlert)
                                    Toast.makeText(this@AlertsActivity, "Alert set for ${targetInstrument.symbol}", Toast.LENGTH_SHORT).show()
                                    loadAlerts()
                                } else {
                                    Toast.makeText(this@AlertsActivity, "Invalid target value", Toast.LENGTH_SHORT).show()
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

    override fun onResume() {
        super.onResume()
        if (PinManager.hasPin(this) && AppLockManager.shouldLock(this)) {
            AppLockManager.redirectToVerify(this, PinVerifyActivity::class.java)
            finish()
        }
    }

    private class AlertsAdapter(
        private val alerts: List<MarketAlert>,
        private val onDelete: (MarketAlert) -> Unit
    ) : RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_market_alert, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(alerts[position], onDelete)
        }

        override fun getItemCount(): Int = alerts.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvAlertSymbol = itemView.findViewById<TextView>(R.id.tvAlertSymbol)
            private val tvAlertName = itemView.findViewById<TextView>(R.id.tvAlertName)
            private val tvAlertCondition = itemView.findViewById<TextView>(R.id.tvAlertCondition)
            private val btnDeleteAlert = itemView.findViewById<ImageView>(R.id.btnDeleteAlert)

            fun bind(alert: MarketAlert, onDelete: (MarketAlert) -> Unit) {
                tvAlertSymbol.text = alert.symbol
                tvAlertName.text = alert.name

                val conditionText = when (alert.alertType) {
                    AlertType.PRICE_ABOVE -> String.format(Locale.US, "Price Above %,.2f", alert.targetValue)
                    AlertType.PRICE_BELOW -> String.format(Locale.US, "Price Below %,.2f", alert.targetValue)
                    AlertType.PCT_CHANGE_24H -> String.format(Locale.US, "24h Move >= %.2f%%", alert.targetValue)
                }
                tvAlertCondition.text = conditionText

                btnDeleteAlert.setOnClickListener { onDelete(alert) }
            }
        }
    }
}
