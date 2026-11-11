package com.repp.aquaticremineztools.presentation.manga

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.MangaPinManager
import kotlinx.coroutines.launch

class MangaPinActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var tvPinTitle: TextView
    private lateinit var tvPinSubtitle: TextView
    private lateinit var tvPinError: TextView

    private lateinit var dots: List<View>

    private val inputPin = StringBuilder()
    private var tempFirstPin: String? = null
    private var isCreatingPin = false
    private var isVerifying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manga_pin)

        bindViews()
        setupState()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBack)
        tvPinTitle = findViewById(R.id.tvPinTitle)
        tvPinSubtitle = findViewById(R.id.tvPinSubtitle)
        tvPinError = findViewById(R.id.tvPinError)

        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3),
            findViewById(R.id.dot4),
            findViewById(R.id.dot5),
            findViewById(R.id.dot6)
        )
    }

    private fun setupState() {
        try {
            isCreatingPin = !MangaPinManager.hasPin(this)
            if (isCreatingPin) {
                tvPinTitle.text = "CREATE 6-DIGIT PIN"
                tvPinSubtitle.text = "Set a 6-digit PIN for Private Downloader access"
            } else {
                tvPinTitle.text = "ENTER SECURITY PIN"
                tvPinSubtitle.text = "Enter 6-digit PIN to access Private Downloader"
            }
            updateLockoutState()
        } catch (e: SecurityException) {
            showError("Secure storage error: ${e.localizedMessage ?: "Access denied"}")
            tvPinTitle.text = "SECURITY ERROR"
            tvPinSubtitle.text = "Encrypted storage unavailable"
            updateKeypadEnabled(false)
        } catch (e: Exception) {
            showError("Storage error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun updateLockoutState() {
        try {
            if (MangaPinManager.isLockedOut(this)) {
                val remainingSec = MangaPinManager.getRemainingLockoutSeconds(this)
                showError("Too many failed attempts. Try again in ${remainingSec}s")
            } else {
                if (tvPinError.text.startsWith("Too many failed attempts") || tvPinError.text.startsWith("Secure storage")) {
                    tvPinError.visibility = View.INVISIBLE
                }
            }
        } catch (e: SecurityException) {
            showError("Secure storage error: ${e.localizedMessage ?: "Access denied"}")
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        val keyIds = listOf(
            R.id.btnKey0 to "0",
            R.id.btnKey1 to "1",
            R.id.btnKey2 to "2",
            R.id.btnKey3 to "3",
            R.id.btnKey4 to "4",
            R.id.btnKey5 to "5",
            R.id.btnKey6 to "6",
            R.id.btnKey7 to "7",
            R.id.btnKey8 to "8",
            R.id.btnKey9 to "9"
        )

        keyIds.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener {
                if (!isVerifying) {
                    onDigitPressed(digit)
                }
            }
        }

        findViewById<Button>(R.id.btnKeyDelete).setOnClickListener {
            if (!isVerifying) {
                onDeletePressed()
            }
        }

        findViewById<Button>(R.id.btnKeyClear).setOnClickListener {
            if (!isVerifying) {
                onClearPressed()
            }
        }
    }

    private fun onDigitPressed(digit: String) {
        if (MangaPinManager.isLockedOut(this)) {
            updateLockoutState()
            return
        }

        if (inputPin.length < 6) {
            inputPin.append(digit)
            updateDots()
            if (inputPin.length == 6) {
                onPinComplete()
            }
        }
    }

    private fun onDeletePressed() {
        if (inputPin.isNotEmpty()) {
            inputPin.deleteCharAt(inputPin.length - 1)
            updateDots()
        }
    }

    private fun onClearPressed() {
        inputPin.clear()
        updateDots()
    }

    private fun updateDots() {
        dots.forEachIndexed { index, view ->
            if (index < inputPin.length) {
                view.setBackgroundResource(R.drawable.bg_manga_pin_dot_on)
            } else {
                view.setBackgroundResource(R.drawable.bg_manga_pin_dot_off)
            }
        }
    }

    private fun onPinComplete() {
        val pin = inputPin.toString()
        inputPin.clear()
        updateDots()

        isVerifying = true
        updateKeypadEnabled(false)
        val oldSubtitle = tvPinSubtitle.text.toString()
        tvPinSubtitle.text = if (isCreatingPin && tempFirstPin != null) "Confirming PIN..." else if (isCreatingPin) "Saving PIN..." else "Verifying PIN..."

        lifecycleScope.launch {
            try {
                if (isCreatingPin) {
                    if (tempFirstPin == null) {
                        tempFirstPin = pin
                        tvPinTitle.text = "CONFIRM 6-DIGIT PIN"
                        tvPinSubtitle.text = "Re-enter the 6-digit PIN to confirm"
                    } else {
                        if (pin == tempFirstPin) {
                            MangaPinManager.savePin(this@MangaPinActivity, pin)
                            Toast.makeText(this@MangaPinActivity, "PIN successfully created", Toast.LENGTH_SHORT).show()
                            openDownloaderHub()
                        } else {
                            tempFirstPin = null
                            tvPinTitle.text = "CREATE 6-DIGIT PIN"
                            tvPinSubtitle.text = "Set a 6-digit PIN for Private Downloader access"
                            showError("PINs do not match. Try again.")
                        }
                    }
                } else {
                    val isCorrect = MangaPinManager.verifyPin(this@MangaPinActivity, pin)
                    if (isCorrect) {
                        openDownloaderHub()
                    } else {
                        if (MangaPinManager.isLockedOut(this@MangaPinActivity)) {
                            updateLockoutState()
                        } else {
                            showError("Incorrect PIN. Please try again.")
                        }
                    }
                }
            } catch (e: SecurityException) {
                showError("Secure storage error: ${e.localizedMessage ?: "Access denied"}")
                tvPinSubtitle.text = oldSubtitle
            } catch (e: Exception) {
                showError("Error: ${e.localizedMessage ?: "Unknown error"}")
                tvPinSubtitle.text = oldSubtitle
            } finally {
                isVerifying = false
                updateKeypadEnabled(true)
            }
        }
    }

    private fun updateKeypadEnabled(enabled: Boolean) {
        val keyIds = listOf(
            R.id.btnKey0, R.id.btnKey1, R.id.btnKey2, R.id.btnKey3, R.id.btnKey4,
            R.id.btnKey5, R.id.btnKey6, R.id.btnKey7, R.id.btnKey8, R.id.btnKey9,
            R.id.btnKeyDelete, R.id.btnKeyClear
        )
        keyIds.forEach { id ->
            findViewById<Button>(id)?.isEnabled = enabled
        }
    }

    private fun showError(message: String) {
        tvPinError.text = message
        tvPinError.visibility = View.VISIBLE
    }

    private fun openDownloaderHub() {
        val intent = Intent(this, com.repp.aquaticremineztools.presentation.video.PrivateDownloaderHubActivity::class.java)
        startActivity(intent)
        finish()
    }
}
