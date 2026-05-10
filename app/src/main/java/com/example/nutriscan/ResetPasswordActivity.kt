package com.example.nutriscan

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ResetPasswordActivity - Stability Optimized
 * Menjamin transisi reset password yang aman dan responsif.
 */
class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var newPasswordError: TextView
    private lateinit var confirmPasswordError: TextView
    private var oobCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()
        oobCode = intent.data?.getQueryParameter("oobCode")

        initViews()
        checkLinkValidity()
    }

    private fun initViews() {
        newPasswordInput = findViewById(R.id.new_password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        newPasswordError = findViewById(R.id.new_password_error)
        confirmPasswordError = findViewById(R.id.confirm_password_error)

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }
        findViewById<Button>(R.id.reset_password_button).setOnClickListener { handleReset() }
    }

    private fun checkLinkValidity() {
        if (oobCode == null) {
            Toast.makeText(this, "Link reset tidak valid atau sudah kadaluwarsa.", Toast.LENGTH_LONG).show()
            // Memberikan jeda singkat agar user bisa membaca pesan sebelum kembali
            lifecycleScope.launch {
                kotlinx.coroutines.delay(2000)
                finish()
            }
        }
    }

    private fun handleReset() {
        if (!validatePassword()) return
        val newPassword = newPasswordInput.text.toString().trim()
        val code = oobCode ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            auth.confirmPasswordReset(code, newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "Password berhasil direset!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@ResetPasswordActivity, Page4Activity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ResetPasswordActivity, "Gagal: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun validatePassword(): Boolean {
        val pass = newPasswordInput.text.toString().trim()
        val confirm = confirmPasswordInput.text.toString().trim()
        var valid = true

        if (pass.length < 6) {
            newPasswordError.apply { text = "Minimal 6 karakter"; visibility = View.VISIBLE }
            newPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            valid = false
        } else {
            newPasswordError.visibility = View.GONE
            newPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        if (pass != confirm) {
            confirmPasswordError.apply { text = "Password tidak cocok"; visibility = View.VISIBLE }
            confirmPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            valid = false
        } else {
            confirmPasswordError.visibility = View.GONE
            confirmPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        return valid
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
}
