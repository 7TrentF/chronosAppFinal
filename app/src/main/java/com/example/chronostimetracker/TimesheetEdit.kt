package com.example.chronostimetracker

import android.content.Context
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialog

import android.util.Log
import android.view.View
import android.widget.Button
import android.util.Base64
import android.widget.ImageView
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class TimesheetEdit(private val context: Context, private val database: DatabaseReference) {
    private lateinit var startDatePicker: DatePicker
    private lateinit var endDatePicker: DatePicker
    private lateinit var startTimePicker: TimePickerHandler
    private lateinit var EndTimePicker: TimePickerHandler



    fun showEditDialog(entry: TimesheetData, onSave: (TimesheetData) -> Unit) {
        val dialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_timesheet, null)





        val projectNameEditText: EditText = view.findViewById(R.id.tvProjectName)
        val categoryEditText: EditText = view.findViewById(R.id.tvCategory)
        val startTimeButton: Button = view.findViewById(R.id.tvStartTime)
        val endTimeButton: Button = view.findViewById(R.id.tvEndTime)
        val startDateButton: Button = view.findViewById(R.id.tvStartDate)
        val endDateButton: Button = view.findViewById(R.id.tvEndDate)
        val descriptionEditText: EditText = view.findViewById(R.id.tvDescription)
        val minTimeEditText: EditText = view.findViewById(R.id.tvMinTime)
        val maxTimeEditText: EditText = view.findViewById(R.id.tvMaxTime)
        val userImageView: ImageView = view.findViewById(R.id.userImage)

/*
        // Initialize TimePickers for buttons
        startTimePicker = TimePickerHandler(this, startTimeButton)
        EndTimePicker = TimePickerHandler(this, endTimeButton)

        // Initialize DatePickers for buttons
        startDatePicker = DatePicker(this, startDateButton)
        endDatePicker = DatePicker(this, endDateButton)
*/
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
            userImageView.setImageBitmap(decodedBitmap)
        } else {
            userImageView.setImageResource(R.drawable.default_image)
        }

        // Handle save button click
        val saveButton: Button = view.findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            // Update the entry with edited values
            entry.projectName = projectNameEditText.text.toString()
            entry.category = categoryEditText.text.toString()
            entry.description = descriptionEditText.text.toString()
            entry.startTime =  startTimePicker.getTime()


            // Call the onSave callback to save the edited entry
            onSave(entry)
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
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
                    // Category does not exist, create a new one
                    // Assuming you want to keep the existing structure and just add a new entry
                    // If you want to replace the existing structure with the new category, remove the else block
                    categoryRef.setValue(newCategory)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("Firebase", "Failed to read category data.", databaseError.toException())
            }
        })
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
