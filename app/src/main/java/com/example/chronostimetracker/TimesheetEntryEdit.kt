package com.example.chronostimetracker

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Intent
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class TimesheetEntryEdit : AppCompatActivity() {
     lateinit var startDatePicker: DatePicker
      lateinit var endDatePicker: DatePicker
     lateinit var startTimePicker: TimePickerHandler
     lateinit var EndTimePicker: TimePickerHandler
     lateinit var minHours: EditText
     lateinit var maxHours: EditText
     lateinit var etProjectName: EditText
     lateinit var etCategory: EditText
     lateinit var etDescription: EditText
     lateinit var imgUserImage: ImageView
    private lateinit var btnPickImg: Button
     var uniqueId: Int = -1 // Initialize uniqueId with a default value
    private lateinit var camera: Camera

    // Firebase database reference
     lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timesheet_entry_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }


        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference


        // Start and end time buttons
        val startTimeButton: Button = findViewById(R.id.tvStartTime)
        val endTimeButton: Button = findViewById(R.id.tvEndTime)
        // Start and End date buttons
        val startDateButton: Button = findViewById(R.id.tvStartDate)
        val endDateButton: Button = findViewById(R.id.tvEndDate)

        etProjectName = findViewById(R.id.etProjectName)
        etCategory = findViewById(R.id.etCategory)
        etDescription = findViewById(R.id.tvDescription)

        // Initialize TimePickers for buttons
        startTimePicker = TimePickerHandler(this, startTimeButton)
        EndTimePicker = TimePickerHandler(this, endTimeButton)

        // Initialize DatePickers for buttons
        startDatePicker = DatePicker(this, startDateButton)
        endDatePicker = DatePicker(this, endDateButton)

        // Min and Max hours
        maxHours = findViewById(R.id.tvMaxTime)
        minHours = findViewById(R.id.tvMinTime)

        // Set current date as default text for buttons
        val currentDate = getCurrentDate()
        startDateButton.text = currentDate
        endDateButton.text = currentDate

        val hoursValidator = HoursValidator(this)

        /*
        btnCreate.setOnClickListener {
            val startTime = startTimePicker.getTimeAsLocalTime()
            val endTime = EndTimePicker.getTimeAsLocalTime()

            // Validate start and end times
            startTimePicker.validateStartEndTime(startTime, endTime)

            // Validate minHours and maxHours before proceeding
            if (hoursValidator.validateMinMaxHours(
                    minHours.text.toString(),
                    maxHours.text.toString()
                )) {
                SaveCategory()
                saveDataToFirebase()
                // Start TimesheetEntryDisplayActivity and pass the unique ID
                val intent = Intent(this, ListOfEntries::class.java)
                intent.putExtra("uniqueId", uniqueId)
                startActivity(intent)
            }
        }
        */


        camera = Camera(this)
        imgUserImage = findViewById(R.id.userImage)
       // btnPickImg = findViewById(R.id.btnPickImg)

        btnPickImg.setOnClickListener {
            //camera.openCamera(imgUserImage)
            camera.showImagePickerOptions(imgUserImage)
        }

        imgUserImage.setOnClickListener {
            // Get the current image from the ImageView
            val drawable = imgUserImage.drawable
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                // Inflate the dialog layout
                val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview, null)
                val imageView = dialogView.findViewById<ImageView>(R.id.previewImageView)
                imageView.setImageBitmap(bitmap.copy(bitmap.config, true)) // Copy the bitmap without compression
                // Create and show the dialog
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setPositiveButton("Close") { dialog, _ -> dialog.dismiss() }
                    .create()
                dialog.show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.handlePermissionResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        camera.handleActivityResult(requestCode, resultCode, data)
    }
    private fun SaveCategory() {
        val categoryText = etCategory.text.toString()

        // Validate the category input
        if (categoryText.isEmpty()) {
            Toast.makeText(this, "Category cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Initialize Firebase database reference
            val database = FirebaseDatabase.getInstance()
            val categoryRef = database.getReference("user_entries").child(user.uid).child("categoryData")

            // Generate a unique key for the category
            val uniqueCategoryKey = categoryRef.push().key ?: return

            // Save the category to Firebase
            categoryRef.child(uniqueCategoryKey).setValue(categoryText).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Category saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save category", Toast.LENGTH_SHORT).show()
                    Log.e("FirebaseError", "Error saving category: ${task.exception?.message}")
                }
            }
        }
    }

    // Save the changes back to Firebase
    fun saveEditsToFirebase(uniqueKey: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userEntriesRef = database.child("user_entries").child(user.uid)
            val timesheetEntriesRef = userEntriesRef.child("Timesheet Entries")

            // Retrieve input data
            val projectName = etProjectName.text.toString()
            val category = etCategory.text.toString()
            val description = etDescription.text.toString()
            val startTime = startTimePicker.getTime()
            val startDate = startDatePicker.getDate()
            val endTime = EndTimePicker.getTime()
            val endDate = endDatePicker.getDate()
            val minHoursValue = minHours.text.toString().toInt()
            val maxHoursValue = maxHours.text.toString().toInt()
            val creationTime = System.currentTimeMillis()

            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(creationTime))

            val bitmap = (imgUserImage.drawable as BitmapDrawable).bitmap
            val encodedImage = camera.encodeImage(bitmap)

            val entry = TimesheetData(
                uniqueKey, projectName, category, description, startTime, startDate,
                endTime, endDate, minHoursValue, maxHoursValue, encodedImage, creationTime
            )

            timesheetEntriesRef.child(uniqueKey).setValue(entry)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("TimesheetEntry", "Data updated successfully")
                        finish() // Close the activity
                    } else {
                        Log.e("TimesheetEntry", "Failed to update data", task.exception)
                    }
                }
        }
    }


    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-based
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Format the date as "MM/dd"
        val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun openStartTimePicker(view: View) {
        startTimePicker.showTimePickerDialog()
    }

    fun openEndTimePicker(view: View) {
        EndTimePicker.showTimePickerDialog()
    }

    fun openStartDatePicker(view: View) {
        startDatePicker.showDatePicker()
    }

    fun openEndDatePicker(view: View) {
        endDatePicker.showDatePicker()
    }
}