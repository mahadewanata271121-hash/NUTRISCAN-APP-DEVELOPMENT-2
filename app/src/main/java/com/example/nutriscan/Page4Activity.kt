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
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Page4Activity (Login) - Performance Optimized
 * Menggunakan View Caching dan Async Firebase calls untuk stabilitas maksimal.
 */
class Page4Activity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    
    // View Caching
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private var customToast: Toast? = null
    private var toastTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page4)

        auth = Firebase.auth
        if (auth.currentUser != null) {
            goToHomeActivity()
            return
        }

        bindViews()
        setupGoogleSignIn()
    }

    private fun bindViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)

        findViewById<Button>(R.id.login_button).setOnClickListener { performLogin() }
        findViewById<Button>(R.id.google_button).setOnClickListener { startGoogleLogin() }
        findViewById<TextView>(R.id.signup_link).setOnClickListener { 
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<TextView>(R.id.forgot_password).setOnClickListener { 
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        findViewById<TextView>(R.id.skip_button).setOnClickListener { 
            handleSkipLogin()
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showEfficientToast("Gagal Login Google")
            }
        }
    }

    private fun startGoogleLogin() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun performLogin() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showEfficientToast("Email dan sandi tidak boleh kosong.")
            return
        }

        // Optimasi: Jalankan auth di context yang tepat
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showEfficientToast("Selamat datang kembali!")
                    goToHomeActivity()
                } else {
                    val msg = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan."
                        is FirebaseAuthInvalidCredentialsException -> "Sandi salah."
                        else -> "Masuk terkendala."
                    }
                    showEfficientToast(msg)
                }
            }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) goToHomeActivity()
            else showEfficientToast("Gagal masuk akun Google.")
        }
    }

    private fun handleSkipLogin() {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
            .putBoolean("skipped_login", true).apply()
        goToHomeActivity()
    }

    private fun showEfficientToast(message: String) {
        if (customToast == null) {
            val layout = LayoutInflater.from(this).inflate(R.layout.custom_toast_layout, null)
            toastTextView = layout.findViewById(R.id.toast_text)
            customToast = Toast(applicationContext).apply {
                duration = Toast.LENGTH_SHORT
                view = layout
            }
        }
        toastTextView?.text = message
        customToast?.show()
    }

    private fun goToHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
