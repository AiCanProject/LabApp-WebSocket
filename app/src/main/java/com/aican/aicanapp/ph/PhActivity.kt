package com.aican.aicanapp.ph

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.aican.aicanapp.Dashboard
import com.aican.aicanapp.ProbeScanner
import com.aican.aicanapp.R
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.ActivityPhBinding
import com.aican.aicanapp.dialogs.UserAuthDialog
import com.aican.aicanapp.ph.phFragment.PhAlarmFragment
import com.aican.aicanapp.ph.phFragment.PhCalibFragmentNew
import com.aican.aicanapp.ph.phFragment.PhFragment
import com.aican.aicanapp.ph.phFragment.PhGraphFragment
import com.aican.aicanapp.ph.phFragment.PhLogFragment
import com.aican.aicanapp.roomDatabase.daoObjects.UserActionDao
import com.aican.aicanapp.roomDatabase.daoObjects.UserDao
import com.aican.aicanapp.roomDatabase.database.AppDatabase
import com.aican.aicanapp.roomDatabase.entities.UserActionEntity
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        var DEVICE_ID: String? = null
        var isReconnecting = false
        private const val CHECK_INTERVAL = 3000L // Check interval in milliseconds
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkWebSocketStatusRunnable = object : Runnable {
        override fun run() {
            // Check WebSocket status
            val isWebSocketConnected = WebSocketManager.WEBSOCKET_CONNECTED
            // Handle the WebSocket status as needed
            handleWebSocketStatus(isWebSocketConnected)
            // Repeat the check after a delay
            handler.postDelayed(this, CHECK_INTERVAL)
        }
    }

    fun addUserAction(action: String, ph: String, temp: String, mv: String, compound: String) {
        lifecycleScope.launch(Dispatchers.IO) {

            userActionDao.insertUserAction(
                UserActionEntity(
                    0, Source.getCurrentTime(), Source.getPresentDate(),
                    action, ph, temp, mv, compound, DEVICE_ID.toString()
                )
            )
        }
    }

    lateinit var userDao: UserDao
    lateinit var userActionDao: UserActionDao

    override fun onResume() {
        super.onResume()
        // Start checking WebSocket status when the activity is resumed
        handler.post(checkWebSocketStatusRunnable)


    }

    override fun onPause() {
        super.onPause()
        // Stop checking WebSocket status when the activity is paused
        handler.removeCallbacks(checkWebSocketStatusRunnable)
    }

    private fun handleWebSocketStatus(isConnected: Boolean) {
        // Handle WebSocket status here
        if (isConnected) {
            binding.socketConnected.visibility = View.VISIBLE
            binding.socketDisconnected.visibility = View.GONE


            binding.offlineModeSwitch.isChecked = true

            // WebSocket is connected
            // Perform actions accordingly
        } else {
            binding.socketConnected.visibility = View.GONE
            binding.socketDisconnected.visibility = View.VISIBLE


//            binding.offlineModeSwitch.isEnabled = true

            if (isReconnecting) {
                // Delay to prevent immediate disconnection after reconnection

            } else {
                binding.offlineModeSwitch.isChecked = false

            }


            // WebSocket is not connected
            // Perform actions accordingly
        }
    }


    lateinit var ph: TextView
    lateinit var calibrate: TextView
    lateinit var log: TextView
    lateinit var graph: TextView
    lateinit var alarm: TextView
    lateinit var tabItemPh: TextView
    lateinit var tabItemCalib: TextView

    lateinit var offlineMode: TextView
    lateinit var onlineMode: TextView
    lateinit var deviceIDTxt: TextView


    var phFragment: PhFragment = PhFragment()
    var phLogFragment: PhLogFragment = PhLogFragment()
    var phGraphFragment: PhGraphFragment = PhGraphFragment()
    var phAlarmFragment: PhAlarmFragment = PhAlarmFragment()
    var phCalibFragmentNew: PhCalibFragmentNew = PhCalibFragmentNew()
    lateinit var databaseHelper: DatabaseHelper
    lateinit var binding: ActivityPhBinding
    private val sharedViewModel: SharedViewModel by viewModels()

//    {"DEVICE_ID": "EPT2006","OFFSET":"5.3", "SLOPE": "102", "BATTERY": "16", "PH_VAL": "4.5"}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhBinding.inflate(layoutInflater)
        setContentView(binding.root)

        DEVICE_ID = intent.getStringExtra(Dashboard.KEY_DEVICE_ID)
        if (DEVICE_ID == null) {
            throw RuntimeException()
        }

        binding.deviceIDTxt.text = "Device ID: $DEVICE_ID"

//        WebSocketManager.setMessageListener {
//
//            runOnUiThread {
//                binding.monitorText.text = it
//                binding.monitorText.setTextColor(resources.getColor(R.color.normalColor))
//            }
//        }


        sharedViewModel.openConnectionLiveData.observe(this) {
            runOnUiThread {
                binding.socketConnected.visibility = View.VISIBLE
                binding.socketDisconnected.visibility = View.GONE

//                binding.offlineModeSwitch.isEnabled = false
                binding.offlineModeSwitch.isChecked = true


            }
        }

//        binding.offlineModeSwitch.isEnabled = !binding.offlineModeSwitch.isChecked

        binding.connectingLay.visibility = View.GONE

        binding.offlineModeSwitch.setOnClickListener {
            val uri = URI(Source.WEBSOCKET_URL)

            if (binding.offlineModeSwitch.isChecked && !WebSocketManager.WEBSOCKET_CONNECTED) {
                isReconnecting = true

                binding.connectingLay.visibility = View.VISIBLE


//                WebSocketManager.reconnect()

//                WebSocketManager.reconnecting()
                WebSocketManager.disconnect(true)

                WebSocketManager.initializeWebSocket(uri,
                    // Open listener
                    {
                        // WebSocket connection opened
                        runOnUiThread {

                            isReconnecting = false
                            binding.connectingLay.visibility = View.GONE

                            Source.SOCKET_CONNECTED = true
                            binding.socketConnected.visibility = View.VISIBLE
                            binding.socketDisconnected.visibility = View.GONE

//                            if (Source.fragmentActive == 1) {
//
//                                phFragment.webSocketInit()
//                            }
//                            if (Source.fragmentActive == 2) {
//                                phCalibFragmentNew.websocketData()
//
//                            }
//                            if (Source.fragmentActive == 3) {
//                                phLogFragment.webSocketConnection()
//
//                            }
//                            if (Source.fragmentActive == 4) {
//                                phGraphFragment.webSocketInit()
//
//                            }
//                            if (Source.fragmentActive == 5) {
//                                phAlarmFragment.webSocketInit()
//
//                            }


                        }


                    },
                    // Close listener
                    { code, reason, remote ->
                        // WebSocket connection closed
                        // Handle UI or other actions as needed
                        runOnUiThread {
//                            Source.SOCKET_CONNECTED = false
//                            binding.offlineModeSwitch.isChecked = false
                            sharedViewModel.closeConnectionLiveData.value = "" + ""
                            binding.connectingLay.visibility = View.GONE

                            binding.socketConnected.visibility = View.GONE
                            binding.socketDisconnected.visibility = View.VISIBLE
                        }
                    }
                )
            } else {
                binding.connectingLay.visibility = View.GONE

            }
//            binding.offlineModeSwitch.isEnabled = !binding.offlineModeSwitch.isChecked
        }


        sharedViewModel.messageLiveData.observe(this) { message ->
            runOnUiThread {
                binding.monitorText.text = message
                binding.socketConnected.visibility = View.VISIBLE
                binding.socketDisconnected.visibility = View.GONE
                binding.monitorText.setTextColor(resources.getColor(R.color.normalColor))
            }
        }

        sharedViewModel.closeConnectionLiveData.observe(this) {
            runOnUiThread {
                binding.socketConnected.visibility = View.GONE
                binding.socketDisconnected.visibility = View.VISIBLE
            }
        }

        sharedViewModel.errorLiveData.observe(this) { error ->
            runOnUiThread {
                binding.monitorText.text = error
                binding.monitorText.setTextColor(resources.getColor(R.color.warningColor))
            }
        }

//        WebSocketManager.setErrorListener {
//            runOnUiThread {
//                binding.monitorText.text = it.message
//                binding.monitorText.setTextColor(resources.getColor(R.color.warningColor))
//            }
//        }


        databaseHelper = DatabaseHelper(this@PhActivity)

        ph = binding.item1
        calibrate = binding.item2
        log = binding.item3
        graph = binding.item4
        alarm = binding.item5
        offlineMode = binding.socketConnected
        onlineMode = binding.socketDisconnected
        tabItemPh = binding.tabItemP
        tabItemCalib = binding.select2

        ph.setOnClickListener(this)
        calibrate.setOnClickListener(this)
        log.setOnClickListener(this)
        graph.setOnClickListener(this)
        alarm.setOnClickListener(this)

        onlineMode.visibility = View.VISIBLE
        offlineMode.visibility = View.GONE

        if (Constants.OFFLINE_DATA && Constants.OFFLINE_MODE) {
            onlineMode.visibility = View.GONE
            offlineMode.visibility = View.VISIBLE
        } else {
            onlineMode.visibility = View.VISIBLE
            offlineMode.visibility = View.GONE
            if (Constants.OFFLINE_MODE) {
            }
            if (Constants.OFFLINE_DATA) {
                onlineMode.visibility = View.GONE
                offlineMode.visibility = View.VISIBLE
            }
        }


        val i = intent.getStringExtra("refreshCalib")
        if (i != null) {
            if (i == "y") {
                tabItemPh.background = resources.getDrawable(R.drawable.backselect1)
                tabItemPh.visibility = View.INVISIBLE
                val select2 = findViewById<TextView>(R.id.select2)
                select2.background = resources.getDrawable(R.drawable.back_select2)
                loadFragments(phCalibFragmentNew)
                calibrate.setTextColor(Color.WHITE)
                ph.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
                //                Toast.makeText(this, "" + calibrate.getWidth(), Toast.LENGTH_SHORT).show();
//                int size = calibrate.getWidth();
//                tabItemPh.animate().x(size).setDuration(100);
            }
        } else {
            loadFragments(phFragment)
        }

        WebSocketManager.setCloseListener { i, s, b ->

        }

        userDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().userDao()

        userActionDao = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "aican-database"
        ).build().userActionDao()

        if (Source.cfr_mode) {
            val userAuthDialog = UserAuthDialog(this@PhActivity, userDao)
            userAuthDialog.showLoginDialog { isValidCredentials ->
                if (isValidCredentials) {
                    addUserAction(
                        "username: " + Source.userName + ", Role: " + Source.userRole +
                                ", entered ph main fragment", "", "", "", ""
                    )
                } else {
                    runOnUiThread {
                        Toast.makeText(this@PhActivity, "Invalid credentials", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }


    }

    override fun onBackPressed() {
        val count = supportFragmentManager.backStackEntryCount
        if (Source.auto_log == 0 && !Source.calibratingNow) {
//            Intent intent = new Intent(PhActivity.this, Dashboard.class);
//            startActivity(intent);
            finish()
        } else {
            Toast.makeText(
                this,
                "You cannot change fragment while logging / calibrating",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadFragments(fragment: Fragment?): Boolean {
        if (fragment != null) {
            Log.d("navigation", "loadFragments: Frag is loaded")
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainerView, fragment)
                .addToBackStack(null)
                .commit()
            return true
        }
        return false
    }

    override fun onClick(view: View) {
        if (Source.auto_log == 0 && !Source.calibratingNow) {
            val select2 = findViewById<TextView>(R.id.select2)
            select2.background = resources.getDrawable(R.drawable.backselect1)
            select2.visibility = View.INVISIBLE
            tabItemPh.visibility = View.VISIBLE
            tabItemPh.background = resources.getDrawable(R.drawable.back_select2)
            if (view.getId() == R.id.item1) {
                tabItemPh.animate().x(0f).duration = 100
                loadFragments(phFragment)
                ph!!.setTextColor(Color.WHITE)
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
            } else if (view.getId() == R.id.item2) {

//                loadFragments(phCalibFragment);
                loadFragments(phCalibFragmentNew)
                calibrate.setTextColor(Color.WHITE)
                ph!!.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
                val size = calibrate.width
                tabItemPh.animate().x(size.toFloat()).duration = 100
            } else if (view.getId() == R.id.item3) {
                loadFragments(phLogFragment)
                log.setTextColor(Color.WHITE)
                ph!!.setTextColor(Color.parseColor("#FF24003A"))
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
                val size = calibrate.width * 2
                tabItemPh.animate().x(size.toFloat()).duration = 100
            } else if (view.getId() == R.id.item4) {
                loadFragments(phGraphFragment)
                graph.setTextColor(Color.WHITE)
                ph!!.setTextColor(Color.parseColor("#FF24003A"))
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
                val size = calibrate.width * 3
                tabItemPh.animate().x(size.toFloat()).duration = 100
            } else if (view.getId() == R.id.item5) {
                loadFragments(phAlarmFragment)
                alarm.setTextColor(Color.WHITE)
                ph!!.setTextColor(Color.parseColor("#FF24003A"))
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                val size = calibrate.width * 4
                tabItemPh.animate().x(size.toFloat()).duration = 100
            } else if (view.getId() == R.id.cLProbes) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                databaseHelper.insert_action_data(
                    time,
                    date,
                    "Probe Scanner : " + Source.logUserName,
                    "",
                    "",
                    "",
                    "",
                    DEVICE_ID
                )
                val intent = Intent(this@PhActivity, ProbeScanner::class.java)
                intent.putExtra("activity", "PhFragment")
                //            intent.addFlags()
                startActivity(intent)
            }
        } else {
            Toast.makeText(
                this,
                "You cannot change fragment while calibrating / logging",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}