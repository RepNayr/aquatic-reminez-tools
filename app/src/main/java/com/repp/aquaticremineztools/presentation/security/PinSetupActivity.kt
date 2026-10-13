package com.repp.aquaticremineztools.presentation.security

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R

class PinSetupActivity : AppCompatActivity() {

    private lateinit var rvKeypad: RecyclerView
    private lateinit var btnBack: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvError: TextView

    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var dot4: View
    private lateinit var dot5: View
    private lateinit var dot6: View

    private var enteredPin = ""
    private var firstPin = ""
    private var isConfirmStep = false
    private var isProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_setup)

        bindViews()
        setupKeypad()
        setupListeners()
    }

    private fun bindViews() {
        rvKeypad = findViewById(R.id.rvKeypad)
        btnBack = findViewById(R.id.btnBackPinSetup)
        tvTitle = findViewById(R.id.tvPinSetupTitle)
        tvSubtitle = findViewById(R.id.tvPinSetupSubtitle)
        tvError = findViewById(R.id.tvPinSetupError)

        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        dot4 = findViewById(R.id.dot4)
        dot5 = findViewById(R.id.dot5)
        dot6 = findViewById(R.id.dot6)
    }

    private fun setupListeners() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(enabled = true) {
                override fun handleOnBackPressed() {
                    if (isConfirmStep) {
                        isConfirmStep = false
                        firstPin = ""
                        enteredPin = ""
                        tvSubtitle.setText(R.string.pin_setup_subtitle)
                        updateDots()
                        hideError()
                    } else {
                        finish()
                    }
                }
            },
        )

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupKeypad() {
        val adapter = KeypadAdapter(
            onNumberClick = { number ->
                if (!isProcessing && (enteredPin.length < 6)) {
                    enteredPin += number
                    updateDots()
                    hideError()

                    if (enteredPin.length == 6) {
                        onPinComplete()
                    }
                }
            },
            onBackspaceClick = {
                if (!isProcessing && enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    updateDots()
                    hideError()
                }
            },
            onClearAllClick = {
                if (!isProcessing) {
                    enteredPin = ""
                    updateDots()
                    hideError()
                }
            },
        )
        rvKeypad.layoutManager = GridLayoutManager(this, 3)
        rvKeypad.adapter = adapter
    }

    private fun onPinComplete() {
        if (!isConfirmStep) {
            firstPin = enteredPin
            isConfirmStep = true
            isProcessing = true
            tvSubtitle.setText(R.string.pin_setup_confirm_subtitle)
            rvKeypad.postDelayed({
                enteredPin = ""
                updateDots()
                isProcessing = false
            }, 200)
        } else {
            if (enteredPin == firstPin) {
                goToSecurityQuestion()
            } else {
                showError(getString(R.string.pin_error_mismatch))
                isProcessing = true
                rvKeypad.postDelayed({
                    enteredPin = ""
                    firstPin = ""
                    isConfirmStep = false
                    tvSubtitle.setText(R.string.pin_setup_subtitle)
                    updateDots()
                    isProcessing = false
                }, 400)
            }
        }
    }

    private fun goToSecurityQuestion() {
        val intent = Intent(this, SecurityQuestionActivity::class.java)
        intent.putExtra("mode", "setup")
        intent.putExtra("pin", firstPin)
        startActivity(intent)
        finish()
    }

    private fun updateDots() {
        val dots = listOf(dot1, dot2, dot3, dot4, dot5, dot6)
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < enteredPin.length) R.drawable.bg_pin_dot_filled
                else R.drawable.bg_pin_dot_empty,
            )
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.INVISIBLE
    }
}
