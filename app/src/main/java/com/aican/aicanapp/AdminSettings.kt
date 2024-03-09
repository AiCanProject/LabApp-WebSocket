package com.aican.aicanapp

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aican.aicanapp.databinding.ActivityAdminSettingsBinding
import com.aican.aicanapp.specificActivities.Users.AddNewUser
import com.aican.aicanapp.specificActivities.Users.AllUsers
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.google.android.material.textfield.TextInputEditText

class AdminSettings : AppCompatActivity() {

    lateinit var binding: ActivityAdminSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.adminSettings.setOnClickListener {
            val intnt = Intent(this@AdminSettings, AdminActivities::class.java)
            startActivity(intnt)
        }

        binding.addUsersBtn.setOnClickListener {
            val intnt = Intent(this@AdminSettings, AddNewUser::class.java)
            startActivity(intnt)
        }

        binding.allUsersBtn.setOnClickListener {
            val intnt = Intent(this@AdminSettings, AllUsers::class.java)
            startActivity(intnt)
        }

        binding.webSocketUrl.setOnClickListener {
            openWebSocketDialog()
        }

        fetchWebSocketUrl()

        fetchExportConditions()

        fetchDesignations()


    }

    private fun fetchDesignations() {
        val leftDesignationString = SharedPref.getSavedData(this@AdminSettings, SharedKeys.LEFT_DESIGNATION_KEY)
        val rightDesignationString = SharedPref.getSavedData(this@AdminSettings, SharedKeys.RIGHT_DESIGNATION_KEY)


        if (leftDesignationString != null && leftDesignationString != ""){
            binding.leftDesignation.setText(leftDesignationString)
        }else{
            binding.leftDesignation.setText("Operator Sign")
            SharedPref.saveData(this@AdminSettings, SharedKeys.LEFT_DESIGNATION_KEY, "Operator Sign")
        }

        if (rightDesignationString != null && rightDesignationString != ""){
            binding.rightDesignation.setText(rightDesignationString)
        }else{
            binding.rightDesignation.setText("Supervisor Sign")
            SharedPref.saveData(this@AdminSettings, SharedKeys.RIGHT_DESIGNATION_KEY, "Supervisor Sign")
        }

        binding.leftDesignation.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                SharedPref.saveData(this@AdminSettings, SharedKeys.LEFT_DESIGNATION_KEY, p0.toString())

            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

        binding.rightDesignation.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                SharedPref.saveData(this@AdminSettings, SharedKeys.RIGHT_DESIGNATION_KEY, p0.toString())

            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

    }

    private fun fetchExportConditions() {

        val exportCsvEnabled = SharedPref.getSavedData(this@AdminSettings, "EXPORT_CSV")
        val exportPdfEnabled = SharedPref.getSavedData(this@AdminSettings, "EXPORT_PDF")

        if (exportCsvEnabled != null && exportCsvEnabled != "") {
            binding.exportCsvSwitch.isChecked = exportCsvEnabled == "true"
            Source.EXPORT_CSV = true
        }else{
            binding.exportCsvSwitch.isChecked = false
            SharedPref.saveData(this@AdminSettings,"EXPORT_CSV", "false")
            Source.EXPORT_CSV = false
        }

        if (exportPdfEnabled != null && exportPdfEnabled != "") {
            binding.exportPdfSwitch.isChecked = exportPdfEnabled == "true"
            Source.EXPORT_PDF = true
        }else{
            binding.exportPdfSwitch.isChecked = true
            Source.EXPORT_PDF = false
            SharedPref.saveData(this@AdminSettings,"EXPORT_PDF", "true")
        }

        binding.exportCsvSwitch.setOnClickListener {
            if (binding.exportCsvSwitch.isChecked){
                Source.EXPORT_CSV = true
                SharedPref.saveData(this@AdminSettings,"EXPORT_CSV", "true")
            }else{
                Source.EXPORT_CSV = false
                SharedPref.saveData(this@AdminSettings,"EXPORT_CSV", "false")
            }
        }

        binding.exportPdfSwitch.setOnClickListener {
            if (binding.exportPdfSwitch.isChecked){
                Source.EXPORT_PDF = true
                SharedPref.saveData(this@AdminSettings,"EXPORT_PDF", "true")
            }else{
                Source.EXPORT_PDF = false
                SharedPref.saveData(this@AdminSettings,"EXPORT_PDF", "false")
            }
        }

    }

    private fun fetchWebSocketUrl() {
        val websocketUrl = SharedPref.getSavedData(this@AdminSettings, "WEBSOCKET_URL")
        if (websocketUrl != null) {
            if (websocketUrl != "") {
                Source.WEBSOCKET_URL = websocketUrl
                binding.websocketUrlText.text = Source.WEBSOCKET_URL

            } else {
                Source.WEBSOCKET_URL = "ws://192.168.4.1:81"
                binding.websocketUrlText.text = Source.WEBSOCKET_URL

                SharedPref.saveData(this@AdminSettings, "WEBSOCKET_URL", "ws://192.168.4.1:81")
            }
        } else {
            Source.WEBSOCKET_URL = "ws://192.168.4.1:81"

            binding.websocketUrlText.text = Source.WEBSOCKET_URL
            SharedPref.saveData(this@AdminSettings, "WEBSOCKET_URL", "ws://192.168.4.1:81")

        }
    }

    private fun openWebSocketDialog() {
        // In your activity or fragment
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.developer_dialog)

        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val etWebSocketUrl = dialog.findViewById<TextInputEditText>(R.id.webSocketUrlTxt)
        val etPassword = dialog.findViewById<TextInputEditText>(R.id.devPassword)

        btnSave.setOnClickListener {
            val webSocketUrl = etWebSocketUrl.text.toString()
            val password = etPassword.text.toString()

            if (webSocketUrl.isEmpty() || password.isEmpty()) {
                if (webSocketUrl.isEmpty()) {
                    etWebSocketUrl.error = "Enter websocket URL"
                }

                if (password.isEmpty()) {
                    etPassword.error = "Enter developer password"
                }
            } else {
                if (password == "12345678") {
                    Source.WEBSOCKET_URL = webSocketUrl
                    binding.websocketUrlText.text = webSocketUrl
                    SharedPref.saveData(this@AdminSettings, "WEBSOCKET_URL", webSocketUrl)

                    Toast.makeText(this, "URL saved", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()

                } else {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()

                }
            }


        }

        dialog.show()

    }
}