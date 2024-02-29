package com.aican.aicanapp.ph

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.aican.aicanapp.Dashboard
import com.aican.aicanapp.ProbeScanner
import com.aican.aicanapp.R
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.ActivityPhBinding
import com.aican.aicanapp.ph.phFragment.PhAlarmFragment
import com.aican.aicanapp.ph.phFragment.PhCalibFragmentNew
import com.aican.aicanapp.ph.phFragment.PhFragment
import com.aican.aicanapp.ph.phFragment.PhGraphFragment
import com.aican.aicanapp.ph.phFragment.PhLogFragment
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.SharedViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.annotations.NotNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        var DEVICE_ID: String? = null
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
    lateinit var notice: TextView
    lateinit var offlineModeSwitch: Switch


    lateinit var deviceRef: DatabaseReference

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

//        WebSocketManager.setMessageListener {
//
//            runOnUiThread {
//                binding.monitorText.text = it
//                binding.monitorText.setTextColor(resources.getColor(R.color.normalColor))
//            }
//        }

        sharedViewModel.closeConnectionLiveData.observe(this) {
            binding.socketConnected.visibility = View.GONE
            binding.socketDisconnected.visibility = View.VISIBLE
        }

        sharedViewModel.openConnectionLiveData.observe(this) {

            binding.socketConnected.visibility = View.VISIBLE
            binding.socketDisconnected.visibility = View.GONE
        }

        sharedViewModel.messageLiveData.observe(this) { message ->
            runOnUiThread {
                binding.monitorText.text = message
                binding.socketConnected.visibility = View.VISIBLE
                binding.socketDisconnected.visibility = View.GONE
                binding.monitorText.setTextColor(resources.getColor(R.color.normalColor))
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

        deviceRef =
            FirebaseDatabase.getInstance(FirebaseApp.getInstance(DEVICE_ID!!)).reference.child("PHMETER")
                .child(
                    DEVICE_ID!!
                )

        deviceRef.child("Data").child("AUTOLOG").setValue(0)

        deviceRef.child("Data").child("AUTOLOG")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(@NotNull snapshot: DataSnapshot) {
                    Source.auto_log = snapshot.getValue<Int>(Int::class.java)!!
                }

                override fun onCancelled(@NotNull error: DatabaseError) {}
            })
        databaseHelper = DatabaseHelper(this@PhActivity)

        offlineModeSwitch = findViewById<Switch>(R.id.offlineModeSwitch)
        ph = binding.item1
        calibrate = binding.item2
        log = binding.item3
        graph = binding.item4
        alarm = binding.item5
        offlineMode = binding.socketConnected
        onlineMode = binding.socketDisconnected
        notice = binding.notice
        tabItemPh = binding.tabItemP
        tabItemCalib = binding.select2

        ph.setOnClickListener(this)
        calibrate.setOnClickListener(this)
        log.setOnClickListener(this)
        graph.setOnClickListener(this)
        alarm.setOnClickListener(this)

        onlineMode.visibility = View.VISIBLE
        offlineMode.visibility = View.GONE
        notice.visibility = View.GONE

        if (Constants.OFFLINE_DATA && Constants.OFFLINE_MODE) {
            onlineMode.visibility = View.GONE
            offlineMode.visibility = View.VISIBLE
        } else {
            onlineMode.visibility = View.VISIBLE
            offlineMode.visibility = View.GONE
            if (Constants.OFFLINE_MODE) {
                notice.visibility = View.GONE
            }
            if (Constants.OFFLINE_DATA) {
                notice.visibility = View.VISIBLE
                notice.text = "Device is not connected"
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


        deviceRef =
            FirebaseDatabase.getInstance(FirebaseApp.getInstance(DEVICE_ID!!)).reference.child("PHMETER")
                .child(
                    DEVICE_ID!!
                )
        deviceRef.child("PH_MODE").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(@NotNull snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    PhCalibFragmentNew.PH_MODE =
                        snapshot.getValue<String>(String::class.java).toString()
                } else {
                    deviceRef.child("PH_MODE").setValue("both")
                    PhCalibFragmentNew.PH_MODE = "both"
                }
            }

            override fun onCancelled(@NotNull error: DatabaseError) {}
        })

        if (Constants.OFFLINE_DATA) {
            offlineModeSwitch.isChecked = true
            offlineModeSwitch.text = "Connected"
            offlineModeSwitch.setOnClickListener {
            }
        }

        offlineModeSwitch.visibility = View.GONE

    }

    override fun onBackPressed() {
        super.onBackPressed()
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
            deviceRef.child("UI").child("PH").child("PH_CAL").child("CAL").setValue(0)
            return true
        }
        return false
    }

    override fun onClick(view: View) {
        if (Source.auto_log === 0 && !Source.calibratingNow) {
            val select2 = findViewById<TextView>(R.id.select2)
            select2.background = resources.getDrawable(R.drawable.backselect1)
            select2.visibility = View.INVISIBLE
            tabItemPh.visibility = View.VISIBLE
            tabItemPh.background = resources.getDrawable(R.drawable.back_select2)
            if (view.getId() == R.id.item1) {
                offlineModeSwitch.visibility = View.VISIBLE
                tabItemPh.animate().x(0f).duration = 100
                loadFragments(phFragment)
                ph!!.setTextColor(Color.WHITE)
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
            } else if (view.getId() == R.id.item2) {
                offlineModeSwitch.visibility = View.VISIBLE

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
                offlineModeSwitch.visibility = View.VISIBLE
                loadFragments(phLogFragment)
                log.setTextColor(Color.WHITE)
                ph!!.setTextColor(Color.parseColor("#FF24003A"))
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                graph.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
                val size = calibrate.width * 2
                tabItemPh.animate().x(size.toFloat()).duration = 100
            } else if (view.getId() == R.id.item4) {
                offlineModeSwitch.visibility = View.GONE
                loadFragments(phGraphFragment)
                graph.setTextColor(Color.WHITE)
                ph!!.setTextColor(Color.parseColor("#FF24003A"))
                calibrate.setTextColor(Color.parseColor("#FF24003A"))
                log.setTextColor(Color.parseColor("#FF24003A"))
                alarm.setTextColor(Color.parseColor("#FF24003A"))
                val size = calibrate.width * 3
                tabItemPh.animate().x(size.toFloat()).duration = 100
            } else if (view.getId() == R.id.item5) {
                offlineModeSwitch.visibility = View.GONE
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
        offlineModeSwitch.visibility = View.GONE
    }

}