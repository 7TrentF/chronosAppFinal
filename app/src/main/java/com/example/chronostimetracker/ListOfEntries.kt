

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
import android.widget.ImageView
import android.widget.Toast
import androidx.navigation.Navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
    private lateinit var currentUser: FirebaseUser


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }



        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.BottomNavigationView)

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Chronos Timesheets"

        populateCategorySpinner()



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
                R.id.logout -> {
                    // Sign out the current user
                    FirebaseAuth.getInstance().signOut()

                    // Open Login activity
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)

                    // Finish the current activity to prevent the user from going back
                    finish()

                    true
                }


                R.id.Timesheet -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, TimesheetEntry::class.java)
                    startActivity(intent)
                    true
                }

                R.id.SetDailyGoal -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, SetDailyGoalActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false

            }
        }




        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_add -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, TimesheetEntry::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_report -> {
                    // Open Report activity when the Report item is clicked
                    val intent = Intent(this, Report::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onStart() {
        super.onStart()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            checkDailyGoalSet()
        }
    }

    private fun checkDailyGoalSet() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference
            val userEntriesRef = database.child("user_entries").child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            dailyGoalRef.child(currentDate).addListenerForSingleValueEvent(object :
                ValueEventListener {
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
        Toast.makeText(
            this,
            "You have not set a daily goal for today. Please set it now.",
            Toast.LENGTH_LONG
        ).show()
    }


    private fun retrieveEntriesFromFirebase() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance()
            val ref = database.getReference("user_entries").child(user.uid).child("Timesheet Entries")

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
    }







    private fun populateCategorySpinner() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Reference to the Firebase database under the specific user's path
            val database = FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
            val timesheetRef = database.child("Timesheet Entries") // Reference to the timesheetEntries child
            val categoryRef = database.child("categoryData") // Reference to the categoryData child

        // Initialize Spinner
        val spinner: Spinner = findViewById(R.id.categorySpinner)

        // Mutable list to hold the filter options
        val mutableFilterOptions =
            mutableListOf("None", "Filter by Categories", "Filter by Date Range")

        // Pass the mutable list to the ArrayAdapter
        val spinnerArrayAdapter = ArrayAdapter<String>(
            this@ListOfEntries,
            android.R.layout.simple_spinner_item,
            mutableFilterOptions
        )
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = spinnerArrayAdapter

        spinner.setSelection(0, false)

        // Set the onItemSelectedListener for the Spinner
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedOption = parent.getItemAtPosition(position).toString()
                when (selectedOption) {
                    "Filter by Categories" -> {
                        showCategorySelectionDialog()
                    }
                    "Filter by Date Range" -> {

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

                    }
                    "None" -> {
                        // Clear all filters and update the UI
                        adapter.updateData(entries)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }


                // Retrieve all saved categories from Firebase
                categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(categoryDataSnapshot: DataSnapshot) {
                        val categoryNamesStringList = mutableListOf<String>()
                        for (categorySnapshot in categoryDataSnapshot.children) {
                            val categoryName =
                                categorySnapshot.child("category_name").getValue(String::class.java)
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
                            Log.e(
                                "Firebase",
                                "Failed to retrieve category names",
                                databaseError.toException()
                            )
                        }

                })
        }
        }


    private fun showCategorySelectionDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
            val categoryRef = database.child("categoryData") // Reference to the categoryData child

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
    }





    private fun filterEntriesByCategory(selectedCategory: String?) {
        val filteredEntries = if (selectedCategory.isNullOrEmpty()) {
            entries // Display all entries if no category is selected
        } else {
            entries.filter { entry -> entry.category == selectedCategory }
        }
        adapter.updateData(filteredEntries)
    }


    // Function to filter entries by date
    private fun filterEntriesByDate(startDate: String, endDate: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference
            // Assuming 'user' contains the UID of the current user
            val userEntriesRef = database.child("user_entries").child(user.uid).child("Timesheet Entries")

            userEntriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val filteredEntries = mutableListOf<TimesheetData>()
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val startDateObj = sdf.parse(startDate)
                    val endDateObj = sdf.parse(endDate)

                    for (entrySnapshot in dataSnapshot.children) {
                        val entry = entrySnapshot.getValue(TimesheetData::class.java)
                        if (entry?.creationTime!= null) {
                            val creationDate = Date(entry.creationTime)
                            if (creationDate in startDateObj..endDateObj) {
                                filteredEntries.add(entry)
                            }
                        }
                    }
                    adapter.updateData(filteredEntries)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve timesheet data", databaseError.toException())
                }
            })
        }
    }





    private fun parseCreationTime(creationTime: Long): Date? {
        return try {
            Date(creationTime)
        } catch (e: IllegalArgumentException) {
            null // Return null if the creationTime cannot be converted to a Date
        }
    }

    // Function to parse date
    private fun parseDate(dateString: String): Date? {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return try {
            dateFormat.parse(dateString)
        } catch (e: ParseException) {
            null
        }
    }

    /*
    private fun filterEntriesByDate(startDate: String, endDate: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
            val timesheetRef = database.child("Timesheet Entries") // Reference to the "Timesheet Entries" child

            timesheetRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val filteredEntries = mutableListOf<TimesheetData>()
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val startDateObj = sdf.parse(startDate)
                    val endDateObj = sdf.parse(endDate)

                    for (entrySnapshot in dataSnapshot.children) {
                        val entry = entrySnapshot.getValue(TimesheetData::class.java)

                        // Ensure entry is not null and creation time is available
                        if (entry != null && entry.creationTime != null) {
                            val creationDate = Date(entry.creationTime)
                            if (creationDate in startDateObj..endDateObj) {
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
    }
*/



    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
