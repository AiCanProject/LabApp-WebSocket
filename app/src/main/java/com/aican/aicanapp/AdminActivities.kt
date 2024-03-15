package com.aican.aicanapp

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.adapters.ProductListAdapter
import com.aican.aicanapp.databinding.ActivitiesAdminPageBinding
import com.aican.aicanapp.ph.PhActivity
import com.aican.aicanapp.ph.phFragment.PhFragment
import com.aican.aicanapp.roomDatabase.daoObjects.ProductsListDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.ProductEntity
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.ProductViewModel
import com.aican.aicanapp.viewModels.ProductViewModelFactory
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class AdminActivities : AppCompatActivity() {

    lateinit var binding: ActivitiesAdminPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitiesAdminPageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        fetchWebSocketUrl()

        binding.webSocketUrl.setOnClickListener {
            openWebSocketDialog()
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
                if (password == "12345678") {
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