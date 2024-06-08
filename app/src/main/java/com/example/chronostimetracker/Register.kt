package com.example.chronostimetracker

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.content.Intent
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth
import com.google.firebase.database.database

class Register : AppCompatActivity() {
    private lateinit var user: EditText
    private lateinit var pass: EditText
    private lateinit var btnReg: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var regInfo :Button
    private lateinit var backLogin :Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        user = findViewById(R.id.editUsername)
        pass = findViewById(R.id.editPassword)
        btnReg = findViewById(R.id.buttonRegister)
        regInfo = findViewById(R.id.btnRegInfo)
        backLogin = findViewById(R.id.btnBackToLogin)
        auth = Firebase.auth
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        regInfo.setOnClickListener {
                Toast.makeText(this, "Password must be at least characters long", Toast.LENGTH_SHORT).show()
        }
        backLogin.setOnClickListener {
            val intent = Intent(this@Register, Login::class.java)
            startActivity(intent)
            finish()
        }
        btnReg.setOnClickListener{
            val username = user.text.toString()
            val password = pass.text.toString()
            registerUser(username, password)
        }

    }

    private fun registerUser(username: String, password: String) {
        auth.createUserWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val userId = it.uid
                        // Create a path specific to this user in the database
                        val userEntriesRef = Firebase.database.reference.child("user_entries").child(userId)

                        // You can save initial data for the user under this path if needed
                        // userEntriesRef.child("initial_entry").setValue("Initial data")

                        Toast.makeText(baseContext, "Registration Successful", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@Register, Login::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    when {
                        task.exception is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(baseContext, "The password is too weak.", Toast.LENGTH_SHORT).show()
                        }
                        task.exception is FirebaseAuthUserCollisionException -> {
                            Toast.makeText(baseContext, "The username is taken.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(baseContext, "Registration failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
    }

}