package com.example.nutriscan

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailInput: EditText
    private lateinit var emailError: TextView
    private lateinit var sendLinkButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.email_input)
        emailError = findViewById(R.id.email_error)
        sendLinkButton = findViewById(R.id.send_link_button)

        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val backToLoginText = findViewById<TextView>(R.id.back_to_login_text)
        backToLoginText.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        sendLinkButton.setOnClickListener {
            sendPasswordResetLink()
        }
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
                    Toast.makeText(this, "Link reset password telah dikirim. Silakan cek email Anda.", Toast.LENGTH_LONG).show()
                    // Di sini, idealnya Anda akan mengarahkan pengguna ke halaman yang memberitahu mereka untuk memeriksa email,
                    // atau langsung kembali ke halaman login.
                    finish()
                } else {
                    Toast.makeText(this, "Gagal mengirim link: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
