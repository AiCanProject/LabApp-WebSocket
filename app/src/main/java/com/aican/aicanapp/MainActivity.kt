package com.aican.aicanapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.aican.aicanapp.websocket.WebSocketManager

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        WebSocketManager.setMessageListener { message ->
            // Handle incoming message here
            runOnUiThread {
                // Update UI with received message
                Log.e("ThisIsNotErrorOnMain", message)
//                textView.text = message
            }
        }

    }
}