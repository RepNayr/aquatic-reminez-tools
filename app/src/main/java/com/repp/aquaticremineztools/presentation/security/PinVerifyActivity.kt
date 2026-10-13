package com.repp.aquaticremineztools.presentation.security

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.AppLockManager
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.presentation.home.HomeActivity

class PinVerifyActivity : AppCompatActivity() {

    private lateinit var rvKeypad: RecyclerView
    private lateinit var tvError: TextView
    private lateinit var btnForgotPin: TextView

    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var dot4: View
    private lateinit var dot5: View
    private lateinit var dot6: View

    private var enteredPin = ""
    private var wrongAttempts = 0
    private var isLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_verify)

        // Blokir back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Sengaja kosong — user harus masukkan PIN
            }
        })

        bindViews()
        setupKeypad()
        btnForgotPin.setOnClickListener {
            val intent = Intent(this, SecurityQuestionActivity::class.java)
            intent.putExtra("mode", "recovery")
            startActivity(intent)
            finish()
        }
    }

    private fun bindViews() {
        rvKeypad = findViewById(R.id.rvKeypad)
        tvError = findViewById(R.id.tvPinVerifyError)
        btnForgotPin = findViewById(R.id.btnForgotPin)

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
                if (isLocked) return@KeypadAdapter
                if (enteredPin.length < 6) {
                    enteredPin += number
                    updateDots()
                    tvError.visibility = View.INVISIBLE

                    if (enteredPin.length == 6) {
                        onPinComplete()
                    }
                }
            },
            onBackspaceClick = {
                if (isLocked) return@KeypadAdapter
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    updateDots()
                    tvError.visibility = View.INVISIBLE
                }
            },
            onClearAllClick = {
                if (isLocked) return@KeypadAdapter
                enteredPin = ""
                updateDots()
                tvError.visibility = View.INVISIBLE
            },
        )
        rvKeypad.layoutManager = GridLayoutManager(this, 3)
        rvKeypad.adapter = adapter
    }

    private fun onPinComplete() {
        if (PinManager.verifyPin(this, enteredPin)) {
            PinManager.setLastUnlock(this)
            AppLockManager.resetPauseTimer()
            goToHome()
        } else {
            wrongAttempts++
            enteredPin = ""

            when {
                wrongAttempts >= 10 -> lockout(5 * 60)
                wrongAttempts >= 5 -> lockout(30)
                wrongAttempts >= 3 -> {
                    showError("Wrong PIN. Tap Forgot PIN?")
                    resetDots()
                }
                else -> {
                    showError("${getString(R.string.pin_error_wrong)} ($wrongAttempts/3)")
                    resetDots()
                }
            }
        }
    }

    private fun lockout(seconds: Int) {
        isLocked = true
        var remaining = seconds
        tvError.visibility = View.VISIBLE

        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                tvError.text = getString(R.string.pin_locked_seconds, remaining)
                remaining--
                if (remaining >= 0) {
                    handler.postDelayed(this, 1000)
                } else {
                    isLocked = false
                    wrongAttempts = 0
                    tvError.visibility = View.INVISIBLE
                }
            }
        }
        handler.post(runnable)
        resetDots()
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

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}