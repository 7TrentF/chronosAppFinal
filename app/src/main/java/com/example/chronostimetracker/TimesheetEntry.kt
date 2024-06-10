package com.example.chronostimetracker

import android.app.Dialog
import android.content.Context
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
import android.graphics.drawable.VectorDrawable
import android.net.Uri
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*


class TimesheetEntry : AppCompatActivity() {

    private lateinit var startDatePicker: DatePicker
    private lateinit var endDatePicker: DatePicker
    private lateinit var startTimePicker: TimePickerHandler
    private lateinit var EndTimePicker: TimePickerHandler
    private lateinit var etProjectName: EditText
    private lateinit var etCategory: EditText
    private lateinit var etDescription: EditText
    private lateinit var imgUserImage: ImageButton
    private lateinit var btnPickImg: Button
    private var uniqueId: Int = -1 // Initialize uniqueId with a default value
    private lateinit var camera: Camera
    private lateinit var database: DatabaseReference  // Firebase database reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timesheet_entry)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Create Timesheet Entry"

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.BottomNavigationView)
        // Create button
        val btnCreate: Button = findViewById(R.id.btnCreate)
        // Start and end time buttons
        val startTimeButton: Button = findViewById(R.id.btnStartTime)
        val endTimeButton: Button = findViewById(R.id.btnEndTime)
        // Start and End date buttons
        val startDateButton: Button = findViewById(R.id.btnStartDate)
        val endDateButton: Button = findViewById(R.id.btnEndDate)

        etProjectName = findViewById(R.id.etProjectName)
        etCategory = findViewById(R.id.etCategory)
        etDescription = findViewById(R.id.etDescription)

        // Initialize TimePickers for buttons
        startTimePicker = TimePickerHandler(this, startTimeButton)
        EndTimePicker = TimePickerHandler(this, endTimeButton)

        // Initialize DatePickers for buttons
        startDatePicker = DatePicker(this, startDateButton)
        endDatePicker = DatePicker(this, endDateButton)


        // Set current date as default text for buttons
        val currentDate = getCurrentDate()
        startDateButton.text = currentDate
        endDateButton.text = currentDate

        val hoursValidator = HoursValidator(this)

        btnCreate.setOnClickListener {


            // Check if all fields are filled
            if (!validateFields()) {
                // Some fields are empty, show an error message if needed
                return@setOnClickListener
            }

            // All fields are filled and validated, proceed with saving data
            SaveCategory()
            saveDataToFirebase()

            // Start ListOfEntries activity
            val intent = Intent(this, ListOfEntries::class.java)
            startActivity(intent)
        }


        camera = Camera(this)
        imgUserImage = findViewById(R.id.btnAddImage)

        // Set up the click listener for the ImageButton
        imgUserImage.setOnClickListener {
            // Trigger the camera to open and set the image
            camera.showImagePickerOptions(imgUserImage)


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

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, ListOfEntries::class.java)
                    startActivity(intent)
                    true
                }

                R.id.action_report -> {
                    // Open Report activity when the Report item is clicked
                    val intent = Intent(this, Report::class.java)
                    startActivity(intent)
                    true
                }
                R.id.pomodoro -> {
                    // Open Pomodoro dialog when the Pomodoro item is clicked
                    val pomodoroDialog = Dialog(this)
                    pomodoroDialog.setContentView(R.layout.dialog_pomodoro_timer)
                    val pomodoroTimer = PomodoroTimer(this)
                    pomodoroTimer.setupDialog(pomodoroDialog)
                    pomodoroDialog.show()
                    true
                }
                else -> false
            }
        }
    }

    private fun validateFields(): Boolean {
        val projectName = etProjectName.text.toString()
        val category = etCategory.text.toString()
        val description = etDescription.text.toString()
        val startTime = startTimePicker.getTime()
        val startDate = startDatePicker.getDate()
        val endTime = EndTimePicker.getTime()
        val endDate = endDatePicker.getDate()

        // Check if any required field is empty
        if (projectName.isEmpty() || category.isEmpty() || description.isEmpty() ||
            startTime.isEmpty() || startDate.isEmpty() || endTime.isEmpty() || endDate.isEmpty()) {
            // Show an alert dialog indicating that all fields must be filled
            AlertDialog.Builder(this@TimesheetEntry)
                .setTitle("Missing Information")
                .setMessage("Please enter all fields.")
                .setPositiveButton("OK") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
                .show()
            return false
        }

        // Check if an image is set
        if (!isImageSet(imgUserImage)) {
            // Show an alert dialog indicating that an image must be selected
            AlertDialog.Builder(this)
                .setTitle("Missing Image")
                .setMessage("Please select an image.")
                .setPositiveButton("OK") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
                .show()
            return false
        }
        return true
    }

    // Method to check if an image is set on the ImageButton
    private fun isImageSet(imageButton: ImageButton): Boolean {
        val drawable = imageButton.drawable
        return drawable is BitmapDrawable
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

    private fun saveDataToFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Get a reference to the user's entry path
            val userEntriesRef = database.child("user_entries").child(user.uid)

            // Create a reference to the Timesheet Entries child under the user's path
            val timesheetEntriesRef = userEntriesRef.child("Timesheet Entries")

            // Generate a unique key for the timesheet entry
            val uniqueKey = timesheetEntriesRef.push().key ?: return

            // Retrieve input data
            val projectName = etProjectName.text.toString()
            val category = etCategory.text.toString()
            val description = etDescription.text.toString()
            val startTime = startTimePicker.getTime()
            val startDate = startDatePicker.getDate()
            val endTime = EndTimePicker.getTime()
            val endDate = endDatePicker.getDate()
            // val minHoursValue = minHours.text.toString().toInt()
            // val maxHoursValue = maxHours.text.toString().toInt()
            val creationTime = System.currentTimeMillis()
            val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
            val formattedDate = sdf.format(Date(creationTime))

            // Retrieve and encode image if available
            val bitmap = (imgUserImage.drawable as BitmapDrawable).bitmap
            val encodedImage = camera.encodeImage(bitmap)

            // Create a TimesheetData object
            val entry = TimesheetData(
                uniqueKey, projectName, category, description, startTime, startDate,
                endTime, endDate, encodedImage, creationTime
            )

            // Save the entry under the Timesheet Entries child
            timesheetEntriesRef.child(uniqueKey).setValue(entry)
                .addOnCompleteListener(OnCompleteListener<Void> { task ->
                    if (task.isSuccessful) {
                        Log.d("TimesheetEntry", "Data saved successfully")
                    } else {
                        Log.e("TimesheetEntry", "Failed to save data", task.exception)
                    }
                })

            // Log the data for debugging purposes
            Log.d("TimesheetEntry", "Project Name: $projectName")
            Log.d("TimesheetEntry", "Category: $category")
            Log.d("TimesheetEntry", "Description: $description")
            Log.d("TimesheetEntry", "Start Time: $startTime")
            Log.d("TimesheetEntry", "Start Date: $startDate")
            Log.d("TimesheetEntry", "End Time: $endTime")
            Log.d("TimesheetEntry", "End Date: $endDate")
            Log.d("TimesheetEntry", "Image: $encodedImage")
            Log.d("TimesheetEntry", "Creation Time: $formattedDate")
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

    fun onAddImageClick(view: View) {
        camera.showImagePickerOptions(imgUserImage)
    }





}


