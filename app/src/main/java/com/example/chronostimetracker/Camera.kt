package com.example.chronostimetracker

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat

class Camera(private val activity: Activity) {

    private val REQUEST_CODE_CAMERA = 1
    private val REQUEST_CODE_GALLERY = 2
    private val REQUEST_CODE_PERMISSIONS = 10
    private lateinit var userImg: ImageView

    fun openImagePicker(userImg: ImageView) {
        this.userImg = userImg // Store the ImageView for later use
        val items = arrayOf("Take Photo", "Choose from Library")
        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Add Photo")
        builder.setItems(items) { dialog, item ->
            if (items[item] == "Take Photo") {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (intent.resolveActivity(activity.packageManager) != null) {
                    startActivityForResult(intent, REQUEST_CODE_CAMERA)
                }
            } else {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_CODE_GALLERY)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun startActivityForResult(intent: Intent, requestCode: Int) {
        activity.startActivityForResult(intent, requestCode)
    }

    fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSIONS)
        } else {
            openImagePicker(userImg) // Pass the ImageView when permissions are already granted
        }
    }



    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_GALLERY -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let {
                        userImg.setImageURI(it)
                    }
                }
                REQUEST_CODE_CAMERA -> {
                    // Use IntentCompat.getParcelableExtra to retrieve the Bitmap safely
                    val bitmap = data?.let { IntentCompat.getParcelableExtra<Bitmap>(it, "data", Bitmap::class.java) }
                    bitmap?.let {
                        userImg.setImageBitmap(it)
                    }
                }
            }
        }
    }



    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Permissions are granted, now open the image picker dialog
                openImagePicker(userImg) // Ensure the ImageView is passed when permissions are granted
            } else {
                // Handle permission denied
                // Optionally, inform the user that permissions are necessary for the feature to work
            }
        }
    }

}
