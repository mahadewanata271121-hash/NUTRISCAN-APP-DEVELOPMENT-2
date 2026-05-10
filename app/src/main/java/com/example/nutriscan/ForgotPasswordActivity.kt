package com.example.nutriscan

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

/**
 * ForgotPasswordActivity - Performance Optimized
 * Fokus pada efisiensi validasi input dan manajemen memori toast.
 */
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var emailError: TextView
    
    // Memory Cache untuk Toast
    private var customToast: Toast? = null
    private var toastTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()
        bindViews()
    }

    private fun bindViews() {
        emailInput = findViewById(R.id.email_input)
        emailError = findViewById(R.id.email_error)
        
        findViewById<Button>(R.id.send_link_button).setOnClickListener { sendPasswordResetLink() }
        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }
        findViewById<TextView>(R.id.back_to_login_text).setOnClickListener { finish() }
    }

    private fun sendPasswordResetLink() {
        val email = emailInput.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError.visibility = View.VISIBLE
            emailInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            return
        }

        emailError.visibility = View.GONE
        emailInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showEfficientToast("Link reset terkirim! Cek email Anda.")
                    finish()
                } else {
                    showEfficientToast("Gagal: ${task.exception?.message}")
                }
            }
    }

    private fun showEfficientToast(message: String) {
        if (customToast == null) {
            val layout = LayoutInflater.from(this).inflate(R.layout.custom_toast_layout, null)
            toastTextView = layout.findViewById(R.id.toast_text)
            customToast = Toast(applicationContext).apply {
                duration = Toast.LENGTH_LONG
                view = layout
            }
        }
        toastTextView?.text = message
        customToast?.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
