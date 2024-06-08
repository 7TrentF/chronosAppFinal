package com.example.chronostimetracker

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.TimePickerDialog
import android.content.Context
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialog

import android.util.Log
import android.view.View
import android.widget.Button
import android.util.Base64
import android.graphics.BitmapFactory
import android.os.Build
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class TimesheetEdit(private val context: Context, private val database: DatabaseReference) {

    //private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>
    private var currentImageButton: ImageButton? = null
    private lateinit var camera: Camera

    @SuppressLint("SuspiciousIndentation")
    fun showEditDialog(entry: TimesheetData, onSave: (TimesheetData) -> Unit, onDelete: (String) -> Unit) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_timesheet, null)

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
            val newCategory = categoryEditText.text.toString()
            if (entry.category != newCategory) {
                // Category has been changed, show an alert dialog
                AlertDialog.Builder(context)
                    .setTitle("Confirm Category Change")
                    .setMessage("Category will be changed. Do you want to proceed?")
                    .setMessage("Note the time tracked for the new category will be set to 00:00:00 Do you want to proceed?")
                    .setPositiveButton("Yes") { _, _ ->
                        entry.projectName = projectNameEditText.text.toString()
                        entry.category = newCategory
                        entry.description = descriptionEditText.text.toString()

                        // Call the onSave callback to save the edited entry
                        onSave(entry)
                        dialog.dismiss() // Dismiss the BottomSheetDialog
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        // Do nothing, dismiss the dialog
                        dialog.dismiss()
                    }
                    .show()
            } else {
                // Category has not been changed, proceed with saving the entry
                entry.projectName = projectNameEditText.text.toString()
                entry.category = newCategory
                entry.description = descriptionEditText.text.toString()

                // Call the onSave callback to save the edited entry
                onSave(entry)
                dialog.dismiss() // Dismiss the BottomSheetDialog
            }
        }


      val  deleteButton:Button = view.findViewById(R.id.deleteButton)

        // Handle delete button click
        deleteButton.setOnClickListener {
            // Show an alert dialog to confirm deletion
            AlertDialog.Builder(context)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Yes") { _, _ ->
                    // Call the onDelete callback to delete the entry
                    onDelete(entry.uniqueId.toString())
                    dialog.dismiss() // Dismiss the BottomSheetDialog
                }
                .setNegativeButton("No") { dialog, _ ->
                    // Do nothing, dismiss the dialog
                    dialog.dismiss()
                }
                .show()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    fun deleteEntry(entryId: String) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val timesheetEntryRef = database.child("user_entries").child(userId).child("Timesheet Entries").child(entryId)

        timesheetEntryRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {

                Log.d("Firebase", "Timesheet entry deleted successfully.")
            } else {
                Log.w("Firebase", "Failed to delete timesheet entry.", task.exception)
            }
        }
    }

    fun saveEntry(entry: TimesheetData) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid?: return

        // Reference to the Timesheet Entry
        val TimesheetEntryRef = database.child("user_entries").child(userId).child("Timesheet Entries").child(entry.uniqueId.toString())
        TimesheetEntryRef.setValue(entry)

        // Extract the new category from the entry
        val newCategory = entry.category
        Log.d("check", "New Category: $newCategory")

        // Reference to categoryData
        val categoryRef = database.child("user_entries").child(userId).child("categoryData").child(entry.uniqueId.toString())

        // Check if the category already exists
        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Category exists, update it
                    categoryRef.setValue(newCategory)
                } else {

                    categoryRef.setValue(newCategory)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("Firebase", "Failed to read category data.", databaseError.toException())
            }
        })
    }

    // Function to create and display a notification


    private fun showTimePickerDialog(view: View, entry: TimesheetData, timeType: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(context, { _, selectedHour, selectedMinute ->
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

        val datePickerDialog = DatePickerDialog(context, { _, selectedYear, selectedMonth, selectedDay ->
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



}
