

package com.example.chronostimetracker

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.concurrent.TimeUnit

class TimesheetEntryAdapter(private var entries: List<TimesheetData>) : RecyclerView.Adapter<TimesheetEntryAdapter.ViewHolder>() {
    private lateinit var camera: Camera
    private var timer: Timer? = null
    private var elapsedTime: Long = 0
    private val timerStates = mutableMapOf<Int, Boolean>()
    private val timerStartTimes = mutableMapOf<Int, Long>() // Map to track timer start times
    private var timerHandler: Handler? = null // Declare timerHandler
    private var timerRunnable: Runnable? = null // Declare timerRunnable
    private val categoryTotalTime = mutableMapOf<String, Long>()
    private var database: DatabaseReference
    private var initialTotal: Long = 0
    init {
        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().reference
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uniqueIdTextView: TextView = itemView.findViewById(R.id.uniqueIdTextView)
        val projectNameTextView: TextView = itemView.findViewById(R.id.projectNameTextView)
        val categoryTextView: TextView = itemView.findViewById(R.id.CategoryTextView)
        val imageView: ImageView = itemView.findViewById(R.id.userImage)
        val timerButton: Button = itemView.findViewById(R.id.timerButton)
        val timerTextView: TextView = itemView.findViewById(R.id.timerTextView)
        val TimesheetDate: TextView = itemView.findViewById(R.id.topTextView)
        val currentUser = FirebaseAuth.getInstance().currentUser // Fetch the currently logged-in user
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newEntries: List<TimesheetData>) {
        this.entries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.timesheet_entry_card, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        holder.uniqueIdTextView.text = entry.uniqueId // Assuming uniqueId is a Long or similar
        holder.projectNameTextView.text = entry.projectName
        holder.categoryTextView.text = entry.category

        if (entry.imageData != null) {
            val decodedString = Base64.decode(entry.imageData, Base64.DEFAULT)
            val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            holder.imageView.setImageBitmap(decodedBitmap)
        } else {
            holder.imageView.setImageResource(R.drawable.default_image) // Default image if none is present

        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Fetch the elapsed time and creation time from Firebase
            val databaseRef = database.child("user_entries").child(user.uid).child("Timesheet Entries").child(entry.uniqueId.toString())
            databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val elapsedTime = dataSnapshot.child("elapsedTime").getValue(Long::class.java) ?: 0
                    val creationTime = dataSnapshot.child("creationTime").getValue(Long::class.java) ?: 0

                    // Log the raw creationTime value
                    Log.d("creationTime", "Raw creationTime for unique ID ${entry.uniqueId}: $creationTime")

                    // Format the creation date
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    val formattedDate = sdf.format(Date(creationTime))
                    holder.TimesheetDate.text = formattedDate

                    // Format the elapsed time and set it to timerTextView
                    val formattedTime = formatElapsedTime(elapsedTime)
                    holder.timerTextView.text = formattedTime

                    Log.d("elapsedTime", "Fetched Elapsed Time for unique ID ${entry.uniqueId}: $elapsedTime")
                    Log.d("creation", "Formatted Creation Time for unique ID ${entry.uniqueId}: $formattedDate")
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("TimesheetEntryAdapter", "Failed to read elapsed time or creation time", databaseError.toException())
                }
            })

            // Fetch the total time for the category from Firebase
            val category = entries[position].category
            val categoryRef = database.child("user_entries").child(user.uid).child("CategoryTimes").child(entry.category)
            categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val totalTime = dataSnapshot.child("totalTime").getValue(Long::class.java) ?: 0
                    Log.d("totalCategory", "Total Time for category ${entry.category}: $totalTime")
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("TimesheetEntryAdapter", "Failed to read total time for category", databaseError.toException())
                }
            })
        }

        holder.timerButton.setOnClickListener {
           // val uniqueId: String? = entry.uniqueId
            entry.uniqueId
            showTimerDialog(holder.itemView.context, entry, position)
        }

        holder.itemView.setOnClickListener {
            showBottomSheetDialog(holder.itemView.context, entry)
        }
    }

    private fun showBottomSheetDialog(context: Context, entry: TimesheetData) {
        val editHandler = TimesheetEdit(context, database)
        editHandler.showEditDialog(entry) { editedEntry ->
            editHandler.saveEntry(editedEntry)
            // Update the list and notify the adapter
            val position = entries.indexOfFirst { it.uniqueId == editedEntry.uniqueId }
            if (position != -1) {
                entries = entries.toMutableList().apply {
                    set(position, editedEntry)
                }
                notifyItemChanged(position)
            }
        }
    }

    fun showTimerDialog(context: Context,  entry: TimesheetData, position: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user is authenticated
        currentUser?.let { user ->
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.dialog_timer)

            val timerTextView = dialog.findViewById<TextView>(R.id.timerTextView)
            val stopButton = dialog.findViewById<Button>(R.id.stopButton)

            var startTime: Long = System.currentTimeMillis() // Start the timer immediately
            var isTimerRunning = true // Timer is running by default

            // Start the timer immediately
            val timerHandler = Handler(Looper.getMainLooper())
            val timerRunnable = object : Runnable {
                override fun run() {
                    if (isTimerRunning) {
                        val elapsedTime = System.currentTimeMillis() - startTime
                        val formattedTime = formatElapsedTime(elapsedTime)
                        timerTextView.text = formattedTime
                        timerHandler.postDelayed(this, 1000)
                    }
                }
            }
            timerHandler.post(timerRunnable)

            stopButton.setOnClickListener {
                isTimerRunning = false // Stop the timer

                // Calculate the elapsed time
                val elapsedTime = System.currentTimeMillis() - startTime
                // Set the formatted time based on the new total elapsed time
                val formattedTime = formatElapsedTime(elapsedTime)
                // Set the text of timerTextView with the formatted time
                timerTextView.text = formattedTime


                database = FirebaseDatabase.getInstance().reference.child("user_entries").child(user.uid)
                val timesheetEntryRef = database.child("Timesheet Entries").child(entry.uniqueId.toString())

                // Get a reference to the user's totalTimeTracked path
                val totalTimeTrackedRef = database.child("totalTimeTracked")
                // Get the current date in the format "YYYY-MM-DD"
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())



                // Check if an entry for the current day already exists in totalTimeTracked
                totalTimeTrackedRef.child(currentDate).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            // Entry for the current day doesn't exist, create a new entry
                            totalTimeTrackedRef.child(currentDate).child("Time").setValue(0L)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("Firebase", "Failed to check totalTimeTracked for the current day", databaseError.toException())
                    }
                })

                timesheetEntryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val existingElapsedTime =
                            dataSnapshot.child("elapsedTime").getValue(Long::class.java) ?: 0
                        val newTotalElapsedTime = existingElapsedTime + elapsedTime

                        // Update the elapsed time in Firebase
                        timesheetEntryRef.child("elapsedTime").setValue(newTotalElapsedTime)
                            .addOnSuccessListener {
                                Log.d(
                                    "Firebase",
                                    "Successfully updated elapsedTime for uniqueId $entry.uniqueId"
                                )

                                // Update totalTimeTracked for the current day
                                totalTimeTrackedRef.child(currentDate).child("Time")
                                    .setValue(ServerValue.increment(elapsedTime))
                                    .addOnSuccessListener {
                                        Log.d(
                                            "Firebase",
                                            "Successfully updated totalTimeTracked for the current day"
                                        )


                                    }
                            }



                            .addOnFailureListener { e ->
                                Log.e(
                                    "Firebase",
                                    "Failed to update elapsedTime for uniqueId $entry.uniqueId",
                                    e
                                )
                            }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(
                            "Firebase",
                            "Failed to retrieve elapsedTime for uniqueId $entry.uniqueId",
                            databaseError.toException()
                        )
                    }
                })

                // Update the total time for the category in Firebase
                val category = entries[position].category
                val categoryRef = database.child("CategoryTimes").child(category)
                categoryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val totalTime  =  dataSnapshot.child("totalTime").getValue(Long::class.java) ?: 0

                        val newCategoryTotalTime = totalTime + elapsedTime
                        categoryRef.child("totalTime").setValue(newCategoryTotalTime)
                            .addOnSuccessListener {
                                Log.d(
                                    "Firebase",
                                    "Successfully updated totalTime for category $category"
                                )
                                // Save the category total times
                                saveCategoryTotalTimes(context)
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "Firebase",
                                    "Failed to update totalTime for category $category",
                                    e
                                )
                            }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e(
                            "Firebase",
                            "Failed to retrieve totalTime for category $category",
                            databaseError.toException()
                        )
                    }
                })

                // Close the dialog
                dialog.dismiss()


                // Set a listener to be called when the dialog is dismissed
                dialog.setOnDismissListener {
                    // Update the timerTextView in the ViewHolder
                    val entry = entries[position]


                    database = FirebaseDatabase.getInstance().reference

                    // Get a reference to the user's entry path
                    val userEntriesRef = database.child("user_entries").child(user.uid)

                    // Create a reference to the Timesheet Entries child under the user's path
                    val timesheetEntryRef = userEntriesRef.child("Timesheet Entries")



                    timesheetEntryRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val savedElapsedTime =
                                dataSnapshot.child("elapsedTime").getValue(Long::class.java) ?: 0
                            Log.d(
                                "YourTag",
                                "Saved Elapsed Time for unique ID ${entry.uniqueId}: $savedElapsedTime"
                            )
                            val formattedTime = formatElapsedTime(savedElapsedTime)
                            notifyItemChanged(position, formattedTime)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            Log.e(
                                "YourTag",
                                "Failed to retrieve elapsedTime for unique ID ${entry.uniqueId}",
                                databaseError.toException()
                            )
                        }
                    })
                }
            }

            dialog.show()

            // Set dialog position to bottom of the screen
            val window = dialog.window
            val layoutParams = window?.attributes
            layoutParams?.gravity = Gravity.BOTTOM
            window?.attributes = layoutParams
        }
    }



    private fun saveCategoryTotalTimes(context: Context) {
        // Get the current authenticated user
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Check if the user is authenticated
        currentUser?.let { user ->
            // Initialize Firebase Database reference
            val database = FirebaseDatabase.getInstance().reference

            categoryTotalTime.forEach { (category, totalTime) ->
                // Create a reference for the specific category
                val categoryRef = database.child("CategoryTimes").child(category)

                // Save the total time to Firebase
                categoryRef.child("totalTime").setValue(totalTime)
                    .addOnSuccessListener {
                        Log.d("CategoryData", "Successfully saved totalTime for category $category: $totalTime")
                    }
                    .addOnFailureListener { e ->
                        Log.e("CategoryData", "Failed to save totalTime for category $category", e)
                    }
            }
        } ?: run {
            // If the user is not authenticated, handle it accordingly
            Log.e("CategoryData", "User is not authenticated. Unable to save category total times.")
            // You can show a message to the user or handle the situation in another way
        }
    }




    private fun formatElapsedTime(elapsedTime: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun getItemCount() = entries.size
}