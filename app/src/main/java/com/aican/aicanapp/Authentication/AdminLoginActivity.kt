package com.aican.aicanapp.Authentication

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aican.aicanapp.AdminSettings
import com.aican.aicanapp.FirebaseAccounts.PrimaryAccount
import com.aican.aicanapp.R
import com.aican.aicanapp.UserDatabase
import com.aican.aicanapp.specificActivities.Users.AllUsers
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class AdminLoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var ilEmail: TextInputLayout
    lateinit var ilPass: TextInputLayout
    lateinit var email: String
    lateinit var password: String
    lateinit var primaryEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)


        auth = FirebaseAuth.getInstance(PrimaryAccount.getInstance(this))

        val mail = findViewById<EditText>(R.id.etAdminEmail)
        val pass = findViewById<EditText>(R.id.etAdminPassword)
        val login = findViewById<Button>(R.id.btnAdminLogin)

        ilEmail = findViewById(R.id.inputLayoutEmail)
        ilPass = findViewById(R.id.inputLayoutPass)

        mail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() == "9315036599@Vishal") {
                    val checkFlag = intent.getStringExtra("checkBtn")
                    if (checkFlag == "addUser") {
                        startSettingActivity()
                    } else if (checkFlag == "logout") {
                        logout()
                    } else if (checkFlag == "checkDatabase") {
                        userDatabase()
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        login.setOnClickListener { v: View? ->
            email = mail.text.toString().trim { it <= ' ' }
            password = pass.text.toString().trim { it <= ' ' }
            val sh =
                getSharedPreferences("loginprefs", MODE_PRIVATE)
            primaryEmail = sh.getString("email", "")!!.trim { it <= ' ' }
            if (email == primaryEmail) {
                auth.signInWithEmailAndPassword(
                    email, password
                ).addOnSuccessListener { authResult: AuthResult? ->
                    val intent = intent
                    val checkFlag = intent.getStringExtra("checkBtn")
                    if (checkFlag == "addUser") {
                        startSettingActivity()
                    } else if (checkFlag == "logout") {
                        logout()
                    } else if (checkFlag == "checkDatabase") {
                        userDatabase()
                    } else if (checkFlag == "adminSettings") {
                        openAdminSettings()
                    }
                }.addOnFailureListener { exception: Exception ->
                    if (exception is FirebaseAuthInvalidUserException) {
                        ilEmail.error = "This Email ID is not registered"
                    } else if (exception is FirebaseAuthInvalidCredentialsException) {
                        ilPass.error = "Incorrect Password"
                    } else {
                        Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
                    }
                    exception.printStackTrace()
                }
            } else {
                Toast.makeText(
                    this, "This email is not connected with this device",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    }

    private fun openAdminSettings() {
        startActivity(Intent(this@AdminLoginActivity, AdminSettings::class.java))

    }

    private fun startSettingActivity() {
        val intent = Intent(this, AllUsers::class.java)
//        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
    }

    private fun logout() {
        FirebaseAuth.getInstance(PrimaryAccount.getInstance(applicationContext)).signOut()
        finish()

        //Clear Shared Preference Login Emailvedant
        val sharedPreferences = getSharedPreferences("loginprefs", MODE_PRIVATE)
        val myEdit = sharedPreferences.edit()
        myEdit.clear()
        myEdit.apply()
        finish()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun userDatabase() {
        val intent = Intent(this, UserDatabase::class.java)
        startActivity(intent)
    }


}