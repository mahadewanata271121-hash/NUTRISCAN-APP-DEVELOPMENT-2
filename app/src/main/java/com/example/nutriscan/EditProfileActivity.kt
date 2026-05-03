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

        val btnBack = findViewById<ImageButton>(R.id.btn_back_edit_profile)
        val etName = findViewById<TextInputEditText>(R.id.et_edit_name)
        val etEmail = findViewById<TextInputEditText>(R.id.et_edit_email)
        val etHeight = findViewById<TextInputEditText>(R.id.et_edit_height)
        val etWeight = findViewById<TextInputEditText>(R.id.et_edit_weight)
        val etBirthdate = findViewById<TextInputEditText>(R.id.et_edit_birthdate)
        val rgGender = findViewById<RadioGroup>(R.id.rg_edit_gender)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_profile)

        // Load Data User
        if (currentUser != null) {
            etName.setText(currentUser.displayName)
            etEmail.setText(currentUser.email)
            val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
            etHeight.setText(sharedPref.getString("height", ""))
            etWeight.setText(sharedPref.getString("weight", ""))
            etBirthdate.setText(sharedPref.getString("birthdate", ""))
            val savedGender = sharedPref.getString("gender", "")
            if (savedGender == "Laki-laki") findViewById<RadioButton>(R.id.rb_male).isChecked = true
            else if (savedGender == "Perempuan") findViewById<RadioButton>(R.id.rb_female).isChecked = true
        }

        etBirthdate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this, { _, year, month, day ->
                val date = "$day/${month + 1}/$year"
                etBirthdate.setText(date)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val hStr = etHeight.text.toString().trim()
            val wStr = etWeight.text.toString().trim()
            val bDateStr = etBirthdate.text.toString().trim()
            val genderId = rgGender.checkedRadioButtonId

            if (name.isEmpty() || hStr.isEmpty() || wStr.isEmpty() || bDateStr.isEmpty() || genderId == -1) {
                Toast.makeText(this, "Harap lengkapi semua data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gender = findViewById<RadioButton>(genderId).text.toString()
            val height = hStr.toDouble()
            val weight = wStr.toDouble()
            val age = calculateAge(bDateStr)

            // Step 1: Calculate BMR (Mifflin-St Jeor)
            val bmr = if (gender == "Laki-laki") {
                (10 * weight) + (6.25 * height) - (5 * age) + 5
            } else {
                (10 * weight) + (6.25 * height) - (5 * age) - 161
            }

            // Step 2: Calculate TDEE (Using Sedentary 1.2 as default or current system 1.375)
            // Let's use 1.375 as per existing logic, or 1.2 if we strictly follow "sedentary" default
            val activityFactor = 1.375 
            val tdee = (bmr * activityFactor).toInt()

            // Step 3: Goals (Default Goal = TDEE)
            val dailyGoal = tdee
            
            // Step 4: Nutritional Targets (AMDR Standards)
            // Carbs: 45-65% (take 55%), Protein: 10-35% (take 20%)
            val carbsGoal = (dailyGoal * 0.55 / 4).toInt()
            val proteinGoal = (dailyGoal * 0.20 / 4).toInt()

            // Step 5: Boundaries for Charts
            val minCal = (dailyGoal * 0.8).toInt()
            val maxCal = (dailyGoal * 1.2).toInt()

            // Simpan Nama ke Firebase
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val sharedPref = getSharedPreferences("user_data", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("height", hStr)
                        putString("weight", wStr)
                        putString("birthdate", bDateStr)
                        putString("gender", gender)
                        putInt("daily_goal", dailyGoal)
                        putInt("carbs_goal", carbsGoal)
                        putInt("protein_goal", proteinGoal)
                        putInt("min_cal", minCal)
                        putInt("max_cal", maxCal)
                        apply()
                    }
                    
                    showCaloriesResultDialog(dailyGoal)
                }
            }
        }
    }

    private fun calculateAge(birthDateString: String): Int {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val birthDate = sdf.parse(birthDateString) ?: return 0
        val today = Calendar.getInstance()
        val birth = Calendar.getInstance()
        birth.time = birthDate
        var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
        return age
    }

    private fun showCaloriesResultDialog(calories: Int) {
        AlertDialog.Builder(this)
            .setTitle("Profil Diperbarui!")
            .setMessage("Berdasarkan standar Nutriscan, target harian Anda adalah:\n\n$calories kkal")
            .setPositiveButton("Selesai") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }
}
