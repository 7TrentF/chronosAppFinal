package com.example.chronostimetracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class TimesheetEntryEdit : AppCompatActivity() {

     lateinit var userImage: ImageButton

    private lateinit var camera: Camera

    // Firebase database reference
    // lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_timesheet_entry_edit)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

        }
         userImage= findViewById(R.id.userImage)

        // Initialize Firebase
       // database = FirebaseDatabase.getInstance().reference

        camera = Camera(this)


        userImage.setOnClickListener {
            camera.showImagePickerOptions(userImage)


            val drawable = userImage.drawable
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

    fun showEditDialog(entry: TimesheetData, onSave: (TimesheetData) -> Unit, onDelete: (String) -> Unit) {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_timesheet, null)

        val projectNameEditText: EditText = view.findViewById(R.id.tvProjectName)
        val categoryEditText: EditText = view.findViewById(R.id.tvCategory)
        val startTimeButton: Button = view.findViewById(R.id.tvStartTime)
        val endTimeButton: Button = view.findViewById(R.id.tvEndTime)
        val startDateButton: Button = view.findViewById(R.id.tvStartDate)
        val endDateButton: Button = view.findViewById(R.id.tvEndDate)
        val descriptionEditText: EditText = view.findViewById(R.id.tvDescription)
        val userImage: ImageButton = view.findViewById(R.id.userImage)

        // Set the existing values
        projectNameEditText.setText(entry.projectName)
        categoryEditText.setText(entry.category)
        descriptionEditText.setText(entry.description)
        startTimeButton.text = entry.startTime
        endTimeButton.text = entry.endTime
        startDateButton.text = entry.startDate
        endDateButton.text = entry.endDate

        if (entry.imageData != null) {
            val decodedString = Base64.decode(entry.imageData, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            userImage.setImageBitmap(decodedBitmap)
        } else {
            userImage.setImageResource(R.drawable.default_image)
        }

        // Show camera when image button is clicked


        // Show time picker dialog when the button is clicked
        startTimeButton.setOnClickListener {
            showTimePickerDialog(it, entry, "start")
        }

        endTimeButton.setOnClickListener {
            showTimePickerDialog(it, entry, "end")
        }

        // Show date picker dialog when the button is clicked
        startDateButton.setOnClickListener {
            showDatePickerDialog(it, entry, "start")
        }

        endDateButton.setOnClickListener {
            showDatePickerDialog(it, entry, "end")
        }

        // Handle save button click
        val saveButton: Button = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            // Update the entry with edited values
            entry.projectName = projectNameEditText.text.toString()
            entry.category = categoryEditText.text.toString()
            entry.description = descriptionEditText.text.toString()

            // Call the onSave callback to save the edited entry
            onSave(entry)
            dialog.dismiss()
        }

        val  deleteButton:Button = view.findViewById(R.id.deleteButton)

        // Handle delete button click
        deleteButton.setOnClickListener {
            onDelete(entry.uniqueId.toString())
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.show()
    }


    private fun showTimePickerDialog(view: View, entry: TimesheetData, timeType: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

            when (timeType) {
                "start" -> {
                    (view as Button).text = selectedTime
                    entry.startTime = selectedTime
                }
                "end" -> {
                    (view as Button).text = selectedTime
                    entry.endTime = selectedTime
                }
            }
        }, hour, minute, true)

        timePickerDialog.show()
    }

    private fun showDatePickerDialog(view: View, entry: TimesheetData, dateType: String) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }

            // Format date as "Month Day, Year"
            val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(selectedDate.time)

            when (dateType) {
                "start" -> {
                    (view as Button).text = formattedDate
                    entry.startDate = formattedDate
                }
                "end" -> {
                    (view as Button).text = formattedDate
                    entry.endDate = formattedDate
                }
            }
        }, year, month, day)

        datePickerDialog.show()
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.handlePermissionResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        camera.handleActivityResult(requestCode, resultCode, data)
    }



}