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
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
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
import com.github.mikephil.charting.formatter.ValueFormatter
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
       // configurePieChart()
        //setupLineChart()

        fetchAndPopulateLineChart()

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



        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance().reference
            val userEntriesRef = database.child("user_entries").child(user.uid)
            val totalTimeTrackedRef = userEntriesRef.child("totalTimeTracked")
            totalTimeTrackedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val entries = mutableListOf<Entry>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                    for (dateSnapshot in dataSnapshot.children) {
                        val date = dateSnapshot.key ?: continue
                        val totalTime = dateSnapshot.child("Time").getValue(Long::class.java) ?: continue

                        val totalHours = (totalTime / (1000 * 60 * 60)).toFloat()
                        val totalMinutes = ((totalTime % (1000 * 60 * 60)) / (1000 * 60)).toFloat()
                        val totalSeconds = ((totalTime % (1000 * 60)) / 1000).toFloat()
                        val totalTimeInHours = totalHours + totalMinutes / 60 + totalSeconds / 3600

                        val dateParsed = dateFormat.parse(date)
                        val dateMillis = dateParsed?.time?.toFloat() ?: continue

                        entries.add(Entry(dateMillis, totalTimeInHours))
                    }

                    // Sort entries by X value (date)
                    entries.sortBy { it.x }

                    // Update LineChart with the retrieved data
                    updateLineChart(entries)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve totalTimeTracked", databaseError.toException())
                }
            })
        }




    }


    fun updateLineChart(entries: List<Entry>) {
        val lineChart = findViewById<LineChart>(R.id.lineChart)

        val lineDataSet = LineDataSet(entries, "Total Time Tracked")

        // Customize the line appearance
        lineDataSet.color = Color.WHITE // Set the line color
        lineDataSet.valueTextColor = Color.YELLOW // Set the text color for values
        lineDataSet.lineWidth = 2f // Set the width of the line
        lineDataSet.circleRadius = 4f // Set the size of the points (circles)
        lineDataSet.circleHoleRadius = 2f // Set the size of the hole in the points
        lineDataSet.setCircleColor(Color.YELLOW) // Set the color of the points (circles)
        lineDataSet.valueTextSize = 12f // Set the text size for values
       // lineDataSet.enableDashedLine(10f, 5f, 0f) // Dashed line
        lineDataSet.setDrawFilled(true) // Enable fill
       // lineDataSet.fillColor = Color.BLUE // Set fill color
        lineDataSet.fillAlpha = 50 // Set fill transparency

        // Highlighting
        lineDataSet.setDrawHighlightIndicators(true)
        lineDataSet.highLightColor = Color.YELLOW // Color for highlighting

        // Create custom value formatter for hours, minutes, and seconds
        lineDataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry): String {
                val totalHours = entry.y.toInt()
                val totalMinutes = ((entry.y - totalHours) * 60).toInt()
                val totalSeconds = ((((entry.y - totalHours) * 60) - totalMinutes) * 60).toInt()

                return String.format("%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds)
            }
        }

        // Create LineData with the customized dataset
        val lineData = LineData(lineDataSet)
        lineChart.data = lineData

        // Customizing the X-axis to display dates
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            override fun getFormattedValue(value: Float): String {
                return dateFormat.format(Date(value.toLong()))
            }
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 12f // Set X-axis text size
        xAxis.textColor = Color.WHITE // Set X-axis text color
        xAxis.labelRotationAngle = -45f // Rotate labels if necessary
        xAxis.granularity = 1f // Set the minimum interval between labels

        // Customizing the Y-axis
        val yAxisLeft = lineChart.axisLeft
        yAxisLeft.textSize = 12f // Set Y-axis text size
        yAxisLeft.textColor = Color.WHITE // Set Y-axis text color
        yAxisLeft.axisMinimum = 0f // Set the minimum value for the Y-axis


        val yAxisRight = lineChart.axisRight
        yAxisRight.isEnabled = true // Disable the right Y-axis

        // Adding labels to the axes
        xAxis.setLabelCount(entries.size, true) // Ensure that all X-axis labels are shown
        yAxisLeft.setLabelCount(6, true) // Adjust number of Y-axis labels

        // Customizing the LineChart
        lineChart.setBackgroundColor(Color.BLACK) // Set background color
        lineChart.xAxis.setDrawGridLines(false) // Disable X-axis grid lines
        lineChart.axisLeft.setDrawGridLines(false) // Disable left Y-axis grid lines

        // Legend customization
        val legend = lineChart.legend
        legend.form = Legend.LegendForm.LINE
        legend.textColor = Color.WHITE // Legend text color
        legend.textSize = 14f // Legend text size

        // Description customization
        val description = Description()
        description.text = "Time Tracked Over Days"
        description.textColor = Color.WHITE // Description text color
        description.textSize = 14f // Description text size
        lineChart.description = description

        // Refresh the chart
        lineChart.invalidate()
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

    private fun fetchAndPopulateLineChart() {
        val database = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        val lineChart = findViewById<LineChart>(R.id.lineChart)

        currentUser?.let { user ->
            val userEntriesRef = database.child("user_entries").child(user.uid)

            // Fetch Daily Goals
            val dailyGoalRef = userEntriesRef.child("DailyGoal")
            dailyGoalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val entries = mutableListOf<Entry>()
                    for (snapshot in dataSnapshot.children) {
                        val dailyGoal = snapshot.getValue(DailyGoal::class.java)
                        dailyGoal?.let {
                            val minGoal = dailyGoal.minGoal.toFloat()
                            val maxGoal = dailyGoal.maxGoal.toFloat()
                            val date = snapshot.key ?: ""
                            val numericDate = convertDateStringToNumeric(date)
                            entries.add(Entry(numericDate.toFloat(), minGoal))
                            entries.add(Entry(numericDate.toFloat(), maxGoal))
                        }
                    }
                    // Populate LineChart
                    val dataSet = LineDataSet(entries, "Min/Max Goals")
                    dataSet.color = Color.WHITE
                    dataSet.valueTextColor = Color.WHITE
                    val lineData = LineData(dataSet)
                    lineChart.data = lineData
                    lineChart.invalidate()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve daily goals", databaseError.toException())
                }
            })

            // Fetch Total Time Tracked
            val totalTimeTrackedRef = userEntriesRef.child("totalTimeTracked")
            totalTimeTrackedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val entries = mutableListOf<Entry>()
                    for (snapshot in dataSnapshot.children) {
                        val totalMilliseconds = snapshot.child("Time").getValue(Long::class.java) ?: 0
                        val totalHours = totalMilliseconds / (1000 * 60 * 60)
                        val date = snapshot.key ?: ""
                        val numericDate = convertDateStringToNumeric(date)
                        entries.add(Entry(numericDate.toFloat(), totalHours.toFloat()))
                    }
                    // Populate LineChart
                    val dataSet = LineDataSet(entries, "Total Time Tracked")
                    dataSet.color = Color.RED // Set color for total time tracked
                    dataSet.valueTextColor = Color.RED
                    val lineData = LineData(dataSet)
                    lineChart.data = lineData
                    lineChart.invalidate()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve total time tracked", databaseError.toException())
                }
            })
        }
    }


    // Function to convert date string to a numeric representation (e.g., number of days since a reference date)
    private fun convertDateStringToNumeric(dateString: String): Long {

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateString)
        val referenceDate = sdf.parse("2024-01-01") // Use your reference date
        val diffInMillis = date.time - referenceDate.time
        // Convert milliseconds to days
        return TimeUnit.MILLISECONDS.toDays(diffInMillis)
    }
    private fun convertNumericToDate(numericDate: Long): String {
        // Convert numeric date back to date string
        val referenceDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse("2024-01-01") // Use your reference date
        val calendar = Calendar.getInstance()
        calendar.time = referenceDate
        calendar.add(Calendar.DAY_OF_YEAR, numericDate.toInt())
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }



    private fun setupLineChart() {
        val lineChart = findViewById<LineChart>(R.id.lineChart)

        // Customize LineChart
        lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.WHITE
            axisLeft.textColor = Color.WHITE

            // Set Y-axis bounds based on the range of min/max goals and total time tracked
            val maxY = calculateMaxYValue() // Implement this function to calculate the maximum Y-axis value
            val minY = calculateMinYValue() // Implement this function to calculate the minimum Y-axis value

            axisLeft.axisMinimum = minY
            axisLeft.axisMaximum = maxY
        }
    }

    private fun calculateMaxYValue(): Float {
        val database = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        var maxY = Float.MIN_VALUE

        currentUser?.let { user ->
            val userEntriesRef = database.child("user_entries").child(user.uid)

            // Fetch Daily Goals
            val dailyGoalRef = userEntriesRef.child("DailyGoal")
            dailyGoalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val dailyGoal = snapshot.getValue(DailyGoal::class.java)
                        dailyGoal?.let {
                            val maxGoal = dailyGoal.maxGoal.toFloat()
                            if (maxGoal > maxY) {
                                maxY = maxGoal
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve daily goals", databaseError.toException())
                }
            })

            // Fetch Total Time Tracked
            val totalTimeTrackedRef = userEntriesRef.child("totalTimeTracked")
            totalTimeTrackedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val totalMilliseconds = snapshot.child("Time").getValue(Long::class.java) ?: 0
                        val totalHours = totalMilliseconds / (1000 * 60 * 60)
                        if (totalHours > maxY) {
                            maxY = totalHours.toFloat()
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve total time tracked", databaseError.toException())
                }
            })
        }

        return maxY
    }

    private fun calculateMinYValue(): Float {
        val database = FirebaseDatabase.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser
        var minY = Float.MAX_VALUE

        currentUser?.let { user ->
            val userEntriesRef = database.child("user_entries").child(user.uid)

            // Fetch Daily Goals
            val dailyGoalRef = userEntriesRef.child("DailyGoal")
            dailyGoalRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val dailyGoal = snapshot.getValue(DailyGoal::class.java)
                        dailyGoal?.let {
                            val minGoal = dailyGoal.minGoal.toFloat()
                            if (minGoal < minY) {
                                minY = minGoal
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve daily goals", databaseError.toException())
                }
            })

            // Fetch Total Time Tracked
            val totalTimeTrackedRef = userEntriesRef.child("totalTimeTracked")
            totalTimeTrackedRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val totalMilliseconds = snapshot.child("Time").getValue(Long::class.java) ?: 0
                        val totalHours = totalMilliseconds / (1000 * 60 * 60)
                        if (totalHours < minY) {
                            minY = totalHours.toFloat()
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase", "Failed to retrieve total time tracked", databaseError.toException())
                }
            })
        }

        return minY
    }



    private fun inflateProjectReportLayout() {
        val inflater = LayoutInflater.from(this)
        val projectReportView = inflater.inflate(R.layout.project_report, viewContainer, false)
        viewContainer.removeAllViews()
        viewContainer.addView(projectReportView)
        viewContainer.visibility = View.VISIBLE

        val lineChart = projectReportView.findViewById<LineChart>(R.id.lineChart)
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
