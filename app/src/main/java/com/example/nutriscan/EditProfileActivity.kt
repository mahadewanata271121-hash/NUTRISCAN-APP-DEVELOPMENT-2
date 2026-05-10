package com.example.nutriscan

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        val etName = findViewById<TextInputEditText>(R.id.et_edit_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_edit_email)
        val etHeight = findViewById<TextInputEditText>(R.id.et_edit_height)
        val etWeight = findViewById<TextInputEditText>(R.id.et_edit_weight)
        val etBirthdate = findViewById<TextInputEditText>(R.id.et_edit_birthdate)
        val rgGender = findViewById<RadioGroup>(R.id.rg_edit_gender)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_profile)

        if (currentUser != null) {
            etName?.setText(currentUser.displayName)
            etEmail?.setText(currentUser.email)
            val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
            etHeight?.setText(sharedPref.getString("height", ""))
            etWeight?.setText(sharedPref.getString("weight", ""))
            etBirthdate?.setText(sharedPref.getString("birthdate", ""))
            val savedGender = sharedPref.getString("gender", "")
            if (savedGender == "Laki-laki") findViewById<RadioButton>(R.id.rb_male)?.isChecked = true
            else if (savedGender == "Perempuan") findViewById<RadioButton>(R.id.rb_female)?.isChecked = true
        }

        etBirthdate?.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                etBirthdate.setText("$day/${month + 1}/$year")
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }

        findViewById<ImageButton>(R.id.btn_back_edit_profile)?.setOnClickListener { finish() }

        btnSave?.setOnClickListener {
            val name = etName?.text.toString().trim()
            val hStr = etHeight?.text.toString().trim()
            val wStr = etWeight?.text.toString().trim()
            val bDateStr = etBirthdate?.text.toString().trim()
            val genderId = rgGender?.checkedRadioButtonId ?: -1

            if (name.isEmpty() || hStr.isEmpty() || wStr.isEmpty() || bDateStr.isEmpty() || genderId == -1) {
                Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = findViewById<RadioButton>(genderId).text.toString()
            val age = calculateAge(bDateStr)
            val height = hStr.toDoubleOrNull() ?: 160.0
            val weight = wStr.toDoubleOrNull() ?: 60.0

            // Rumus Mifflin-St Jeor
            val bmr = if (gender == "Laki-laki") (10 * weight) + (6.25 * height) - (5 * age) + 5
                      else (10 * weight) + (6.25 * height) - (5 * age) - 161
            
            // Menggunakan Faktor Aktivitas Sedentary (1.375)
            val dailyGoal = (bmr * 1.375).toInt()
            val carbsGoal = (dailyGoal * 0.50 / 4).toInt()   // 50% Karbo
            val proteinGoal = (dailyGoal * 0.20 / 4).toInt() // 20% Protein
            val fatGoal = (dailyGoal * 0.30 / 9).toInt()     // 30% Lemak

            currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())?.addOnCompleteListener {
                val pref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
                pref.edit().apply {
                    putString("name", name)
                    putString("height", hStr)
                    putString("weight", wStr)
                    putString("birthdate", bDateStr)
                    putString("gender", gender)
                    putInt("daily_goal", dailyGoal)
                    putInt("carbs_goal", carbsGoal)
                    putInt("protein_goal", proteinGoal)
                    putInt("fat_goal", fatGoal) // Ditambahkan agar sinkron
                    apply()
                }
                AlertDialog.Builder(this).setTitle("Berhasil").setMessage("Target harian Anda: $dailyGoal kkal")
                    .setPositiveButton("Selesai") { _, _ -> finish() }.show()
            }
        }
    }

    private fun calculateAge(birthDateString: String): Int {
        return try {
            val birthDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(birthDateString)!!
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
            if (age < 1) 1 else age
        } catch (e: Exception) { 25 }
    }
}
