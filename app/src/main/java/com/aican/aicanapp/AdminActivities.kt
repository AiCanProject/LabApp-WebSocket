package com.aican.aicanapp

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aican.aicanapp.databinding.ActivitiesAdminPageBinding
import com.aican.aicanapp.ph.phFragment.PhCalibFragmentNew
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.google.android.material.textfield.TextInputEditText

class AdminActivities : AppCompatActivity() {

    lateinit var binding: ActivitiesAdminPageBinding
    val arrayOfInt = arrayOf(3, 5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitiesAdminPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOfInt)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.noOfPartSpinner.adapter = adapter


        fetchWebSocketUrl()

        binding.webSocketUrl.setOnClickListener {
            openWebSocketDialog()
        }

        fetchPhMode()

        binding.noOfPartSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val selectedItem = parent.getItemAtPosition(position) as Int

                    SharedPref.saveData(
                        this@AdminActivities,

                        SharedKeys.Ph_Mode_Key,
                        selectedItem.toString()
                    )

                    PhCalibFragmentNew.ph_mode_selected = selectedItem
                    PhCalibFragmentNew.PH_MODE = selectedItem.toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do something when nothing is selected
                }
            }


    }

    private fun fetchPhMode() {
        if (SharedPref.getSavedData(
                this@AdminActivities,
                SharedKeys.Ph_Mode_Key
            ) != null && SharedPref.getSavedData(
                this@AdminActivities,
                SharedKeys.Ph_Mode_Key
            ) != ""
        ) {
            val data =
                SharedPref.getSavedData(this@AdminActivities, SharedKeys.Ph_Mode_Key)

            PhCalibFragmentNew.ph_mode_selected = data.toInt()

            if (data.toInt() == 5) {
                PhCalibFragmentNew.PH_MODE = "5"
            }
            if (data.toInt() == 3) {
                PhCalibFragmentNew.PH_MODE = "3"
            }

            for (d in arrayOfInt.indices) {
                if (arrayOfInt[d].toString() == data) {
                    binding.noOfPartSpinner.setSelection(d)
                }
            }
        } else {
            SharedPref.saveData(this@AdminActivities, SharedKeys.Ph_Mode_Key, "5")
            PhCalibFragmentNew.ph_mode_selected = 5
            PhCalibFragmentNew.PH_MODE = "5"

        }

    }

    private fun fetchWebSocketUrl() {
        val websocketUrl = SharedPref.getSavedData(this@AdminActivities, "WEBSOCKET_URL")
        if (websocketUrl != null) {
            if (websocketUrl != "") {
                Source.WEBSOCKET_URL = websocketUrl
                binding.websocketUrlText.text = Source.WEBSOCKET_URL

            } else {
                Source.WEBSOCKET_URL = "ws://192.168.4.1:81"
                binding.websocketUrlText.text = Source.WEBSOCKET_URL

                SharedPref.saveData(this@AdminActivities, "WEBSOCKET_URL", "ws://192.168.4.1:81")
            }
        } else {
            Source.WEBSOCKET_URL = "ws://192.168.4.1:81"

            binding.websocketUrlText.text = Source.WEBSOCKET_URL
            SharedPref.saveData(this@AdminActivities, "WEBSOCKET_URL", "ws://192.168.4.1:81")

        }
    }

    private fun openWebSocketDialog() {
        // In your activity or fragment
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.developer_dialog)

        val btnSave = dialog.findViewById<Button>(R.id.btnSave)
        val etWebSocketUrl = dialog.findViewById<TextInputEditText>(R.id.webSocketUrlTxt)
        val etPassword = dialog.findViewById<TextInputEditText>(R.id.devPassword)

        etWebSocketUrl.setText(Source.WEBSOCKET_URL)

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
                if (password == Source.ADMIN_PASSWORD) {
                    Source.WEBSOCKET_URL = webSocketUrl
                    binding.websocketUrlText.text = webSocketUrl
                    SharedPref.saveData(this@AdminActivities, "WEBSOCKET_URL", webSocketUrl)

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