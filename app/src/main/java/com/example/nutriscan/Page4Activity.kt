package com.example.nutriscan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class Page4Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page4)

        auth = Firebase.auth

        if (auth.currentUser != null) {
            goToHomeActivity()
        }

        // 1. Konfigurasi Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    showCustomToast("Gagal Login Google: ${e.message}")
                }
            }
        }

        // --- Event Listener Tombol ---

        // Tombol Google
        findViewById<Button>(R.id.google_button).setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
        
        // Tombol Lewati
        findViewById<TextView>(R.id.skip_button).setOnClickListener {
            val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().putBoolean("skipped_login", true).apply()
            goToHomeActivity()
        }

        // Tombol Masuk (Login Manual)
        findViewById<Button>(R.id.login_button).setOnClickListener {
            performLogin()
        }

        // Tombol Daftar (Link ke Register)
        findViewById<TextView>(R.id.signup_link).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Tombol Lupa Password - SEKARANG SUDAH AKTIF
        findViewById<TextView>(R.id.forgot_password).setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = findViewById<EditText>(R.id.email_input).text.toString().trim()
        val password = findViewById<EditText>(R.id.password_input).text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showCustomToast("Email dan kata sandi tidak boleh kosong.")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showCustomToast("Selamat datang kembali!")
                    goToHomeActivity()
                } else {
                    val exception = task.exception
                    when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            showCustomToast("Akun tidak ditemukan. Silakan lakukan pendaftaran terlebih dahulu.")
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            showCustomToast("Email atau kata sandi yang Anda masukkan tidak valid.")
                        }
                        else -> {
                            showCustomToast("Terjadi kesalahan saat masuk. Silakan coba kembali.")
                        }
                    }
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showCustomToast("Selamat Datang!")
                    goToHomeActivity()
                } else {
                    showCustomToast("Gagal masuk menggunakan akun Google.")
                }
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

    private fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
