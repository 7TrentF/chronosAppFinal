package com.example.chronostimetracker

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class SetDailyGoalActivity : AppCompatActivity() {

    private lateinit var minGoalButton: Button
    private lateinit var maxGoalButton: Button
    private lateinit var saveGoalButton: Button


    // Firebase reference
    private lateinit var database: DatabaseReference
    private var minGoal: String? = null
    private var maxGoal: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_daily_goal)


        val toolbar: Toolbar = findViewById(R.id.Daily_Goal_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Daily Goal" // Set the title here

        minGoalButton = findViewById(R.id.btnMinGoal)
        maxGoalButton = findViewById(R.id.btnMaxGoal)
        saveGoalButton = findViewById(R.id.saveGoalButton)

        minGoalButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                minGoal = String.format("%02d:%02d", hour, minute)
                minGoalButton.text = "Min Goal: $minGoal"
            }
        }

        maxGoalButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                maxGoal = String.format("%02d:%02d", hour, minute)
                maxGoalButton.text = "Max Goal: $maxGoal"
            }
        }

        saveGoalButton.setOnClickListener {
            saveDailyGoal()
        }

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference.child("user_entries")

        checkDailyGoal()
    }



    private fun showTimePickerDialog(onTimeSet: (hour: Int, minute: Int) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            onTimeSet(selectedHour, selectedMinute)
        }, hour, minute, true).show()
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
        minGoalButton = findViewById(R.id.btnMinGoal)
        maxGoalButton = findViewById(R.id.btnMaxGoal)
        saveGoalButton = findViewById(R.id.btnSave)

        minGoalButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                minGoal = String.format("%02d:%02d", hour, minute)
                minGoalButton.text = "Min Goal: $minGoal"
            }
        }

        maxGoalButton.setOnClickListener {
            showTimePickerDialog { hour, minute ->
                maxGoal = String.format("%02d:%02d", hour, minute)
                maxGoalButton.text = "Max Goal: $maxGoal"
            }
        }

        saveGoalButton.setOnClickListener {
            val minGoal = this.minGoal
            val maxGoal = this.maxGoal

            if (minGoal != null && maxGoal != null) {
                saveGoals(minGoal, maxGoal)
            } else {
                Toast.makeText(this, "Please enter valid goals", Toast.LENGTH_SHORT).show()
            }
        }
        loadCurrentGoals()
    }


    private fun setupSaveButtonForInitialLayout() {
        saveGoalButton.setOnClickListener {
            val minGoal = this.minGoal
            val maxGoal = this.maxGoal

            if (minGoal != null && maxGoal != null) {
                saveGoals(minGoal, maxGoal)
            } else {
                Toast.makeText(this, "Please enter valid goals", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, ListOfEntries::class.java)
            startActivity(intent)
        }
    }


    private fun setupSaveButtonForEditLayout() {
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener {
            val minGoal = this.minGoal
            val maxGoal = this.maxGoal

            if (minGoal != null && maxGoal != null) {
                saveGoals(minGoal, maxGoal)
            } else {
                Toast.makeText(this, "Please enter valid goals", Toast.LENGTH_SHORT).show()
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
                        val minGoal = dataSnapshot.child("minGoal").getValue(String::class.java)
                        val maxGoal = dataSnapshot.child("maxGoal").getValue(String::class.java)

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

    private fun saveGoals(minGoal: String, maxGoal: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val dailyGoal = DailyGoal(currentDate, minGoal, maxGoal)

            dailyGoalRef.child(currentDate).setValue(dailyGoal)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Saving Daily Goal: Date: $currentDate, Min Goal: $minGoal, Max Goal: $maxGoal", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Failed to update goals", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveDailyGoal() {
        val minGoal = this.minGoal
        val maxGoal = this.maxGoal

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid goals", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val dailyGoal = DailyGoal(currentDate, minGoal, maxGoal)


            dailyGoalRef.child(currentDate).setValue(dailyGoal)
                .addOnSuccessListener {
                    Toast.makeText(this, "Daily goal saved", Toast.LENGTH_SHORT).show()
                    Log.d("Firebase", "Saving Daily Goal: Date: $currentDate, Min Goal: $minGoal, Max Goal: $maxGoal")
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




