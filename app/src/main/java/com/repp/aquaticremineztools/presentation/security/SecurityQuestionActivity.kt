package com.repp.aquaticremineztools.presentation.security

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.repp.aquaticremineztools.R
import com.repp.aquaticremineztools.core.security.PinManager
import com.repp.aquaticremineztools.presentation.home.HomeActivity

class SecurityQuestionActivity : AppCompatActivity() {

    private lateinit var btnBack: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var etAnswer: EditText
    private lateinit var tvError: TextView
    private lateinit var btnAction: AppCompatButton
    private lateinit var btnResetApp: TextView

    private var mode: String = "setup"
    private var pin: String = ""
    private var wrongAttempts = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_question)

        mode = intent.getStringExtra("mode") ?: "setup"
        pin = intent.getStringExtra("pin") ?: ""

        bindViews()
        setupLowercaseInput()
        setupUI()
        setupListeners()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackSecQ)
        tvTitle = findViewById(R.id.tvSecQTitle)
        tvQuestion = findViewById(R.id.tvQuestionText)
        etAnswer = findViewById(R.id.etAnswer)
        tvError = findViewById(R.id.tvSecQError)
        btnAction = findViewById(R.id.btnSecQAction)
        btnResetApp = findViewById(R.id.btnResetApp)
    }

    private fun setupLowercaseInput() {
        etAnswer.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        etAnswer.addTextChangedListener(
            object : TextWatcher {
                private var isUpdating = false

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (isUpdating) return
                    val original = s.toString()
                    val lowercase = original.lowercase()

                    if (original != lowercase) {
                        isUpdating = true
                        etAnswer.setText(lowercase)
                        etAnswer.setSelection(lowercase.length)
                        isUpdating = false
                    }
                }
            },
        )
    }

    private fun setupUI() {
        tvTitle.setText(R.string.security_question_title)
        if (mode == "setup") {
            tvQuestion.text = getString(R.string.security_question_default)
            btnAction.setText(R.string.security_question_save)
            btnResetApp.visibility = View.GONE
        } else {
            tvQuestion.text = PinManager.getSecurityQuestion(this)
            btnAction.setText(R.string.security_question_verify)
            btnResetApp.visibility = View.VISIBLE
            btnResetApp.setText(R.string.reset_app_confirm)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        btnAction.setOnClickListener {
            val answer = etAnswer.text.toString().trim()

            if (answer.isEmpty()) {
                showError("Jawaban tidak boleh kosong")
                return@setOnClickListener
            }

            if (mode == "setup") {
                val question = tvQuestion.text.toString()
                PinManager.savePin(this, pin, question, answer)
                Toast.makeText(this, "PIN berhasil dibuat", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, HomeActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            } else {
                if (PinManager.verifyAnswer(this, answer)) {
                    Toast.makeText(this, "Jawaban benar", Toast.LENGTH_SHORT).show()
                    PinManager.deletePin(this)

                    val intent = Intent(this, PinSetupActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    wrongAttempts++
                    if (wrongAttempts >= 3) {
                        Toast.makeText(this, "3x salah. Keluar.", Toast.LENGTH_SHORT).show()
                        finishAffinity()
                    } else {
                        showError("Jawaban salah ($wrongAttempts/3)")
                    }
                }
            }
        }

        btnResetApp.setOnClickListener {
            AlertDialog.Builder(this, R.style.Theme_AquaticReminezTools_Dialog)
                .setTitle(R.string.reset_app_title)
                .setMessage(R.string.reset_app_message)
                .setPositiveButton(R.string.reset_app_confirm) { _, _ ->
                    PinManager.resetAll(this)
                    deleteAppData()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun deleteAppData() {
        try {
            val prefsDir = applicationInfo.dataDir + "/shared_prefs"
            java.io.File(prefsDir).deleteRecursively()

            val storageDir = java.io.File(getExternalFilesDir(null), "AquaticStorage")
            if (storageDir.exists()) storageDir.deleteRecursively()

            Toast.makeText(this, "App reset. Restart aplikasi.", Toast.LENGTH_LONG).show()
            finishAffinity()
        } catch (_: Exception) {
            Toast.makeText(this, "Gagal reset app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}
