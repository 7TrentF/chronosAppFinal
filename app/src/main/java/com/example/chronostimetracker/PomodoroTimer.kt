package com.example.chronostimetracker

import android.app.Dialog
import android.graphics.drawable.AnimationDrawable
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.util.Locale

class PomodoroTimer(private val activity: AppCompatActivity) : LifecycleObserver {

    private var timeSelected: Int = 0 // No default time
    private var timeCountDown: CountDownTimer? = null
    private var timeProgress = 0
    private var pauseOffset: Long = 0
    private var isStart = true
    private var isBreak = false
    private var mediaPlayer: MediaPlayer? = null
    private var timeAddedMediaPlayer: MediaPlayer? = null
    private var tickTockMediaPlayer: MediaPlayer? = null

    private var breakTimeCountDown: CountDownTimer? = null
    private var isBreakRunning = false
    private var breakTimeSelected: Int = 5 * 60 // default break time set to 5 minutes
    private var breakMediaPlayer: MediaPlayer? = null

    init {
        activity.lifecycle.addObserver(this)
    }

    fun setupDialog(pomodoroDialog: Dialog) {
        val mainLayout = pomodoroDialog.findViewById<ConstraintLayout>(R.id.mainLayout)
        val animationDrawable = mainLayout.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(3000)
        animationDrawable.setExitFadeDuration(3000)
        animationDrawable.start()

        val settingsTv: TextView = pomodoroDialog.findViewById(R.id.tvPomodoroSettings)
        settingsTv.setOnClickListener {
            setTime(pomodoroDialog)
        }

        val startBtn: Button = pomodoroDialog.findViewById(R.id.btnPlayPause)
        startBtn.setOnClickListener {
            startTimerSetup(pomodoroDialog)
        }

        val resetBtn: ImageButton = pomodoroDialog.findViewById(R.id.ib_reset)
        resetBtn.setOnClickListener {
            resetTime(pomodoroDialog)
        }

        val addTimeTv: TextView = pomodoroDialog.findViewById(R.id.tv_addTime)
        addTimeTv.setOnClickListener {
            addExtraTime(pomodoroDialog)
        }

        val startBreakBtn: Button = pomodoroDialog.findViewById(R.id.btnBreakStartStop)
        startBreakBtn.setOnClickListener {
            if (isBreakRunning) {
                endBreak(pomodoroDialog)
            } else {
                startBreak(pomodoroDialog)
            }
        }

        // Initially hide the break button
        startBreakBtn.visibility = View.GONE
    }

    private fun setTime(pomodoroDialog: Dialog) {
        val timeDialog = Dialog(activity)
        timeDialog.setContentView(R.layout.add_dialog)
        val timeSet = timeDialog.findViewById<EditText>(R.id.etGetTime)
        val timeLeftTv: TextView = pomodoroDialog.findViewById(R.id.tvTimeLeft)
        val btnStart: Button = pomodoroDialog.findViewById(R.id.btnPlayPause)
        val progressBar = pomodoroDialog.findViewById<ProgressBar>(R.id.pbTimer)
        timeDialog.findViewById<Button>(R.id.btnOk).setOnClickListener {
            if (timeSet.text.isEmpty()) {
                Toast.makeText(activity, activity.getString(R.string.enter_time), Toast.LENGTH_SHORT).show()
            } else {
                resetTime(pomodoroDialog)
                timeLeftTv.text = timeSet.text
                btnStart.text = activity.getString(R.string.start)
                timeSelected = timeSet.text.toString().toInt() * 60 // Convert minutes to seconds
                progressBar.max = timeSelected
            }
            timeDialog.dismiss()
        }
        timeDialog.show()
    }

    private fun addExtraTime(pomodoroDialog: Dialog) {
        val progressBar: ProgressBar = pomodoroDialog.findViewById(R.id.pbTimer)
        if (timeSelected != 0) {
            timeSelected += 60 // Add 1 minute
            progressBar.max = timeSelected
            timePause()
            startTimer(pomodoroDialog, pauseOffset)
            Toast.makeText(activity, activity.getString(R.string.minute_added), Toast.LENGTH_SHORT).show()

            // Play time added sound
            playTimeAddedSound()
        }
    }

    private fun resetTime(pomodoroDialog: Dialog) {
        if (timeCountDown != null) {
            timeCountDown!!.cancel()
            timeProgress = 0
            timeSelected = 0 // Reset to 0 minutes
            pauseOffset = 0
            timeCountDown = null
            val startBtn: Button = pomodoroDialog.findViewById(R.id.btnPlayPause)
            startBtn.text = activity.getString(R.string.start)
            isStart = true
            isBreak = false
            val progressBar = pomodoroDialog.findViewById<ProgressBar>(R.id.pbTimer)
            progressBar.progress = 0
            val timeLeftTv: TextView = pomodoroDialog.findViewById(R.id.tvTimeLeft)
            timeLeftTv.text = "0"

            // Release MediaPlayer for tick-tock sound
            stopTickTockSound()

            // Stop break sound if playing
            stopBreakSound()

            // Hide the break button
            val startBreakBtn: Button = pomodoroDialog.findViewById(R.id.btnBreakStartStop)
            startBreakBtn.visibility = View.GONE
        }
    }

    private fun timePause() {
        // Pause tick-tock sound
        pauseTickTockSound()

        if (timeCountDown != null) {
            timeCountDown!!.cancel()
        }
    }

    private fun startTimerSetup(pomodoroDialog: Dialog) {
        val startBtn: Button = pomodoroDialog.findViewById(R.id.btnPlayPause)
        if (timeSelected > 0) {
            if (isStart) {
                startBtn.text = activity.getString(R.string.pause)
                startTimer(pomodoroDialog, pauseOffset)
                isStart = false

                // Show the break button
                val startBreakBtn: Button = pomodoroDialog.findViewById(R.id.btnBreakStartStop)
                startBreakBtn.visibility = View.VISIBLE
            } else {
                isStart = true
                startBtn.text = activity.getString(R.string.resume)
                timePause()
            }
        } else {
            Toast.makeText(activity, "Set the minutes in the pomodoro settings first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startTimer(pomodoroDialog: Dialog, timeInMilliSeconds: Long) {
        val timeLeftTv: TextView = pomodoroDialog.findViewById(R.id.tvTimeLeft)
        val progressBar: ProgressBar = pomodoroDialog.findViewById(R.id.pbTimer)
        progressBar.progress = timeProgress

        // Resume or start tick-tock sound
        resumeTickTockSound()

        timeCountDown = object : CountDownTimer(timeSelected * 1000L - timeInMilliSeconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                pauseOffset = timeSelected * 1000L - millisUntilFinished
                timeProgress++
                progressBar.progress = timeProgress

                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timeLeftTv.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timeProgress = 0
                pauseOffset = 0
                resetTime(pomodoroDialog)
                // Play sound on finish
                playSound()
            }
        }.start()
    }

    private fun startBreak(pomodoroDialog: Dialog) {
        val startBreakBtn: Button = pomodoroDialog.findViewById(R.id.btnBreakStartStop)
        startBreakBtn.text = activity.getString(R.string.end_break)
        isBreakRunning = true
        timePause()

        val timeLeftTv: TextView = pomodoroDialog.findViewById(R.id.tvTimeLeft)
        val progressBar: ProgressBar = pomodoroDialog.findViewById(R.id.pbTimer)
        progressBar.max = breakTimeSelected
        progressBar.progress = 0
        var breakTimeProgress = 0

        // Hide the pause button
        val startBtn: Button = pomodoroDialog.findViewById(R.id.btnPlayPause)
        startBtn.visibility = View.GONE

        breakTimeCountDown = object : CountDownTimer(breakTimeSelected * 1000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                breakTimeProgress++
                progressBar.progress = breakTimeProgress

                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                timeLeftTv.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isBreakRunning = false
                startBreakBtn.text = activity.getString(R.string.start_break)
                startTimer(pomodoroDialog, pauseOffset)

                // Stop break sound
                stopBreakSound()

                // Show the pause button
                startBtn.visibility = View.VISIBLE
            }
        }.start()

        // Play break sound
        playBreakSound()
    }

    private fun endBreak(pomodoroDialog: Dialog) {
        val startBreakBtn: Button = pomodoroDialog.findViewById(R.id.btnBreakStartStop)
        startBreakBtn.text = activity.getString(R.string.start_break)
        isBreakRunning = false
        breakTimeCountDown?.cancel()

        // Stop break sound
        stopBreakSound()

        // Resume the pomodoro timer
        startTimer(pomodoroDialog, pauseOffset)

        // Show the pause button
        val startBtn: Button = pomodoroDialog.findViewById(R.id.btnPlayPause)
        startBtn.visibility = View.VISIBLE
    }

    private fun playSound() {
        mediaPlayer = MediaPlayer.create(activity, R.raw.tick_tock_sound)
        mediaPlayer?.start()
    }

    private fun playTimeAddedSound() {
        timeAddedMediaPlayer = MediaPlayer.create(activity, R.raw.time_added)
        timeAddedMediaPlayer?.setOnCompletionListener {
            // Resume tick-tock sound from where it left off
            resumeTickTockSound()
        }
        pauseTickTockSound()
        timeAddedMediaPlayer?.start()
    }

    private fun playBreakSound() {
        breakMediaPlayer = MediaPlayer.create(activity, R.raw.break_sound)
        breakMediaPlayer?.isLooping = true
        breakMediaPlayer?.start()
    }

    private fun stopBreakSound() {
        if (breakMediaPlayer?.isPlaying == true) {
            breakMediaPlayer?.stop()
            breakMediaPlayer?.release()
            breakMediaPlayer = null
        }
    }

    private fun playTickTockSound() {
        if (tickTockMediaPlayer == null) {
            tickTockMediaPlayer = MediaPlayer.create(activity, R.raw.tick_tock_sound)
            tickTockMediaPlayer?.isLooping = true
        }
        tickTockMediaPlayer?.start()
    }

    private fun pauseTickTockSound() {
        if (tickTockMediaPlayer?.isPlaying == true) {
            tickTockMediaPlayer?.pause()
        }
    }

    private fun resumeTickTockSound() {
        if (tickTockMediaPlayer == null) {
            tickTockMediaPlayer = MediaPlayer.create(activity, R.raw.tick_tock_sound)
            tickTockMediaPlayer?.isLooping = true
        }
        tickTockMediaPlayer?.start()
    }

    private fun stopTickTockSound() {
        if (tickTockMediaPlayer?.isPlaying == true) {
            tickTockMediaPlayer?.stop()
            tickTockMediaPlayer?.release()
            tickTockMediaPlayer = null
        }
    }

    fun stopAllSounds() {
        stopTickTockSound()
        stopBreakSound()
        mediaPlayer?.release()
        mediaPlayer = null
        timeAddedMediaPlayer?.release()
        timeAddedMediaPlayer = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onAppPause() {
        // Stop all sounds and timers when the user navigates away from the Pomodoro dialog
        timePause()
        stopAllSounds()
    }
}
