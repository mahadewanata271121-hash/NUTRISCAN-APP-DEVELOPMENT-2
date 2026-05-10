package com.example.nutriscan

import android.content.Context
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
    private var customToast: Toast? = null
    private var toastTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = Firebase.auth
        initViews()
    }

    private fun initViews() {
        val nameInput = findViewById<EditText>(R.id.name_input)
        val emailInput = findViewById<EditText>(R.id.email_input_reg)
        val passwordInput = findViewById<EditText>(R.id.password_input_reg)
        val registerButton = findViewById<Button>(R.id.register_button)
        val backToLogin = findViewById<TextView>(R.id.back_to_login)

        registerButton?.setOnClickListener {
            handleRegistration(nameInput, emailInput, passwordInput)
        }

        backToLogin?.setOnClickListener {
            val intent = Intent(this, Page4Activity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun handleRegistration(nameEt: EditText, emailEt: EditText, passEt: EditText) {
        val name = nameEt.text.toString().trim()
        val email = emailEt.text.toString().trim()
        val password = passEt.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showEfficientToast("Harap lengkapi semua kolom.")
            return
        }

        if (password.length < 6) {
            showEfficientToast("Password minimal 6 karakter.")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    
                    user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                        // PERBAIKAN: Hapus status tamu jika registrasi berhasil
                        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
                            .remove("skipped_login").apply()

                        user.sendEmailVerification().addOnCompleteListener { verifyTask ->
                            if (verifyTask.isSuccessful) {
                                showEfficientToast("Pendaftaran berhasil! Silakan cek email Anda.")
                                auth.signOut()
                                finish()
                            }
                        }
                    }
                } else {
                    val exception = task.exception
                    val msg = if (exception is FirebaseAuthUserCollisionException) {
                        "Email sudah terdaftar. Silakan gunakan email lain."
                    } else {
                        exception?.localizedMessage ?: "Pendaftaran gagal."
                    }
                    showEfficientToast(msg)
                }
            }
    }

    private fun showEfficientToast(message: String) {
        if (customToast == null) {
            val layout = LayoutInflater.from(this).inflate(R.layout.custom_toast_layout, null)
            toastTextView = layout?.findViewById(R.id.toast_text)
            customToast = Toast(applicationContext).apply {
                duration = Toast.LENGTH_SHORT
                view = layout
            }
        }
        toastTextView?.text = message
        customToast?.show()
    }
}
