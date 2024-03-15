package com.aican.aicanapp

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.LocationManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.AddDevice.AddDeviceOption
import com.aican.aicanapp.Authentication.AdminLoginActivity
import com.aican.aicanapp.FirebaseAccounts.PrimaryAccount
import com.aican.aicanapp.adapters.NewPhAdapter
import com.aican.aicanapp.adapters.PhAdapter
import com.aican.aicanapp.dataClasses.PhDevice
import com.aican.aicanapp.databinding.ActivityDashboardBinding
import com.aican.aicanapp.dialogs.EditNameDialog
import com.aican.aicanapp.dialogs.EditNameDialog.OnNameChangedListener
import com.aican.aicanapp.interfaces.DashboardListsOptionsClickListener
import com.aican.aicanapp.ph.phFragment.PhCalibFragmentNew
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.viewModels.SharedViewModel
import com.aican.aicanapp.websocket.WebSocketManager
import com.aican.aicanapp.websocket.webViewModel.SocketViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File
import java.net.URI


class Dashboard : AppCompatActivity(), DashboardListsOptionsClickListener, OnNameChangedListener {

    companion object {
        const val TAG = "Dashboard"
        const val KEY_DEVICE_ID = "device_id"
        const val GRAPH_PLOT_DELAY = 15000
        const val DEVICE_TYPE_PH = "PHMETER"
        const val DEVICE_TYPE_PUMP = "P_PUMP"
        const val DEVICE_TYPE_TEMP = "TEMP_CONTROLLER"
        const val DEVICE_TYPE_COOLING = "PELTIER"
        const val DEVICE_TYPE_EC = "ECMETER"
    }

    lateinit var phDev: CardView
    lateinit var tempDev: CardView
    lateinit var IndusDev: CardView
    lateinit var peristalticDev: CardView
    lateinit var ecDev: CardView
    lateinit var file: File
    lateinit var fileDestination: File
    lateinit var primaryDatabase: DatabaseReference

    private val databaseReference: DatabaseReference by lazy {
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true)

        FirebaseDatabase.getInstance().getReference("NEW_USERS")
    }

    lateinit var deviceRef: DatabaseReference
    lateinit var offlineMode: Switch
    lateinit var offlineModeSwitch: Switch
    lateinit var jsonData: JSONObject
    lateinit var connectedDeviceSSID: TextView
    lateinit var uid: String

    //    lateinit var device: PhDevice
    lateinit var refreshWifi: ImageView
    lateinit var mUid: String
    lateinit var setting: Button
    lateinit var export: Button
    lateinit var internetStatus: TextView
    lateinit var locationD: TextView
    lateinit var weather: TextView
    lateinit var batteryPercentage: TextView
    lateinit var phAdapter: PhAdapter
    lateinit var newPhAdapter: NewPhAdapter

    lateinit var deviceIds: ArrayList<String>
    lateinit var deviceIdIds: HashMap<String, String>
    lateinit var deviceTypes: HashMap<String, String>
    lateinit var deviceNames: HashMap<String, String>

    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle

    lateinit var tempRecyclerView: RecyclerView
    lateinit var coolingRecyclerView: RecyclerView
    lateinit var phRecyclerView: RecyclerView
    lateinit var pumpRecyclerView: RecyclerView
    lateinit var ecRecyclerView: RecyclerView

    lateinit var addNewDevice: FloatingActionButton
    lateinit var tvTemp: TextView
    lateinit var tvCooling: TextView
    lateinit var tvPump: TextView
    lateinit var tvPh: TextView
    lateinit var tvName: TextView
    lateinit var tvConnectDevice: TextView
    lateinit var tvInstruction: TextView
    lateinit var ivLogout: ImageView
    lateinit var onlineStatus: LinearLayout
    lateinit var offlineStatus: LinearLayout

    val REQUEST_CODE_ASK_PERMISSIONS = 123

    lateinit var binding: ActivityDashboardBinding
    private var webSocketConnected = false
    lateinit var phDevices: java.util.ArrayList<PhDevice>
    lateinit var newPhDevices: ArrayList<PhDevice>
    private val sharedViewModel: SharedViewModel by viewModels()
    lateinit var viewModel: SocketViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        newPhDevices = ArrayList()
        phRecyclerView = binding.phRecyclerview
        setting = binding.settings
        tempRecyclerView = binding.tempRecyclerview
        coolingRecyclerView = binding.coolingRecyclerview
        pumpRecyclerView = binding.pumpRecyclerview
        tempDev = binding.tempDev
        phDev = binding.phDev
        IndusDev = binding.indusPhDev
        peristalticDev = binding.peristalticDev


        phDev = binding.phDev

        binding.settingPage.setOnClickListener {

            val intent = Intent(this@Dashboard, AdminLoginActivity::class.java)
            intent.putExtra("checkBtn", "adminSettings")
            startActivity(intent)
        }

        fetchWebSocketUrl()


        binding.offlineModeSwitch.setOnClickListener {
            webSocketConnected = if (binding.offlineModeSwitch.isChecked) {

//                val uri = URI("wss://socketsbay.com/wss/v2/1/demo/")
//                val uri = URI("ws://192.168.4.1:81")

                val uri = URI(Source.WEBSOCKET_URL)

                //{"DEVICE_ID": "EPT2020", "CAL_MODE": "1"}


//
//                WebSocketManager.disconnect()
                WebSocketManager.initializeWebSocket(uri,
//                    viewModel,
                    // Open listener
                    {
                        // WebSocket connection opened
                        runOnUiThread {
                            Source.SOCKET_CONNECTED = true
                            binding.socketConnected.visibility = View.VISIBLE
                            binding.socketDisconnected.visibility = View.GONE
                        }

//                        WebSocketManager.setMessageListener { message ->
//                            val jsonData = JSONObject(message)
//
//                            Toast.makeText(this@Dashboard, "" + message, Toast.LENGTH_SHORT).show()
//
//                            if (jsonData.has("PH_VAL") && jsonData.has("DEVICE_ID")) {
//                                var ph = 0.0f
//                                if (jsonData.getString("PH_VAL") != "nan" && PhFragment.validateNumber(
//                                        jsonData.getString(
//                                            "PH_VAL"
//                                        )
//                                    )
//                                ) {
//                                    ph = jsonData.getString("PH_VAL").toFloat()
//                                }
//                                val devID = jsonData.getString("DEVICE_ID")
//
//                                Log.e("ThisPHVAL", "PH $ph")
//                                newPhAdapter.refreshPh(ph, devID)
//
//                            }
//
//                            if (jsonData.has("TEMP_VAL") && jsonData.has("DEVICE_ID")) {
//                                var tem = 0.0f
//                                if (jsonData.getString("TEMP_VAL") != "nan" && PhFragment.validateNumber(
//                                        jsonData.getString(
//                                            "TEMP_VAL"
//                                        )
//                                    )
//                                ) {
//                                    tem = jsonData.getString("TEMP_VAL").toFloat()
//                                }
//                                val devID = jsonData.getString("DEVICE_ID")
//                                if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
//                                    newPhAdapter.refreshTemp(Math.round(tem), devID)
//                                }
//                            }
//                            if (jsonData.has("EC_VAL") && jsonData.has("DEVICE_ID")) {
//                                var ecVal = 0.0f
//                                if (jsonData.getString("EC_VAL") != "nan" && PhFragment.validateNumber(
//                                        jsonData.getString(
//                                            "EC_VAL"
//                                        )
//                                    )
//                                ) {
//                                    ecVal = jsonData.getString("EC_VAL").toFloat()
//                                }
//                                val devID = jsonData.getString("DEVICE_ID")
//
//
//
//                                if (Constants.OFFLINE_MODE && Constants.OFFLINE_DATA) {
//                                    phAdapter.refreshMv(ecVal, devID)
//                                }
//                            }
//
//
//                        }


                    },
                    // Close listener
                    { code, reason, remote ->
                        // WebSocket connection closed
                        // Handle UI or other actions as needed
                        runOnUiThread {
                            Source.SOCKET_CONNECTED = false
                            binding.offlineModeSwitch.isChecked = false
                            sharedViewModel.closeConnectionLiveData.value = "" + ""

                            binding.socketConnected.visibility = View.GONE
                            binding.socketDisconnected.visibility = View.VISIBLE
                        }
                    }
                )
                true
            } else {
                // Disconnect WebSocket
                runOnUiThread {

//                    Toast.makeText(this@Dashboard, "hjvvb", Toast.LENGTH_SHORT).show()
                    binding.socketConnected.visibility = View.GONE
                    binding.socketDisconnected.visibility = View.VISIBLE
                    WebSocketManager.disconnect(true)
                }
//                val inte = Intent(this@Dashboard, Dashboard::class.java)
//                startActivity(inte)
//                finish()
                false
            }
        }

        WebSocketManager.setMessageListener {
            runOnUiThread {
                Toast.makeText(this@Dashboard, "Message $it", Toast.LENGTH_SHORT).show()
                binding.monitorText.text = it
            }
        }

        WebSocketManager.setOpenListener {
            runOnUiThread {

                binding.socketConnected.visibility = View.VISIBLE
                binding.socketDisconnected.visibility = View.GONE
            }
        }
        WebSocketManager.setErrorListener { exception ->
            runOnUiThread {

                binding.socketConnected.visibility = View.GONE
                binding.socketDisconnected.visibility = View.VISIBLE
            }

        }

        WebSocketManager.setCloseListener { _, _, _ ->
            runOnUiThread {

                binding.socketConnected.visibility = View.GONE
                binding.socketDisconnected.visibility = View.VISIBLE
            }
        }

        binding.addNewDevice.setOnClickListener {
            val toAddDevice = Intent(this@Dashboard, AddDeviceOption::class.java)
            startActivity(toAddDevice)
        }

        binding.batteryPercent.setOnClickListener {
            startActivity(Intent(this@Dashboard, MainActivity::class.java))
        }
        Source.showLoading(this, false, false, "Loading Devices....", true)

        setUpNavDrawer()
        setUpToolBar()

        binding.ivLogout.setOnClickListener { v: View? ->
            val intent = Intent(this@Dashboard, AdminLoginActivity::class.java)
            intent.putExtra("checkBtn", "logout")
            startActivity(intent)
        }


        if (Build.VERSION.SDK_INT >= 33) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_VIDEO
                ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permissions are not granted, request them
                val STORAGE_PERMISSION_REQUEST_CODE = 1000
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_CODE_ASK_PERMISSIONS
                )
            } else {
                Toast.makeText(this, "granted", Toast.LENGTH_SHORT).show()

                // Permissions are already granted, proceed with using external storage
                // Your code for accessing external storage goes here
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permissions are not granted, request them
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_CODE_ASK_PERMISSIONS
                )
            } else {
                // Permissions are already granted, proceed with using external storage
                // Your code for accessing external storage goes here
            }
        }

        mUid = FirebaseAuth.getInstance(PrimaryAccount.getInstance(this)).uid!!
        primaryDatabase =
            FirebaseDatabase.getInstance(PrimaryAccount.getInstance(this)).reference.child("USERS")
                .child(mUid)

        deviceIds = java.util.ArrayList()
        phDevices = java.util.ArrayList<PhDevice>()
        deviceTypes = java.util.HashMap()
        deviceIdIds = java.util.HashMap()
        deviceNames = java.util.HashMap()

        jsonData = JSONObject()


        // battery percentage


        setUpNewPh()
        //showNetworkDialog();
        binding.phRecyclerview.visibility = View.VISIBLE
        binding.phDev.setCardBackgroundColor(Color.GRAY)
        binding.tempRecyclerview.visibility = View.GONE
        binding.coolingRecyclerview.visibility = View.GONE
        binding.pumpRecyclerview.visibility = View.GONE
        binding.ecRecyclerview.visibility = View.GONE


        //        Toast.makeText(this, "Size " + phDevices.size(), Toast.LENGTH_SHORT).show();
        phDev.setOnClickListener { //                Toast.makeText(Dashboard.this, "Size " + phDevices.size(), Toast.LENGTH_SHORT).show();

            //    showNetworkDialog();
            if (phDevices.size != 0) {
                binding.phRecyclerview.visibility = View.VISIBLE
            } else {
                binding.phRecyclerview.visibility = View.GONE
            }
            binding.tempRecyclerview.visibility = View.GONE
            binding.coolingRecyclerview.visibility = View.GONE
            binding.pumpRecyclerview.visibility = View.GONE
            binding.ecRecyclerview.visibility = View.GONE
            binding.phDev.setCardBackgroundColor(Color.GRAY)
            binding.tempDev.setCardBackgroundColor(Color.WHITE)
            binding.peristalticDev.setCardBackgroundColor(Color.WHITE)
            binding.indusPhDev.setCardBackgroundColor(Color.WHITE)
            binding.ecMeterDev.setCardBackgroundColor(Color.WHITE)
        }
        setting.setOnClickListener {
            val intent = Intent(this@Dashboard, AdminLoginActivity::class.java)
            intent.putExtra("checkBtn", "addUser")
            startActivity(intent)
        }

        binding.refreshWifi.setOnClickListener { v: View? ->
            if (isLocnEnabled(this)) {
                val ssid: String? = getCurrentSsid(this@Dashboard)
                Constants.wifiSSID = ssid
                binding.connectedDeviceSSID.text = ssid ?: "N/A"
            } else {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                val ssid: String? = getCurrentSsid(this@Dashboard)
                Constants.wifiSSID = ssid
                binding.connectedDeviceSSID.text = ssid ?: "N/A"
            }
            if (isConnected) {
                binding.internetStatus.text = "Active"
                binding.internetStatus.setTextColor(resources.getColor(R.color.internetActive))
            } else {
                binding.internetStatus.text = "Inactive"
                binding.internetStatus.setTextColor(resources.getColor(R.color.internetInactive))
            }
        }


    }

    private fun fetchExportConditions() {
        val exportCsvEnabled = SharedPref.getSavedData(this@Dashboard, "EXPORT_CSV")
        val exportPdfEnabled = SharedPref.getSavedData(this@Dashboard, "EXPORT_PDF")

        if (exportCsvEnabled != null && exportCsvEnabled != "") {
            Source.EXPORT_CSV = exportCsvEnabled == "true"
        } else {
            SharedPref.saveData(this@Dashboard, "EXPORT_CSV", "false")
            Source.EXPORT_CSV = false
        }

        if (exportPdfEnabled != null && exportPdfEnabled != "") {
            Source.EXPORT_PDF = exportPdfEnabled == "true"

        } else {
            Source.EXPORT_PDF = false
            SharedPref.saveData(this@Dashboard, "EXPORT_PDF", "true")
        }
    }

    private fun fetchWebSocketUrl() {
        val websocketUrl = SharedPref.getSavedData(this@Dashboard, "WEBSOCKET_URL")
        if (websocketUrl != null) {
            if (websocketUrl != "") {
                Source.WEBSOCKET_URL = websocketUrl
            } else {
                Source.WEBSOCKET_URL = "ws://192.168.4.1:81"
                SharedPref.saveData(this@Dashboard, "WEBSOCKET_URL", "ws://192.168.4.1:81")
            }
        } else {
            Source.WEBSOCKET_URL = "ws://192.168.4.1:81"
            SharedPref.saveData(this@Dashboard, "WEBSOCKET_URL", "ws://192.168.4.1:81")

        }
    }

    fun setUpNewPh() {

//        Toast.makeText(this@Dashboard, "" + uid, Toast.LENGTH_SHORT).show()

//        FirebaseDatabase.getInstance().setPersistenceEnabled(true)


        databaseReference.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    newPhDevices.clear()

                    val cfr = snapshot.child("subscription").value as String
                    if (cfr == "na") {
                        setting.visibility = View.GONE

                        Source.cfr_mode = false
                        val dialog = Dialog(this@Dashboard)
                        dialog.setContentView(R.layout.no_subscription)
                        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.setCancelable(false)
                        dialog.findViewById<View>(R.id.contactWith)
                            .setOnClickListener { finishAffinity() }
                        dialog.show()
                    }
                    if ((cfr == "non cfr") || (cfr == "non_cfr")
                        || (cfr == "nonCfr") || cfr ==
                        "noncfr"
                        || (cfr == "Non Cfr") || (cfr == "Non cfr")
                    ) {
                        Source.cfr_mode = false
                        setting.visibility = View.GONE
                    }
                    if (cfr == "cfr") {
                        Source.cfr_mode = true
                        setting.visibility = View.VISIBLE

                    }

                    for (data in snapshot.child("DEVICES").children) {


                        newPhDevices.add(
                            PhDevice(
                                data.getValue(String::class.java).toString(),
                                "", 0f, 0f, 0, 0L, 0
                            )
                        )
                    }

                    if (newPhDevices.size <= 0) {
                        Toast.makeText(this@Dashboard, "No device added", Toast.LENGTH_SHORT).show()
                    }
                    Source.cancelLoading()


                    phRecyclerView.layoutManager =
                        LinearLayoutManager(this@Dashboard, LinearLayoutManager.HORIZONTAL, false)
                    newPhAdapter = NewPhAdapter(
                        this@Dashboard,
                        newPhDevices,
                        this@Dashboard
                    )

//        Toast.makeText(this, "Size " + phDevices.size(), Toast.LENGTH_SHORT).show();
                    phRecyclerView.adapter = newPhAdapter
                    phRecyclerView.itemAnimator = Dashboard.NoAnimationItemAnimator()

                    // Update the existing adapter's data and notify the changes
                }

//                Toast.makeText(this@Dashboard, "" + newPhDevices.size, Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AllVideosFragment", "Firebase data retrieval canceled: ${error.message}")
            }
        })


    }


    override fun onStart() {
        super.onStart()
        if (isLocnEnabled(this)) {
            val ssid: String? = getCurrentSsid(this@Dashboard)
            Constants.wifiSSID = ssid
            binding.connectedDeviceSSID.text = ssid ?: "N/A"
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            val ssid: String? = getCurrentSsid(this@Dashboard)
            Constants.wifiSSID = ssid
            binding.connectedDeviceSSID.text = ssid ?: "N/A"
        }
    }

    var isConnected = false

    fun isLocnEnabled(context: Context): Boolean {
        var locnProviders: List<*>? = null
        try {
            val lm =
                context.applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
            locnProviders = lm.getProviders(true)
            return locnProviders.isNotEmpty()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            if (BuildConfig.DEBUG) {
                if (locnProviders == null || locnProviders.isEmpty()) Log.d(
                    TAG,
                    "Location services disabled"
                ) else Log.d(
                    TAG,
                    "locnProviders: $locnProviders"
                )
            }
        }
        return false
    }

    fun getCurrentSsid(context: Context): String? {
        var ssid: String? = null
        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo
        wifiInfo = wifiManager.connectionInfo
        if (wifiInfo.supplicantState == SupplicantState.COMPLETED) {
            ssid = wifiInfo.ssid
        }
        return ssid
    }


    //Toolbar------------------------------------------------------------------------------------------------------
    fun setUpToolBar() {
        setSupportActionBar(binding.mainToolbar)
        actionBarDrawerToggle =
            ActionBarDrawerToggle(
                this,
                binding.drawerLayout,
                binding.mainToolbar,
                R.string.app_name,
                R.string.app_name
            )
        binding.drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
    }


    class NoAnimationItemAnimator : DefaultItemAnimator() {
        override fun animateChange(
            oldHolder: RecyclerView.ViewHolder,
            newHolder: RecyclerView.ViewHolder,
            fromX: Int,
            fromY: Int,
            toX: Int,
            toY: Int
        ): Boolean {
            dispatchChangeFinished(oldHolder, true)
            dispatchChangeFinished(newHolder, false)
            return false
        }
    }

    private fun setUpNavDrawer() {
        val uid = FirebaseAuth.getInstance(PrimaryAccount.getInstance(this)).uid
        this.uid = uid.toString()
        FirebaseFirestore.getInstance(PrimaryAccount.getInstance(this))
            .collection("NAMES").document(uid!!).get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                binding.tvName.text = documentSnapshot.get(
                    "NAME",
                    String::class.java
                )
            }
    }

    private fun fetchPhMode() {
        if (SharedPref.getSavedData(
                this@Dashboard,
                SharedKeys.Ph_Mode_Key
            ) != null && SharedPref.getSavedData(
                this@Dashboard,
                SharedKeys.Ph_Mode_Key
            ) != ""
        ) {
            val data =
                SharedPref.getSavedData(this@Dashboard, SharedKeys.Ph_Mode_Key)

            PhCalibFragmentNew.ph_mode_selected = data.toInt()

            if (data.toInt() == 5) {
                PhCalibFragmentNew.PH_MODE = "5"
            }
            if (data.toInt() == 3) {
                PhCalibFragmentNew.PH_MODE = "3"
            }

        } else {
            SharedPref.saveData(this@Dashboard, SharedKeys.Ph_Mode_Key, "5")
            PhCalibFragmentNew.ph_mode_selected = 5
            PhCalibFragmentNew.PH_MODE = "5"

        }

    }



    override fun onResume() {
        super.onResume()

        fetchExportConditions()

        fetchPhMode()

        WebSocketManager.setMessageListener { message ->
            runOnUiThread {
                Log.e("ThisIsNotError", message)
            }
        }

        val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val tabBatteryPer = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            binding.batteryPercent.text = "$tabBatteryPer%"
        }

        if (WebSocketManager.WEBSOCKET_CONNECTED) {
            binding.offlineModeSwitch.isChecked = true
            Source.SOCKET_CONNECTED = true
            binding.socketConnected.visibility = View.VISIBLE
            binding.socketDisconnected.visibility = View.GONE
        }
    }

    override fun onOptionsIconClicked(view: View?, deviceId: String?) {
        val menu = PopupMenu(this, view)
        menu.menuInflater.inflate(R.menu.device_options, menu.menu)
        menu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.menuRemoveDevice) {

                return@setOnMenuItemClickListener true
            } else if (item.itemId == R.id.menuRename) {
                val dialog = EditNameDialog(
                    deviceId,
                    deviceTypes[deviceId],
                    deviceNames[deviceId],
                    this
                )
                dialog.show(supportFragmentManager, null)
            }
            false
        }
        menu.show()
    }

    override fun onNameChanged(deviceId: String?, type: String?, newName: String?) {
        FirebaseDatabase.getInstance(FirebaseApp.getInstance(deviceId!!)).reference
            .child(type!!).child(deviceId!!).child("NAME").setValue(newName)
        Log.e("CallingTwMKTLKKRes", "CAA")

    }

    //Pump RC------------------------------------------------------------------------------------------------------


    lateinit var device: PhDevice

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

}