

package com.example.chronostimetracker

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.os.Handler
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import java.util.concurrent.TimeUnit
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.firebase.database.*

class ListOfEntries : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var adapter: TimesheetEntryAdapter
    private lateinit var entries: MutableList<TimesheetData>
    private lateinit var camera: Camera
    private lateinit var timerTextView: TextView
    private var timerHandler: Handler? = null
    private var timerRunnable: Runnable? = null
    private var startTime: Long = 0
    private var isTimerRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Chronos Timesheets"

        populateCategorySpinner()

        val button = findViewById<Button>(R.id.btnNext)
        button.setOnClickListener {
            val intent = Intent(this, TimesheetEntry::class.java)
            startActivity(intent)
        }

        // Initialize RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize entries list and adapter
        entries = mutableListOf()
        adapter = TimesheetEntryAdapter(entries)
        recyclerView.adapter = adapter

        // Retrieve entries from Firebase
        retrieveEntriesFromFirebase()

    // Set up the RecyclerView with the adapter
        adapter = TimesheetEntryAdapter(entries)
        recyclerView.adapter = adapter

        // Ensure entries is initialized before accessing it
        if (!entries.isNullOrEmpty()) {
            // Perform operations that require entries to be initialized
        }

        // Initialize the DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navView)

        // Setup the ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        //navigationView.inflateMenu(R.menu.mymenus)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.report -> {
                    // Open Report activity when the Report item is clicked
                    val intent = Intent(this, Report::class.java)
                    startActivity(intent)
                    true
                }
                R.id.login -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    true
                }

                R.id.Timesheet -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, TimesheetEntry::class.java)
                    startActivity(intent)
                    true
                }


                else -> false

            }
        }

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun retrieveEntriesFromFirebase() {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("timesheetEntries")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                entries.clear() // Clear the existing entries
                for (snapshot in dataSnapshot.children) {
                    val entry = snapshot.getValue(TimesheetData::class.java)
                    if (entry != null) {
                        entries.add(entry)
                    }
                }
                adapter.notifyDataSetChanged() // Notify the adapter about data changes
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("ListOfEntries", "Failed to read entries", databaseError.toException())
            }
        })
    }

    private fun populateCategorySpinner() {
        // Reference to the Firebase database
        val database = FirebaseDatabase.getInstance().reference
        val timesheetRef = database.child("timesheetEntries")
        val categoryRef = database.child("CategoryData")

        // Initialize Spinner
        val spinner: Spinner = findViewById(R.id.categorySpinner)

        // Mutable list to hold the filter options
        val mutableFilterOptions = mutableListOf( "None", "Filter by Categories", "Filter by Date Range")

        // Pass the mutable list to the ArrayAdapter
        val spinnerArrayAdapter = ArrayAdapter<String>(this@ListOfEntries, android.R.layout.simple_spinner_item, mutableFilterOptions)
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerArrayAdapter

        spinner.setSelection(0, false)

        // Set the onItemSelectedListener for the Spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                if (selectedOption == "Filter by Categories") {
                    showCategorySelectionDialog()
                } else if (selectedOption == "Filter by Date Range") {
                    // If "Filter by Date Range" is selected, show two DatePickerDialogs
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    // Show the first DatePickerDialog for the start date
                    DatePickerDialog(this@ListOfEntries, { _, year, monthOfYear, dayOfMonth ->
                        // Store the selected start date
                        val startDate = "$dayOfMonth/${monthOfYear + 1}/$year"

                        // Show the second DatePickerDialog for the end date
                        DatePickerDialog(this@ListOfEntries, { _, year, monthOfYear, dayOfMonth ->
                            // Handle the end date selected by the user
                            val endDate = "$dayOfMonth/${monthOfYear + 1}/$year"
                            filterEntriesByDate(startDate, endDate)
                        }, year, month, day).show()
                    }, year, month, day).show()
                } else if (selectedOption == "None") {
                    // Clear all filters and update the UI
                    clearAllFiltersAndUpdateUI()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        // Retrieve all saved dates from Firebase
        timesheetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val dateValues = mutableListOf<String>()
                for (entrySnapshot in dataSnapshot.children) {
                    val startDate = entrySnapshot.child("startDate").getValue(String::class.java)
                    val endDate = entrySnapshot.child("endDate").getValue(String::class.java)
                    startDate?.let { dateValues.add(it) }
                    endDate?.let { dateValues.add(it) }
                }

                // Retrieve all saved categories from Firebase
                categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(categoryDataSnapshot: DataSnapshot) {
                        val categoryNamesStringList = mutableListOf<String>()
                        for (categorySnapshot in categoryDataSnapshot.children) {
                            val categoryName = categorySnapshot.child("category_name").getValue(String::class.java)
                            categoryName?.let { categoryNamesStringList.add(it) }
                        }

                        // Remove duplicates from the list of category names
                        val uniqueCategoryNames = categoryNamesStringList.distinct()

                        // Add categories to the filter options
                        mutableFilterOptions.addAll(uniqueCategoryNames)

                        // Update the ArrayAdapter with the new filter options
                        spinnerArrayAdapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("Firebase", "Failed to retrieve category names", databaseError.toException())
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve timesheet data", databaseError.toException())
            }
        })
    }


    private fun clearAllFiltersAndUpdateUI() {
        filterEntriesByCategory("")
        filterEntriesByDate("", "")
    }


    private fun showCategorySelectionDialog() {
        val database = FirebaseDatabase.getInstance().reference
        val categoryRef = database.child("CategoryData")

        // Retrieve all saved categories from Firebase
        categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(categoryDataSnapshot: DataSnapshot) {
                val categoryNamesStringList = mutableListOf<String>()
                for (categorySnapshot in categoryDataSnapshot.children) {
                    val categoryName = categorySnapshot.getValue(String::class.java)
                    categoryName?.let { categoryNamesStringList.add(it) }
                }

                // Remove duplicates from the list of category names
                val uniqueCategoryNames = categoryNamesStringList.distinct()

                // Convert uniqueCategoryNames to an array
                val items = uniqueCategoryNames.toTypedArray()

                // Build the dialog
                AlertDialog.Builder(this@ListOfEntries)
                    .setTitle("Select a Category")
                    .setSingleChoiceItems(items, -1) { dialog, which ->
                        // The user has selected a category
                        filterEntriesByCategory(uniqueCategoryNames[which])
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve category names", databaseError.toException())
            }
        })
    }




    private fun filterEntriesByCategory(selectedCategory: String?) {
        val filteredEntries = if (selectedCategory.isNullOrEmpty()) {
            entries // Display all entries if no category is selected
        } else {
            entries.filter { entry -> entry.category == selectedCategory }
        }
        adapter.updateData(filteredEntries)
    }

    private fun parseCreationTime(creationTime: Long): Date? {
        return try {
            Date(creationTime)
        } catch (e: IllegalArgumentException) {
            null // Return null if the creationTime cannot be converted to a Date
        }
    }

    private fun parseDate(dateString: String): Date? {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: ParseException) {
            null // Return null if the date string cannot be parsed
        }
    }

    private fun filterEntriesByDate(startDate: String, endDate: String) {
        val database = FirebaseDatabase.getInstance().reference
        val timesheetRef = database.child("timesheetEntries")

        timesheetRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val filteredEntries = mutableListOf<TimesheetData>()

                for (entrySnapshot in dataSnapshot.children) {
                    val entry = entrySnapshot.getValue(TimesheetData::class.java)

                    // Ensure entry is not null and creation time is available
                    if (entry != null && entry.creationTime != null) {
                        val creationTime = entry.creationTime

                        // Convert creation time to Date object
                        val creationDate = Date(creationTime)

                        // Parse start and end date strings to Date objects
                        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        val startDateObj = sdf.parse(startDate)
                        val endDateObj = sdf.parse(endDate)

                        // Check if the creation date falls within the selected range
                        if (creationDate in startDateObj..endDateObj) {
                            // Entry falls within the selected date range, add it to filtered list
                            filteredEntries.add(entry)
                        }
                    }
                }

                // Now you have the filtered entries, you can update the UI or perform further operations
                // For example, you can display the filtered entries in a RecyclerView or ListView
                // Or you can call a function to update the UI with the filtered entries

                adapter.updateData(filteredEntries)
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve timesheet data", databaseError.toException())
            }
        })
    }



    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
