package com.repp.aquaticremineztools.presentation.security

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.PinManager

class PinChangeActivity : AppCompatActivity() {

    private lateinit var rvKeypad: RecyclerView
    private lateinit var btnBack: FrameLayout
    private lateinit var tvSubtitle: TextView
    private lateinit var tvError: TextView

    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var dot4: View
    private lateinit var dot5: View
    private lateinit var dot6: View

    private var enteredPin = ""
    private var step = 1
    private var oldPin = ""
    private var newPin = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_change)

        bindViews()
        setupKeypad()
        btnBack.setOnClickListener { finish() }
    }

    private fun bindViews() {
        rvKeypad = findViewById(R.id.rvKeypad)
        btnBack = findViewById(R.id.btnBackPinChange)
        tvSubtitle = findViewById(R.id.tvPinChangeSubtitle)
        tvError = findViewById(R.id.tvPinChangeError)

        dot1 = findViewById(R.id.dot1)
        dot2 = findViewById(R.id.dot2)
        dot3 = findViewById(R.id.dot3)
        dot4 = findViewById(R.id.dot4)
        dot5 = findViewById(R.id.dot5)
        dot6 = findViewById(R.id.dot6)
    }

    private fun setupKeypad() {
        val adapter = KeypadAdapter(
            onNumberClick = { number ->
                if (enteredPin.length < 6) {
                    enteredPin += number
                    updateDots()
                    tvError.visibility = View.INVISIBLE
                    if (enteredPin.length == 6) onPinComplete()
                }
            },
            onBackspaceClick = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    updateDots()
                }
            },
            onClearAllClick = {
                enteredPin = ""
                updateDots()
                tvError.visibility = View.INVISIBLE
            },
        )
        rvKeypad.layoutManager = GridLayoutManager(this, 3)
        rvKeypad.adapter = adapter
    }

    private fun onPinComplete() {
        when (step) {
            1 -> {
                if (PinManager.verifyPin(this, enteredPin)) {
                    oldPin = enteredPin
                    enteredPin = ""
                    step = 2
                    tvSubtitle.setText(R.string.pin_change_new)
                    resetDots()
                } else {
                    showError(getString(R.string.pin_error_wrong))
                    enteredPin = ""
                    resetDots()
                }
            }
            2 -> {
                newPin = enteredPin
                enteredPin = ""
                step = 3
                tvSubtitle.setText(R.string.pin_change_confirm)
                resetDots()
            }
            3 -> {
                if (enteredPin == newPin) {
                    PinManager.updatePin(this, newPin)
                    Toast.makeText(this, "PIN changed successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    showError(getString(R.string.pin_error_mismatch))
                    enteredPin = ""
                    newPin = ""
                    step = 2
                    tvSubtitle.setText(R.string.pin_change_new)
                    resetDots()
                }
            }
        }
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

    private fun resetDots() {
        val dots = listOf(dot1, dot2, dot3, dot4, dot5, dot6)
        dots.forEach { it.setBackgroundResource(R.drawable.bg_pin_dot_empty) }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}