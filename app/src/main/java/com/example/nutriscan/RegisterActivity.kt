package com.example.nutriscan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth

        val nameInput = findViewById<EditText>(R.id.name_input)
        val emailInput = findViewById<EditText>(R.id.email_input_reg)
        val passwordInput = findViewById<EditText>(R.id.password_input_reg)
        val registerButton = findViewById<Button>(R.id.register_button)
        val backToLogin = findViewById<TextView>(R.id.back_to_login)

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showCustomToast("Harap lengkapi semua kolom pendaftaran.")
                return@setOnClickListener
            }

            if (password.length < 6) {
                showCustomToast("Demi keamanan, gunakan minimal 6 karakter password.")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        user?.updateProfile(profileUpdates)

                        user?.sendEmailVerification()
                            ?.addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    showCustomToast("Pendaftaran berhasil! Silakan verifikasi email Anda.")
                                    auth.signOut()
                                    finish()
                                }
                            }
                    } else {
                        val exception = task.exception
                        if (exception is FirebaseAuthUserCollisionException) {
                            // EMAIL SUDAH TERDAFTAR (Mungkin lewat Google)
                            showCustomToast("Email ini sudah terdaftar. Silakan masuk melalui halaman Login atau gunakan fitur Lupa Password.")
                        } else {
                            showCustomToast("Pendaftaran terkendala: ${exception?.localizedMessage}")
                        }
                    }
                }
        }

        backToLogin.setOnClickListener {
            // REDIRECT TO LOGIN PAGE (Page4Activity) INSTEAD OF JUST FINISHING
            val intent = Intent(this, Page4Activity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showCustomToast(message: String) {
        val layout = LayoutInflater.from(this).inflate(R.layout.custom_toast_layout, null)
        layout.findViewById<TextView>(R.id.toast_text).text = message
        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_SHORT
        toast.view = layout
        toast.show()
    }
}
