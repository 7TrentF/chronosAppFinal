package com.example.chronostimetracker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class Report : AppCompatActivity() {
    lateinit var pieChart: PieChart
    private val viewsToRemove = mutableListOf<View>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CategoryTimeAdapter
    private lateinit var clearButton: ImageButton
    private lateinit var viewContainer: ConstraintLayout
    private lateinit var projectView: ConstraintLayout

    private lateinit var CategoryDisplayButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Reports"

        pieChart = findViewById(R.id.pieChart)
        configurePieChart()
        lineChart()
        val categoryContainer = findViewById<FrameLayout>(R.id.category_container)
        val pieChartContainer = findViewById<FrameLayout>(R.id.pie_chart_container)
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.BottomNavigationView)
        val btnProject: Button = findViewById(R.id.btnProject)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize views
        viewContainer = findViewById(R.id.viewContainer)
        projectView = findViewById(R.id.projectContainer)
        clearButton = findViewById(R.id.clearButton)
        CategoryDisplayButton = findViewById(R.id.CategoryDisplayButton)

        clearButton.setOnClickListener {
            viewContainer.visibility = View.GONE
        }

        CategoryDisplayButton.setOnClickListener {
            projectView.visibility = View.GONE
            viewContainer.visibility = View.VISIBLE
        }

        btnProject.setOnClickListener {
            viewContainer.visibility = View.GONE

            projectView.visibility = View.VISIBLE
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    val intent = Intent(this, ListOfEntries::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_add -> {
                    val intent = Intent(this, TimesheetEntry::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
            retrieveCategoryTimes(database)
            displayTotalTimeTracked(database)
        }
    }

    private fun configurePieChart() {
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.setExtraOffsets(5f, 10f, 5f, 5f)
        pieChart.dragDecelerationFrictionCoef = 0.95f
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.setTransparentCircleColor(Color.WHITE)
        pieChart.setTransparentCircleAlpha(110)
        pieChart.holeRadius = 58f
        pieChart.transparentCircleRadius = 61f
        pieChart.setDrawCenterText(true)
        pieChart.rotationAngle = 0f
        pieChart.isRotationEnabled = true
        pieChart.setHighlightPerTapEnabled(true)
        pieChart.animateY(1400, Easing.EaseInOutQuad)
        pieChart.legend.isEnabled = false
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(12f)
    }

    private fun retrieveCategoryTimes(database: DatabaseReference) {
        val categoryTimesRef = database.child("CategoryTimes")
        categoryTimesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val categories = mutableMapOf<String, Long>()
                for (categorySnapshot in dataSnapshot.children) {
                    val category = categorySnapshot.key ?: ""
                    val totalTime = categorySnapshot.child("totalTime").getValue(Long::class.java) ?: 0
                    categories[category] = totalTime
                }

                updatePieChart(categories)
                displayCategoryTimes(categories)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve category times", databaseError.toException())
            }
        })
    }

    private fun updatePieChart(categories: Map<String, Long>) {
        val entries = ArrayList<PieEntry>()
        for ((category, totalTime) in categories) {
            entries.add(PieEntry(totalTime.toFloat(), category))
        }

        val colors = ArrayList<Int>()
        val predefinedColors = listOf(
            resources.getColor(R.color.blue),
            resources.getColor(R.color.yellow),
            resources.getColor(R.color.red),
            resources.getColor(R.color.lightGreen),
            resources.getColor(R.color.purple_200),
            resources.getColor(R.color.veryLightYellow)
        )
        var colorIndex = 0
        for (i in categories.keys.indices) {
            colors.add(predefinedColors[colorIndex])
            colorIndex = (colorIndex + 1) % predefinedColors.size
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.setDrawIcons(false)
        dataSet.sliceSpace = 3f
        dataSet.iconsOffset = MPPointF(0f, 40f)
        dataSet.selectionShift = 5f
        dataSet.colors = colors

        val data = PieData(dataSet)
        data.setValueFormatter(PercentFormatter())
        data.setValueTextSize(15f)
        data.setValueTypeface(Typeface.DEFAULT_BOLD)
        data.setValueTextColor(Color.WHITE)
        pieChart.data = data
        pieChart.highlightValues(null)
        pieChart.invalidate()
    }

    private fun displayCategoryTimes(categories: Map<String, Long>) {
        val categoryTimeList = categories.map { (category, totalTime) ->
            val hours = TimeUnit.MILLISECONDS.toHours(totalTime)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalTime) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(totalTime) % 60
            val formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            CategoryTime(category, formattedTime)
        }

        adapter = CategoryTimeAdapter(categoryTimeList)
        recyclerView.adapter = adapter
    }

    private fun displayTotalTimeTracked(database: DatabaseReference) {
        val totalTimeTextView = findViewById<TextView>(R.id.totalTimeTextView)
        val totalTimeTrackedRef = database.child("totalTimeTracked")

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val totalTimeRef = totalTimeTrackedRef.child(currentDate).child("Time")
        totalTimeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val totalMilliseconds = dataSnapshot.getValue(Long::class.java) ?: 0
                Log.d("Totaltime", "Total Time Tracked: $totalMilliseconds milliseconds")

                val totalHours = totalMilliseconds / (1000 * 60 * 60)
                val totalMinutes = (totalMilliseconds % (1000 * 60 * 60)) / (1000 * 60)
                val totalSeconds = (totalMilliseconds % (1000 * 60)) / 1000

                val formattedTotalTime = String.format(Locale.getDefault(), "%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds)
                totalTimeTextView.text = "Total Time Tracked today: $formattedTotalTime"
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Failed to retrieve totalTimeTracked", databaseError.toException())
            }
        })
    }


    private fun lineChart() {
        val database = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            val userEntriesRef = database.child("user_entries").child(user.uid)
            val dailyGoalRef = userEntriesRef.child("DailyGoal")

            // Retrieve min and max goals
            dailyGoalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val minGoals = mutableMapOf<String, String>()
                    val maxGoals = mutableMapOf<String, String>()
                    for (snapshot in dataSnapshot.children) {
                        val date = snapshot.key ?: continue
                        val dailyGoal = snapshot.getValue(DailyGoal::class.java) ?: continue
                        minGoals[date] = dailyGoal.minGoal
                        maxGoals[date] = dailyGoal.maxGoal
                    }

                    val totalTimeTrackedRef = userEntriesRef.child("totalTimeTracked")

                    // Retrieve total time tracked
                    totalTimeTrackedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val totalTimeTracked = mutableMapOf<String, Long>()
                            for (snapshot in dataSnapshot.children) {
                                val date = snapshot.key ?: continue
                                val totalTime = snapshot.child("Time").getValue(Long::class.java) ?: 0L
                                totalTimeTracked[date] = totalTime
                            }

                            // Prepare data for LineChart
                            setupLineChart(minGoals, maxGoals, totalTimeTracked)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e("Firebase", "Failed to retrieve totalTimeTracked", databaseError.toException())
                        }
                    })
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve DailyGoal", databaseError.toException())
                }
            })

        }
    }

    private fun inflateProjectReportLayout() {
        val inflater = LayoutInflater.from(this)
        val projectReportView = inflater.inflate(R.layout.project_report, viewContainer, false)
        viewContainer.removeAllViews()
        viewContainer.addView(projectReportView)
        viewContainer.visibility = View.VISIBLE

        val lineChart = projectReportView.findViewById<LineChart>(R.id.lineChart)

    }

    private fun setupLineChart(minGoals: Map<String, String>, maxGoals: Map<String, String>, totalTimeTracked: Map<String, Long>) {
        val dates = minGoals.keys.toList().sorted()
        val minGoalEntries = mutableListOf<Entry>()
        val maxGoalEntries = mutableListOf<Entry>()
        val totalTimeEntries = mutableListOf<Entry>()

        for (i in dates.indices) {
            val date = dates[i]
            val minGoalTimeString = minGoals[date]
            val maxGoalTimeString = maxGoals[date]
            val totalTime = totalTimeTracked[date]?.toFloat() ?: continue

            if (minGoalTimeString != null && maxGoalTimeString != null) {
                val (minHours, minMinutes) = minGoalTimeString.split(":").map { it.toInt() }
                val (maxHours, maxMinutes) = maxGoalTimeString.split(":").map { it.toInt() }

                val minGoal = minGoals.values.map { timeString ->
                    val (hours, minutes) = timeString.split(":").map { it.toInt() }
                    hours * 60 + minutes // Convert hours to minutes and add them to the minutes
                }.minOrNull()?.toFloat() ?: 0f

///                val maxGoal = maxHours * 60 + maxMinutes

                val maxGoal = maxGoals.values.map { timeString ->
                    val (hours, minutes) = timeString.split(":").map { it.toInt() }
                    hours * 60 + minutes // Convert hours to minutes and add them to the minutes
                }.minOrNull()?.toFloat() ?: 0f

                minGoalEntries.add(Entry(i.toFloat(), minGoal.toFloat()))
                maxGoalEntries.add(Entry(i.toFloat(), maxGoal.toFloat()))
                totalTimeEntries.add(Entry(i.toFloat(), totalTime))
            }
        }

        val minGoalDataSet = LineDataSet(minGoalEntries, "Min Goal").apply {
            color = Color.RED
            valueTextColor = Color.WHITE
        }
        val maxGoalDataSet = LineDataSet(maxGoalEntries, "Max Goal").apply {
            color = Color.GREEN
            valueTextColor = Color.WHITE
        }
        val totalTimeDataSet = LineDataSet(totalTimeEntries, "Total Time Tracked").apply {
            color = Color.BLUE
            valueTextColor = Color.WHITE
        }

        val lineChart = findViewById<LineChart>(R.id.lineChart)

        val lineData = LineData(minGoalDataSet, maxGoalDataSet, totalTimeDataSet)
        lineChart.data = lineData

        // Configure X-axis
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setTextColor(Color.WHITE)

        // Configure Y-axis
        lineChart.axisLeft.setTextColor(Color.WHITE)
        lineChart.axisRight.setTextColor(Color.WHITE)
        lineChart.axisLeft.axisMinimum = minGoals.values.minOrNull()?.toFloat() ?: 0f
        lineChart.axisLeft.axisMaximum = maxGoals.values.maxOrNull()?.toFloat() ?: 100f

        // Configure other chart properties
        lineChart.setBackgroundColor(Color.BLACK)
        lineChart.legend.textColor = Color.WHITE
        lineChart.invalidate() // Refresh the chart
    }

    private fun updateChart(
        lineChart: LineChart,
        entries: MutableList<Entry>,
        minGoalEntries: MutableList<Entry>,
        maxGoalEntries: MutableList<Entry>,
        dateList: MutableList<String>
    ) {
        val dataSet = LineDataSet(entries, "Hours Worked")
        val minGoalDataSet = LineDataSet(minGoalEntries, "Min Goal")
        val maxGoalDataSet = LineDataSet(maxGoalEntries, "Max Goal")

        dataSet.color = Color.BLUE
        minGoalDataSet.color = Color.GREEN
        maxGoalDataSet.color = Color.RED

        val lineData = LineData(dataSet, minGoalDataSet, maxGoalDataSet)
        lineChart.data = lineData

        // Set the custom formatter for the x-axis
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(dateList)
        xAxis.granularity = 1f
        xAxis.position = XAxis.XAxisPosition.BOTTOM

        lineChart.invalidate()
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

    fun addCategoryTextViews(categoryContainer: ViewGroup, categories: List<String>, formattedTimes: List<String>) {
        var topPadding = 40
        categories.zip(formattedTimes).forEach { (category, formattedTime) ->
            val textView = createStyledTextView(categoryContainer.context, "Category: $category $formattedTime", topPadding)
            categoryContainer.addView(textView)
            topPadding += 40
        }
    }
}
