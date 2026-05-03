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
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var newPasswordError: TextView
    private lateinit var confirmPasswordError: TextView
    private lateinit var resetPasswordButton: Button
    private var oobCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()

        // Ambil oobCode dari data Intent (dari deep link)
        oobCode = intent.data?.getQueryParameter("oobCode")

        newPasswordInput = findViewById(R.id.new_password_input)
        confirmPasswordInput = findViewById(R.id.confirm_password_input)
        newPasswordError = findViewById(R.id.new_password_error)
        confirmPasswordError = findViewById(R.id.confirm_password_error)
        resetPasswordButton = findViewById(R.id.reset_password_button)

        val backButton = findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            finish()
        }

        resetPasswordButton.setOnClickListener {
            resetPassword()
        }

        if (oobCode == null) {
            Toast.makeText(this, "Invalid reset link.", Toast.LENGTH_LONG).show()
            // Sebaiknya kembali ke halaman login jika link tidak valid
            // finish()
        }
    }

    private fun validatePassword(): Boolean {
        val newPassword = newPasswordInput.text.toString().trim()
        val confirmPassword = confirmPasswordInput.text.toString().trim()
        var isValid = true

        if (newPassword.length < 6) {
            newPasswordError.text = "Password harus memiliki minimal 6 karakter"
            newPasswordError.visibility = View.VISIBLE
            newPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            isValid = false
        } else {
            newPasswordError.visibility = View.GONE
            newPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        if (newPassword != confirmPassword) {
            confirmPasswordError.text = "Password tidak cocok"
            confirmPasswordError.visibility = View.VISIBLE
            confirmPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext_error)
            isValid = false
        } else {
            confirmPasswordError.visibility = View.GONE
            confirmPasswordInput.background = ContextCompat.getDrawable(this, R.drawable.bg_dark_edittext)
        }

        return isValid
    }

    private fun resetPassword() {
        if (!validatePassword()) return

        val newPassword = newPasswordInput.text.toString().trim()

        oobCode?.let {
            auth.confirmPasswordReset(it, newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password berhasil direset. Silakan login kembali.", Toast.LENGTH_LONG).show()
                        // Arahkan kembali ke halaman login
                        val intent = Intent(this, Page4Activity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Gagal mereset password: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
