package com.example.chronostimetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView


class ListOfEntries : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val button = findViewById<Button>(R.id.btnNext)
        button.setOnClickListener {
            val intent = Intent(this, TimesheetEntry::class.java)
            startActivity(intent)
        }


        // Initialize RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Retrieve all entries from SharedPreferences
        val sharedPreferences = getSharedPreferences("TimesheetData", MODE_PRIVATE)
        val entries = mutableListOf<TimesheetData>()
        val allKeys = sharedPreferences.all.keys
        for (key in allKeys) {
            if (key.startsWith("projectName_")) {
                val uniqueIdString = key.substringAfter("projectName_")
                val uniqueId = uniqueIdString.toLongOrNull() ?: 0 // Convert String to Long, default to 0 if conversion fails
                val projectName = sharedPreferences.getString("projectName_$uniqueIdString", "") ?: ""
                val category = sharedPreferences.getString("category_$uniqueIdString", "") ?: ""
                val description = sharedPreferences.getString("description_$uniqueIdString", "") ?: ""
                val startTime = sharedPreferences.getString("startTime_$uniqueIdString", "") ?: ""
                val startDate = sharedPreferences.getString("startDate_$uniqueIdString", "") ?: ""
                val endTime = sharedPreferences.getString("endTime_$uniqueIdString", "") ?: ""
                val endDate = sharedPreferences.getString("endDate_$uniqueIdString", "") ?: ""
                val minHours = sharedPreferences.getInt("minHours_$uniqueIdString", 0)
                val maxHours = sharedPreferences.getInt("maxHours_$uniqueIdString", 0)

                entries.add(TimesheetData(uniqueId, projectName, category, description, startTime, startDate, endTime, endDate, minHours, maxHours))
            }
        }


        // Set up the RecyclerView with the adapter
        val adapter = TimesheetEntryAdapter(entries)
        recyclerView.adapter = adapter


        // Initialize the DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navView)

        // Setup the ActionBarDrawerToggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        val navigationView: NavigationView = findViewById(R.id.navView)
        navigationView.inflateMenu(R.menu.mymenus)


        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Setup navigation item selected listener
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // Handle navigation view item clicks here
            when (menuItem.itemId) {
                // Add your menu item IDs here
                R.id.nav_item_1 -> {
                    // Handle item click
                }
                // Add more items as needed
            }
            // Close the drawer when an item is tapped
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }



    }




