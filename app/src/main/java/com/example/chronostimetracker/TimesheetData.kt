package com.example.chronostimetracker

data class TimesheetData(
    //val uniqueId: String? = null, // Use unique key as the ID
    val uniqueId: String? = null,
    var projectName: String = "",
    var category: String = "",
    var description: String = "",
    var startTime: String = "",
    var startDate: String = "",
    var endTime: String = "",
    var endDate: String = "",

    var imageData: String? = "",
    var creationTime: Long = 0,
    var elapsedTime: Long = 0
)
