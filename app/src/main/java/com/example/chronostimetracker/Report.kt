package com.example.chronostimetracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Report : AppCompatActivity() {
    lateinit var pieChart: PieChart
    private val viewsToRemove = mutableListOf<View>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryTimeAdapter
    private lateinit var clearButton: ImageButton
    private lateinit var viewContainer: ConstraintLayout

    private lateinit var CategoryDisplayButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)


        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Reports" // Set the title here

        pieChart = findViewById(R.id.pieChart)
        // Configure the pie chart
        pieChart.setUsePercentValues(true)
        pieChart.getDescription().setEnabled(false)
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.setDragDecelerationFrictionCoef(0.95f)
        pieChart.setDrawHoleEnabled(true)
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.setHoleRadius(58f)
        pieChart.setTransparentCircleRadius(61f)
        pieChart.setDrawCenterText(true)
        pieChart.setRotationAngle(0f)
        pieChart.setRotationEnabled(true)
        pieChart.setHighlightPerTapEnabled(true)
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)


        val categoryContainer = findViewById<FrameLayout>(R.id.category_container)
        val pieChartContainer = findViewById<FrameLayout>(R.id.pie_chart_container)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.BottomNavigationView)
        val pieChartView = findViewById<PieChart>(R.id.pieChart)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView)
        viewContainer = findViewById(R.id.viewContainer)
        clearButton = findViewById(R.id.clearButton)

        CategoryDisplayButton = findViewById(R.id.CategoryDisplayButton)

        clearButton.setOnClickListener {
            viewContainer.visibility = View.GONE
        }

        CategoryDisplayButton.setOnClickListener {
            viewContainer.visibility = View.VISIBLE
        }



        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, ListOfEntries::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_add -> {
                    // Open Login activity when the Login item is clicked
                    val intent = Intent(this, TimesheetEntry::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }


        val currentUser = FirebaseAuth.getInstance().currentUser
        // Check if the user is authenticated
        currentUser?.let { user ->


            // Retrieve total times for each category from Firebase
            val database =
                FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
            val categoryTimesRef = database.child("CategoryTimes")
            categoryTimesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val categories = mutableMapOf<String, Long>()
                    for (categorySnapshot in dataSnapshot.children) {
                        val category = categorySnapshot.key ?: ""
                        val totalTime =
                            categorySnapshot.child("totalTime").getValue(Long::class.java) ?: 0
                        categories[category] = totalTime
                    }

                    // Create PieEntries based on the total times for each category
                    val entries: ArrayList<PieEntry> = ArrayList()
                    for ((category, totalTime) in categories) {
                        entries.add(
                            PieEntry(
                                totalTime.toFloat(),
                                category
                            )
                        ) // Use the total time as the value
                    }

// Convert the data to a list of CategoryTime
                    val categoryTimeList = categories.map { (category, totalTime) ->
                        val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) % 60
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(totalTime) % 60
                        val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                        CategoryTime(category, formattedTime)
                    }

                    adapter = CategoryTimeAdapter(categoryTimeList)
                    recyclerView.adapter = adapter


                    // Assign colors to each category
                    val colors: ArrayList<Int> = ArrayList()
                    val predefinedColors = listOf(
                        resources.getColor(R.color.blue),
                        resources.getColor(R.color.yellow),
                        resources.getColor(R.color.red),
                        resources.getColor(R.color.lightGreen),
                        resources.getColor(R.color.purple_200),
                        resources.getColor(R.color.veryLightYellow)


                    )
                    var colorIndex = predefinedColors.size
                    for ((_, _) in categories) { // Iterate over categories to assign colors
                        if (colorIndex >= predefinedColors.size) {
                            colorIndex = 0
                        }
                        colors.add(predefinedColors[colorIndex])
                        colorIndex++
                    }

                    // Update the pie chart with the new entries
                    val dataSet = PieDataSet(entries, "Categories")
                    dataSet.setDrawIcons(false)
                    dataSet.sliceSpace = 3f
                    dataSet.iconsOffset = MPPointF(0f, 40f)
                    dataSet.selectionShift = 5f
                    dataSet.colors = colors // Assign the colors to the dataset

                    val data = PieData(dataSet)
                    data.setValueFormatter(PercentFormatter())
                    data.setValueTextSize(15f)
                    data.setValueTypeface(Typeface.DEFAULT_BOLD)
                    data.setValueTextColor(Color.WHITE)
                    pieChart.data = data
                    pieChart.highlightValues(null)
                    pieChart.invalidate()


                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(
                        "Firebase",
                        "Failed to retrieve category times",
                        databaseError.toException()
                    )
                }
            })

// Assuming totalTimeTextView is the TextView in which you want to display the total time tracked
            val totalTimeTextView = findViewById<TextView>(R.id.totalTimeTextView)

// Get a reference to the user's totalTimeTracked path

            // Get a reference to the user's totalTimeTracked path
            val totalTimeTrackedRef = database.child("totalTimeTracked")

            // Get the current date in the format "yyyy-MM-dd"
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            // Retrieve the total time tracked for the current date
            val totalTimeRef = totalTimeTrackedRef.child(currentDate).child("Time")
            totalTimeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val totalMilliseconds = dataSnapshot.getValue(Long::class.java) ?: 0
                    Log.d("Totaltimz", "Total Time Tracked: $totalMilliseconds milliseconds")

                    // Convert total time from milliseconds to hours, minutes, and seconds
                    val totalHours = totalMilliseconds / (1000 * 60 * 60)
                    val totalMinutes = (totalMilliseconds % (1000 * 60 * 60)) / (1000 * 60)
                    val totalSeconds = (totalMilliseconds % (1000 * 60)) / 1000

                    // Format the total time as a string
                    val formattedTotalTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds)

                    // Set the formatted total time to the TextView
                    totalTimeTextView.text = "Total Time Tracked today: $formattedTotalTime"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve totalTimeTracked", databaseError.toException())
                }
            })
        }
    }



    private fun createStyledTextView(context: Context, text: String, topPadding: Int): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = 16f

            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(30, topPadding, 30, 0)
            }

            setPadding(0, topPadding, 0, 0)
        }
    }

    // Usage example
    fun addCategoryTextViews(categoryContainer: ViewGroup, categories: List<String>, formattedTimes: List<String>) {
        var topPadding = 40
        categories.zip(formattedTimes).forEach { (category, formattedTime) ->
            val textView = createStyledTextView(categoryContainer.context, "Category: $category $formattedTime", topPadding)
            categoryContainer.addView(textView)
            topPadding += 40
        }
    }





}