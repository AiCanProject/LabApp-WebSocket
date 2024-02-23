package com.aican.aicanapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
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
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aican.aicanapp.Authentication.AdminLoginActivity
import com.aican.aicanapp.FirebaseAccounts.DeviceAccount
import com.aican.aicanapp.FirebaseAccounts.PrimaryAccount
import com.aican.aicanapp.FirebaseAccounts.SecondaryAccount
import com.aican.aicanapp.adapters.PhAdapter
import com.aican.aicanapp.dataClasses.PhDevice
import com.aican.aicanapp.databinding.ActivityDashboardBinding
import com.aican.aicanapp.dialogs.EditNameDialog
import com.aican.aicanapp.dialogs.EditNameDialog.OnNameChangedListener
import com.aican.aicanapp.interfaces.DashboardListsOptionsClickListener
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.tubesock.WebSocket
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

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
//    lateinit var phDevices: ArrayList<PhDevice>
//    lateinit var pumpDevices: ArrayList<PumpDevice>
//    lateinit var tempDevices: ArrayList<TempDevice>
//    lateinit var coolingDevices: ArrayList<CoolingDevice>
//    lateinit var ecDevices: ArrayList<EcDevice>
//
//    lateinit var tempAdapter: TempAdapter
//    lateinit var coolingAdapter: CoolingAdapter
//    lateinit var phAdapter: PhAdapter
//    lateinit var ecAdapter: EcAdapter
//    lateinit var pumpAdapter: PumpAdapter
//    lateinit var databaseHelper: DatabaseHelper

    lateinit var phDev: CardView
    lateinit var tempDev: CardView
    lateinit var IndusDev: CardView
    lateinit var peristalticDev: CardView
    lateinit var ecDev: CardView
    lateinit var file: File
    lateinit var fileDestination: File
    lateinit var primaryDatabase: DatabaseReference
    lateinit var databaseReference: DatabaseReference
    lateinit var deviceRef: DatabaseReference
    lateinit var offlineMode: Switch
    lateinit var offlineModeSwitch: Switch
    lateinit var webSocket1: WebSocket
    lateinit var jsonData: JSONObject
    lateinit var connectedDeviceSSID: TextView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.offlineModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            webSocketConnected = if (isChecked) {
                // Connect to WebSocket
                val uri = URI("wss://socketsbay.com/wss/v2/1/demo/")
                WebSocketManager.initializeWebSocket(uri)
                true
            } else {
                // Disconnect WebSocket
                WebSocketManager.disconnect()
                false
            }
        }

        binding.batteryPercent.setOnClickListener {
            startActivity(Intent(this@Dashboard, MainActivity::class.java))
        }
        Source.showLoading(this, false, false, "Loading Devices....")

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


//        offlineMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                if (offlineMode.isChecked()) {
//                    Constants.OFFLINE_MODE = true;
//                    initiateSocketConnection();
//
//                } else {
//                    Constants.OFFLINE_MODE = false;
//                    webSocket1.cancel();
//                }
//            }
//        });

        // battery percentage
        val bm = applicationContext.getSystemService(BATTERY_SERVICE) as BatteryManager
        if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val tabBatteryPer = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            binding.batteryPercent.text = "$tabBatteryPer%"
        }

        setUpPh()
        refresh()
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

    //Cooling RC------------------------------------------------------------------------------------------------------
    fun setUpPh() {
        phRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        phAdapter = PhAdapter(
            this@Dashboard,
            phDevices,

            ) { view: View?, deviceId: String? -> this.onOptionsIconClicked(view, deviceId) }

//        Toast.makeText(this, "Size " + phDevices.size(), Toast.LENGTH_SHORT).show();
        phRecyclerView.adapter = phAdapter
        phRecyclerView.itemAnimator = Dashboard.NoAnimationItemAnimator()
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
        FirebaseFirestore.getInstance(PrimaryAccount.getInstance(this))
            .collection("NAMES").document(uid!!).get()
            .addOnSuccessListener { documentSnapshot: DocumentSnapshot ->
                binding.tvName.text = documentSnapshot.get(
                    "NAME",
                    String::class.java
                )
            }
    }


    override fun onResume() {
        super.onResume()

        WebSocketManager.setMessageListener { message ->
            runOnUiThread {
                Log.e("ThisIsNotError", message)
            }
        }
    }

    override fun onOptionsIconClicked(view: View?, deviceId: String?) {
        val menu = PopupMenu(this, view)
        menu.menuInflater.inflate(R.menu.device_options, menu.menu)
        menu.setOnMenuItemClickListener { item: MenuItem ->
            if (item.itemId == R.id.menuRemoveDevice) {
                val uid =
                    FirebaseAuth.getInstance(PrimaryAccount.getInstance(this)).uid
                if (uid != null && deviceIdIds.containsKey(deviceId)) {
                    FirebaseDatabase.getInstance(PrimaryAccount.getInstance(this)).reference
                        .child("USERS").child(uid).child("DEVICES")
                        .child(deviceIdIds[deviceId]!!).removeValue()
                        .addOnSuccessListener { d: Void? ->
                            FirebaseFirestore.getInstance().collection("Devices Registered")
                                .document(
                                    deviceId!!
                                ).delete()
                                .addOnSuccessListener {
                                    Log.e("CallingTwMKTLKK", "CAA")
                                    refresh()
                                }.addOnFailureListener { e ->
                                    Log.w(
                                        TAG,
                                        "Error deleting document",
                                        e
                                    )
                                }
                        }
                }
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

        refresh()
    }

    //Pump RC------------------------------------------------------------------------------------------------------
    private fun refresh() {
        deviceIds.clear()
        phDevices.clear()
        deviceIdIds.clear()
        getDeviceIds()
    }

    private fun getDeviceIds() {
        primaryDatabase.child("DEVICES").get().addOnSuccessListener { dataSnapshot: DataSnapshot ->
//            Toast.makeText(this, "Hello", Toast.LENGTH_SHORT).show();
            if (dataSnapshot.hasChildren()) {
                for (deviceSnapshot in dataSnapshot.children) {
                    val deviceId =
                        deviceSnapshot.getValue(String::class.java)
                    if (!deviceIds.contains(deviceId)) {
//                        Toast.makeText(this, "Set", Toast.LENGTH_SHORT).show();
                        deviceIds.add(deviceId!!)
                        deviceIdIds[deviceId] = deviceSnapshot.key!!
                    }
                    //                    deviceIds.add(deviceId);
//                    deviceIdIds.put(deviceId, deviceSnapshot.getKey());
                }
                //                Log.e("CallingTwMK","CAA");
                getDeviceAccounts()
            }
        }
    }

    private fun getDeviceAccounts() {
        val accountsLoaded = AtomicInteger()
        val secondaryDatabase =
            FirebaseDatabase.getInstance(SecondaryAccount.getInstance(this)).reference
        for (id in deviceIds) {
            secondaryDatabase.child(id).get().addOnSuccessListener { dataSnapshot: DataSnapshot ->
                accountsLoaded.incrementAndGet()
                val deviceAccount =
                    dataSnapshot.getValue<DeviceAccount>(DeviceAccount::class.java)
                        ?: return@addOnSuccessListener
                deviceTypes[id] = deviceAccount.type
                initialiseFirebaseForDevice(id, deviceAccount)
                if (accountsLoaded.get() == deviceIds.size) {
                    Log.e("CallingTw", "C")
                    //                    Toast.makeText(Dashboard.this, "Loaded : " + deviceAccount, Toast.LENGTH_SHORT).show();
                    getDevices()
                }
            }.addOnFailureListener { exception: Exception -> exception.printStackTrace() }
                .addOnCanceledListener {
                    Log.d(
                        "TAG",
                        "onCanceled: "
                    )
                }
        }
    }

    private fun initialiseFirebaseForDevice(deviceId: String, deviceAccount: DeviceAccount) {
        val firebaseOptions = FirebaseOptions.Builder()
            .setApiKey(deviceAccount.api)
            .setApplicationId(deviceAccount.app)
            .setDatabaseUrl(deviceAccount.database)
            .setProjectId(deviceAccount.project)
            .build()
        try {
            val app = FirebaseApp.initializeApp(this, firebaseOptions, deviceId)
            FirebaseDatabase.getInstance(app).setPersistenceEnabled(true)
        } catch (e: IllegalStateException) {
            //Ignore
        }
    }

    lateinit var device: PhDevice
    private fun getDevices() {
        phDevices.clear()
        Log.d(TAG, "Device IDs: $deviceIds")
        Log.d(TAG, "Device IDs: " + deviceIdIds.toString() + " Sii " + deviceIdIds.size)
        val devicesLoaded = AtomicInteger()
        for (id in deviceIds) {
            val app = FirebaseApp.getInstance(id)
            FirebaseDatabase.getInstance(app).reference.child(deviceTypes[id]!!).child(id).get()
                .addOnSuccessListener { dataSnapshot: DataSnapshot ->
                    devicesLoaded.incrementAndGet()
                    val data = dataSnapshot.child("Data")
                    val ui = dataSnapshot.child("UI")
                    val name =
                        dataSnapshot.child("NAME").getValue(String::class.java)
                    var offline = 0
                    offline = if (dataSnapshot.child("offline").exists()) {
                        dataSnapshot.child("offline").getValue(Int::class.java)!!
                    } else {
                        0
                    }
                    deviceNames[id] = name!!
                    when (deviceTypes[id]) {
                        "PHMETER" -> {
                            device = PhDevice(
                                id,
                                name,
                                data.child("PH_VAL")
                                    .getValue<Float>(Float::class.java)!!,
                                data.child("EC_VAL")
                                    .getValue<Float>(Float::class.java)!!,
                                data.child("TEMP_VAL").getValue<Int>(Int::class.java)!!,
                                data.child("TDS_VAL")
                                    .getValue<Long>(Long::class.java)!!, offline
                            )
                            Source.cancelLoading()

//                        Toast.makeText(Dashboard.this, "Loaded : " + data.child("PH_VAL").getValue(Float.class), Toast.LENGTH_SHORT).show();
                            phDevices.add(device)
                        }
                    }
                    if (devicesLoaded.get() == deviceIds.size) {

                        phAdapter.notifyDataSetChanged()

                        if (phDevices.size != 0) {
                            phRecyclerView.visibility = View.VISIBLE
                            phDev.setCardBackgroundColor(Color.GRAY)
                        } else {
                            phRecyclerView.visibility = View.GONE
                        }
                        tempRecyclerView.visibility = View.GONE
                        coolingRecyclerView.visibility = View.GONE
                        pumpRecyclerView.visibility = View.GONE
                        //                    ecRecyclerView.setVisibility(View.GONE);
                        phDev.setCardBackgroundColor(Color.GRAY)
                        tempDev.setCardBackgroundColor(Color.WHITE)
                        peristalticDev.setCardBackgroundColor(Color.WHITE)
                        IndusDev.setCardBackgroundColor(Color.WHITE)
                    }
                }
        }
    }

}