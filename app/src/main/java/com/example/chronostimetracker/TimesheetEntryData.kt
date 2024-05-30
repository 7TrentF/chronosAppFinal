package com.example.chronostimetracker

class TimesheetEntryData(
    val id: String? = null,
  //  var id: Long = 0,
    val projectName: String,
    val category: String,
    val description: String,
    val startTime: String,
    val startDate: String,
    val endTime: String,
    val endDate: String,
    val minHours: Int,
    val maxHours: Int,
    val imageData: String?,
    var creationTime: Long = 0,
    var elapsedTime: Long = 0
)


