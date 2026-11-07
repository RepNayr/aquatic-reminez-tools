package com.repp.aquaticremineztools.presentation.manga

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.MangaPinManager

class MangaPinActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var tvPinTitle: TextView
    private lateinit var tvPinSubtitle: TextView
    private lateinit var tvPinError: TextView

    private lateinit var dots: List<View>

    private val inputPin = StringBuilder()
    private var tempFirstPin: String? = null
    private var isCreatingPin = false

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
        isCreatingPin = !MangaPinManager.hasPin(this)
        if (isCreatingPin) {
            tvPinTitle.text = "CREATE 6-DIGIT PIN"
            tvPinSubtitle.text = "Set a 6-digit PIN for Private Manga Downloader access"
        } else {
            tvPinTitle.text = "ENTER SECURITY PIN"
            tvPinSubtitle.text = "Enter 6-digit PIN to access Private Manga Downloader"
        }
        updateLockoutState()
    }

    private fun updateLockoutState() {
        if (MangaPinManager.isLockedOut(this)) {
            val remainingSec = MangaPinManager.getRemainingLockoutSeconds(this)
            showError("Too many failed attempts. Try again in ${remainingSec}s")
        } else {
            tvPinError.visibility = View.INVISIBLE
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
                onDigitPressed(digit)
            }
        }

        findViewById<Button>(R.id.btnKeyDelete).setOnClickListener {
            onDeletePressed()
        }

        findViewById<Button>(R.id.btnKeyClear).setOnClickListener {
            onClearPressed()
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

        if (isCreatingPin) {
            if (tempFirstPin == null) {
                tempFirstPin = pin
                tvPinTitle.text = "CONFIRM 6-DIGIT PIN"
                tvPinSubtitle.text = "Re-enter the 6-digit PIN to confirm"
            } else {
                if (pin == tempFirstPin) {
                    MangaPinManager.savePin(this, pin)
                    Toast.makeText(this, "PIN successfully created", Toast.LENGTH_SHORT).show()
                    openMangaDownloader()
                } else {
                    tempFirstPin = null
                    tvPinTitle.text = "CREATE 6-DIGIT PIN"
                    tvPinSubtitle.text = "Set a 6-digit PIN for Private Manga Downloader access"
                    showError("PINs do not match. Try again.")
                }
            }
        } else {
            val isCorrect = MangaPinManager.verifyPin(this, pin)
            if (isCorrect) {
                openMangaDownloader()
            } else {
                if (MangaPinManager.isLockedOut(this)) {
                    updateLockoutState()
                } else {
                    showError("Incorrect PIN. Please try again.")
                }
            }
        }
    }

    private fun showError(message: String) {
        tvPinError.text = message
        tvPinError.visibility = View.VISIBLE
    }

    private fun openMangaDownloader() {
        val intent = Intent(this, MangaActivity::class.java)
        startActivity(intent)
        finish()
    }
}
