package com.example.chronostimetracker

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class SetDailyGoalActivity : AppCompatActivity() {

    // Firebase reference
    private lateinit var database: DatabaseReference
    private lateinit var minGoal: EditText
    private lateinit var maxGoal: EditText
    private lateinit var saveGoalButton:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_daily_goal)


        val toolbar: Toolbar = findViewById(R.id.Daily_Goal_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Daily Goal" // Set the title here



        minGoal = findViewById(R.id.etMinGoal)
        maxGoal = findViewById(R.id.etMaxGoal)
        saveGoalButton = findViewById(R.id.saveGoalButton)


        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference.child("user_entries")
        checkDailyGoal()
    }

    private fun checkDailyGoal() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dailyGoalRef.child(currentDate).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        showEditDailyGoalLayout()
                    } else {
                        setupSaveButtonForInitialLayout()
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to check daily goal", databaseError.toException())
                }
            })
        }
    }

    private fun showEditDailyGoalLayout() {
        setContentView(R.layout.edit_daily_goal)
        minGoal = findViewById(R.id.etMinGoal)
        maxGoal = findViewById(R.id.etMaxGoal)
        saveGoalButton = findViewById(R.id.btnSave)

        val hoursValidator = HoursValidator(this)
        saveGoalButton.setOnClickListener {
            val minGoalValue = minGoal.text.toString().toIntOrNull()
            val maxGoalValue = maxGoal.text.toString().toIntOrNull()

            if (minGoalValue != null && maxGoalValue != null) {
                if (hoursValidator.validateMinMaxHours(minGoalValue.toString(), maxGoalValue.toString())) {
                    saveDailyGoal(minGoalValue, maxGoalValue)
                    val intent = Intent(this, ListOfEntries::class.java)
                    startActivity(intent)
                }
            }
            else {
                Toast.makeText(this, "Please enter goals for both fields", Toast.LENGTH_SHORT).show()
            }
        }
        loadCurrentGoals()

    }

    private fun setupSaveButtonForInitialLayout() {
        val hoursValidator = HoursValidator(this)
        saveGoalButton.setOnClickListener {
            val minGoalValue = minGoal.text.toString().toIntOrNull()
            val maxGoalValue = maxGoal.text.toString().toIntOrNull()

            if (minGoalValue != null && maxGoalValue != null) {
                if (hoursValidator.validateMinMaxHours(minGoalValue.toString(), maxGoalValue.toString())) {
                    saveDailyGoal(minGoalValue, maxGoalValue)
                    val intent = Intent(this, ListOfEntries::class.java)
                    startActivity(intent)
                }
            }
            else {
                Toast.makeText(this, "Please enter goals for both fields", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun loadCurrentGoals() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dailyGoalRef.child(currentDate).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val minGoal = dataSnapshot.child("minGoal").getValue(Int::class.java) ?: 0
                        val maxGoal = dataSnapshot.child("maxGoal").getValue(Int::class.java) ?: 0

                        findViewById<TextView>(R.id.tvCurrentMin).text = "Min Goal: $minGoal Hour(s)"
                        findViewById<TextView>(R.id.tvCurrentMax).text = "Max Goal: $maxGoal Hour(s)"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to fetch daily goals", databaseError.toException())
                }
            })
        }
    }

    private fun saveGoals(minGoal: Int, maxGoal: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val dailyGoal = DailyGoal(minGoal, maxGoal)

            dailyGoalRef.child(currentDate).setValue(dailyGoal)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Saving Daily Goal: Date: $currentDate, Min Goal: $minGoal hours, Max Goal: $maxGoal hours", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to update goals", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }


    private fun saveDailyGoal(minGoal: Int, maxGoal: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val dailyGoal = DailyGoal(minGoal, maxGoal)

            dailyGoalRef.child(currentDate).setValue(dailyGoal)
                .addOnSuccessListener {
                    Toast.makeText(this, "Daily goal saved", Toast.LENGTH_SHORT).show()
                    Log.d(
                        "Firebase",
                        "Saving Daily Goal: Date: $currentDate, Min Goal: $minGoal hours, Max Goal: $maxGoal hours"
                    )
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save daily goal", Toast.LENGTH_SHORT).show()
                }
        }

    }


        private fun checkDailyGoalSet() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dailyGoalRef.child(currentDate).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        showDailyGoalNotification()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to check daily goal", databaseError.toException())
                }
            })
        }
    }

    private fun showDailyGoalNotification() {
        val toast = Toast.makeText(
            this,
            "You have not set a daily goal for today. Please set it now.",
            Toast.LENGTH_LONG
        )

        val toastLayout = toast.view as LinearLayout?
        val toastImageView = ImageView(this)
        toastImageView.setImageResource(R.drawable.ic_notification) // Set your icon drawable here
        toastLayout?.addView(toastImageView, 0)
        toast.show()
    }


}




