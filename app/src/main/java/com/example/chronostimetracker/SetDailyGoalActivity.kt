package com.example.chronostimetracker

import android.Manifest
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

    private lateinit var minGoalEditText: EditText
    private lateinit var maxGoalEditText: EditText
    private lateinit var saveGoalButton: Button
    // Firebase reference
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_daily_goal)

        minGoalEditText = findViewById(R.id.etMinGoalEditText)
        maxGoalEditText = findViewById(R.id.etMaxGoalEditText)
        saveGoalButton = findViewById(R.id.saveGoalButton)

        saveGoalButton.setOnClickListener {
            saveDailyGoal()
        }

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference.child("user_entries")

        checkDailyGoal()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            checkDailyGoalSet()
        }
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
                        setupSaveButton()
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
        setupSaveButton()
        loadCurrentGoals()
    }

    private fun setupSaveButton() {
        val btnSave = findViewById<Button>(R.id.btnSave)
        btnSave.setOnClickListener {
            val etMinGoal = findViewById<EditText>(R.id.etMinGoal)
            val etMaxGoal = findViewById<EditText>(R.id.etMaxGoal)

            val minGoal = etMinGoal.text.toString().toLongOrNull()
            val maxGoal = etMaxGoal.text.toString().toLongOrNull()

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
                        val minGoal = dataSnapshot.child("minGoal").getValue(Long::class.java)
                        val maxGoal = dataSnapshot.child("maxGoal").getValue(Long::class.java)

                        findViewById<TextView>(R.id.tvCurrentMin).text = "Min Goal: $minGoal"
                        findViewById<TextView>(R.id.tvCurrentMax).text = "Max Goal: $maxGoal"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to fetch daily goals", databaseError.toException())
                }
            })
        }
    }

    private fun saveGoals(minGoal: Long, maxGoal: Long) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val dailyGoal = DailyGoal(currentDate, minGoal, maxGoal)

            dailyGoalRef.child(currentDate).setValue(dailyGoal)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Goals updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to update goals", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveDailyGoal() {
        val minGoal = minGoalEditText.text.toString().toLongOrNull()
        val maxGoal = maxGoalEditText.text.toString().toLongOrNull()

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




