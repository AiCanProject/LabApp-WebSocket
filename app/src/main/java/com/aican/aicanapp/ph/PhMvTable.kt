package com.aican.aicanapp.ph

import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aican.aicanapp.R
import com.aican.aicanapp.data.DatabaseHelper
import com.aican.aicanapp.databinding.ActivityPhMvTableBinding
import com.aican.aicanapp.dialogs.EditPhBufferDialog
import com.aican.aicanapp.ph.phFragment.PhFragment
import com.aican.aicanapp.utils.Constants
import com.aican.aicanapp.utils.SharedKeys
import com.aican.aicanapp.utils.SharedPref
import com.aican.aicanapp.utils.Source
import com.aican.aicanapp.websocket.WebSocketManager
import com.google.firebase.database.DatabaseReference
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhMvTable : AppCompatActivity() {


    lateinit var binding: ActivityPhMvTableBinding


    private lateinit var ph1: TextView
    private lateinit var minMV1: TextView
    private lateinit var phEdit1: TextView
    private lateinit var ph2: TextView
    private lateinit var minMV2: TextView
    private lateinit var phEdit2: TextView
    private lateinit var ph3: TextView
    private lateinit var minMV3: TextView
    private lateinit var phEdit3: TextView
    private lateinit var ph4: TextView
    private lateinit var minMV4: TextView
    private lateinit var phEdit4: TextView
    private lateinit var ph5: TextView
    private lateinit var minMV5: TextView
    private lateinit var phEdit5: TextView
    private lateinit var maxMV1: TextView
    private lateinit var maxMV2: TextView
    private lateinit var maxMV3: TextView
    private lateinit var maxMV4: TextView
    private lateinit var maxMV5: TextView

    private lateinit var minMVEdit1: TextView
    private lateinit var minMVEdit2: TextView
    private lateinit var minMVEdit3: TextView
    private lateinit var minMVEdit4: TextView
    private lateinit var minMVEdit5: TextView
    private lateinit var maxMVEdit1: TextView
    private lateinit var maxMVEdit2: TextView
    private lateinit var maxMVEdit3: TextView
    private lateinit var maxMVEdit4: TextView
    private lateinit var maxMVEdit5: TextView

    private lateinit var monitorValTxt: TextView

    private lateinit var MIN_MV1: String
    private lateinit var MIN_MV2: String
    private lateinit var MIN_MV3: String
    private lateinit var MIN_MV4: String
    private lateinit var MIN_MV5: String
    private lateinit var MAX_MV1: String
    private lateinit var MAX_MV2: String
    private lateinit var MAX_MV3: String
    private lateinit var MAX_MV4: String
    private lateinit var MAX_MV5: String

    private lateinit var PH1: String
    private lateinit var PH2: String
    private lateinit var PH3: String
    private lateinit var PH4: String
    private lateinit var PH5: String

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var deviceRef: DatabaseReference

    private lateinit var tempValue: EditText
    private lateinit var setATC: Button


    private lateinit var jsonData: JSONObject


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhMvTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        monitorValTxt = findViewById<TextView>(R.id.monitorValTxt)
        maxMVEdit1 = findViewById<TextView>(R.id.maxMVEdit1)
        maxMVEdit2 = findViewById<TextView>(R.id.maxMVEdit2)
        maxMVEdit3 = findViewById<TextView>(R.id.maxMVEdit3)
        maxMVEdit4 = findViewById<TextView>(R.id.maxMVEdit4)
        maxMVEdit5 = findViewById<TextView>(R.id.maxMVEdit5)

        setATC = findViewById<Button>(R.id.setATC)
        tempValue = findViewById<EditText>(R.id.tempValue)

        minMVEdit1 = findViewById<TextView>(R.id.minMVEdit1)
        minMVEdit2 = findViewById<TextView>(R.id.minMVEdit2)
        minMVEdit3 = findViewById<TextView>(R.id.minMVEdit3)
        minMVEdit4 = findViewById<TextView>(R.id.minMVEdit4)
        minMVEdit5 = findViewById<TextView>(R.id.minMVEdit5)

        phEdit1 = findViewById<TextView>(R.id.phEdit1)
        phEdit2 = findViewById<TextView>(R.id.phEdit2)
        phEdit3 = findViewById<TextView>(R.id.phEdit3)
        phEdit4 = findViewById<TextView>(R.id.phEdit4)
        phEdit5 = findViewById<TextView>(R.id.phEdit5)

        jsonData = JSONObject()

        maxMV1 = findViewById<TextView>(R.id.maxMV1)
        maxMV2 = findViewById<TextView>(R.id.maxMV2)
        maxMV3 = findViewById<TextView>(R.id.maxMV3)
        maxMV4 = findViewById<TextView>(R.id.maxMV4)
        maxMV5 = findViewById<TextView>(R.id.maxMV5)

        minMV1 = findViewById<TextView>(R.id.minMV1)
        minMV2 = findViewById<TextView>(R.id.minMV2)
        minMV3 = findViewById<TextView>(R.id.minMV3)
        minMV4 = findViewById<TextView>(R.id.minMV4)
        minMV5 = findViewById<TextView>(R.id.minMV5)

        ph1 = findViewById<TextView>(R.id.ph1)
        ph2 = findViewById<TextView>(R.id.ph2)
        ph3 = findViewById<TextView>(R.id.ph3)
        ph4 = findViewById<TextView>(R.id.ph4)
        ph5 = findViewById<TextView>(R.id.ph5)

        tempValue = findViewById<EditText>(R.id.tempValue)

        phEdit1.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        phEdit2.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        phEdit3.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        phEdit4.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        phEdit5.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }

        maxMVEdit1.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        maxMVEdit2.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        maxMVEdit3.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        maxMVEdit4.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        maxMVEdit5.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }

        minMVEdit1.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        minMVEdit2.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        minMVEdit3.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        minMVEdit4.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }
        minMVEdit5.setOnClickListener { v: View ->
            this.onClick(
                v
            )
        }


        databaseHelper = DatabaseHelper(this@PhMvTable)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        databaseHelper.insert_action_data(
            time,
            date,
            "pHMvTable : " + Source.logUserName,
            "",
            "",
            "",
            "",
            PhActivity.DEVICE_ID
        )

        databaseHelper.deletePhBufferMVTable()
        insertIntoDB()


        if (SharedPref.getSavedData(this, "ATC_R_C") != null && SharedPref.getSavedData(
                this,
                "ATC_R_C"
            ) !== ""
        ) {
            tempValue.setText(SharedPref.getSavedData(this, "ATC_R_C"))
        } else {
            tempValue.setText("0.0")
        }

        setATC.setOnClickListener { v: View? ->
            if (tempValue.text.toString() != "") {
                val va = tempValue.text.toString().toFloat()
                SharedPref.saveData(this, "ATC_R_C", va.toString())
                if (Constants.OFFLINE_MODE) {
                    try {
                        jsonData.put("R_C", va)
                        jsonData.put("DEVICE_ID", PhActivity.DEVICE_ID)
                        WebSocketManager.sendMessage(jsonData.toString())
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }
                } else {
                    deviceRef.child("Data").child("T_SET").setValue(va)
                    databaseHelper.insert_action_data(
                        time,
                        date,
                        "Temperature offset at: " + tempValue.text + " set by " + Source.logUserName,
                        "",
                        "",
                        "",
                        "",
                        PhActivity.DEVICE_ID
                    )
                }
            }
        }




        webSocketInit()
        updateBufferMinMaxValues()
    }

    private fun updateBufferMinMaxValues() {

        binding.phEdit1.visibility = View.INVISIBLE

        binding.phEdit2.visibility = View.INVISIBLE
        binding.phEdit3.visibility = View.INVISIBLE
        binding.phEdit4.visibility = View.INVISIBLE
        binding.phEdit5.visibility = View.INVISIBLE

        val mMinMV1 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.minMV1 + PhActivity.DEVICE_ID)
        if (mMinMV1 != null) {
            minMV1.text = mMinMV1
        }
        val mMinMV2 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.minMV2 + PhActivity.DEVICE_ID)
        if (mMinMV2 != null) {
            minMV2.text = mMinMV2
        }

        val mMinMV3 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.minMV3 + PhActivity.DEVICE_ID)
        if (mMinMV3 != null) {
            minMV3.text = mMinMV3
        }

        val mMinMV4 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.minMV4 + PhActivity.DEVICE_ID)
        if (mMinMV4 != null) {
            minMV4.text = mMinMV4
        }

        val mMinMV5 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.minMV5 + PhActivity.DEVICE_ID)
        if (mMinMV5 != null) {
            minMV5.text = mMinMV5
        }
        val mMaxMV1 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.maxMV1 + PhActivity.DEVICE_ID)
        if (mMaxMV1 != null) {
            maxMV1.text = mMaxMV1
        }

        val mMaxMV2 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.maxMV2 + PhActivity.DEVICE_ID)
        if (mMaxMV2 != null) {
            maxMV2.text = mMaxMV2
        }

        val mMaxMV3 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.maxMV3 + PhActivity.DEVICE_ID)
        if (mMaxMV3 != null) {
            maxMV3.text = mMaxMV3
        }

        val mMaxMV4 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.maxMV4 + PhActivity.DEVICE_ID)
        if (mMaxMV4 != null) {
            maxMV4.text = mMaxMV4
        }

        val mMaxMV5 =
            SharedPref.getSavedData(this@PhMvTable, SharedKeys.maxMV5 + PhActivity.DEVICE_ID)
        if (mMaxMV5 != null) {
            maxMV5.text = mMaxMV5
        }

        val db = databaseHelper.writableDatabase
        val calibCSV5: Cursor = db.rawQuery("SELECT * FROM CalibOfflineDataFive", null)

        var index5 = 0
        while (calibCSV5.moveToNext()) {
            val ph = calibCSV5.getString(calibCSV5.getColumnIndex("PH"))
            val mv = calibCSV5.getString(calibCSV5.getColumnIndex("MV"))
            val date = calibCSV5.getString(calibCSV5.getColumnIndex("DT"))
            val slope = calibCSV5.getString(calibCSV5.getColumnIndex("SLOPE"))
            val pHAC = calibCSV5.getString(calibCSV5.getColumnIndex("pHAC"))
            val temperature1 = calibCSV5.getString(calibCSV5.getColumnIndex("temperature"))
            Log.d("Cursor Data", "PH: " + calibCSV5.getString(calibCSV5.getColumnIndex("PH")))
            if (index5 == 0) {
                PH1 = ph
                binding.ph1.text = ph
            }
            if (index5 == 1) {
                PH2 = ph
                binding.ph2.text = ph
            }
            if (index5 == 2) {
                PH3 = ph
                binding.ph3.text = ph
            }
            if (index5 == 3) {
                PH4 = ph
                binding.ph4.text = ph
            }
            if (index5 == 4) {
                PH5 = ph
                binding.ph5.text = ph
            }
            index5++
        }

    }

    private fun insertIntoDB() {
        databaseHelper.insertPHBuffer(1, "-", "-", "-", this@PhMvTable)
        databaseHelper.insertPHBuffer(2, "-", "-", "-", this@PhMvTable)
        databaseHelper.insertPHBuffer(3, "-", "-", "-", this@PhMvTable)
        databaseHelper.insertPHBuffer(4, "-", "-", "-", this@PhMvTable)
        databaseHelper.insertPHBuffer(5, "-", "-", "-", this@PhMvTable)
    }

    private fun updateDB() {
        databaseHelper.updateBufferData(
            1,
            ph1.text.toString(),
            minMV1.text.toString(),
            maxMV1.text.toString(),
            this@PhMvTable
        )
        databaseHelper.updateBufferData(
            2,
            ph2.text.toString(),
            minMV2.text.toString(),
            maxMV2.text.toString(),
            this@PhMvTable
        )
        databaseHelper.updateBufferData(
            3,
            ph3.text.toString(),
            minMV3.text.toString(),
            maxMV3.text.toString(),
            this@PhMvTable
        )
        databaseHelper.updateBufferData(
            4,
            ph4.text.toString(),
            minMV4.text.toString(),
            maxMV4.text.toString(),
            this@PhMvTable
        )
        databaseHelper.updateBufferData(
            5,
            ph5.text.toString(),
            minMV5.text.toString(),
            maxMV5.text.toString(),
            this@PhMvTable
        )
    }

    private fun onClick(v: View) {
        when (v.id) {
            R.id.phEdit1 -> {
                val dialog = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("PH1", String.valueOf(ph));
//                    myEdit.commit();
//                    ph1.setText(sharedPreferences.getString("PH1", ""));

//                    databaseHelper.updateBufferData(
//                        1,
//                        ph.toString(),
//                        minMV1.text.toString(),
//                        maxMV1.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog.show(supportFragmentManager, null)
            }

            R.id.phEdit2 -> {
                val dialog1 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("PH2", String.valueOf(ph));
//                    myEdit.commit();
//                    ph2.setText(sharedPreferences.getString("PH2", ""));

//                    databaseHelper.updateBufferData(
//                        2,
//                        ph.toString(),
//                        minMV2.text.toString(),
//                        maxMV2.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog1.show(supportFragmentManager, null)
            }

            R.id.phEdit3 -> {
                val dialog2 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("PH3", String.valueOf(ph));
//                    myEdit.commit();
//                    ph3.setText(sharedPreferences.getString("PH3", ""));

//                    databaseHelper.updateBufferData(
//                        3,
//                        ph.toString(),
//                        minMV3.text.toString(),
//                        maxMV3.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog2.show(supportFragmentManager, null)
            }

            R.id.phEdit4 -> {
                val dialog3 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("PH4", String.valueOf(ph));
//                    myEdit.commit();
//                    ph4.setText(sharedPreferences.getString("PH4", ""));

//                    databaseHelper.updateBufferData(
//                        4,
//                        ph.toString(),
//                        minMV4.text.toString(),
//                        maxMV4.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog3.show(supportFragmentManager, null)
            }

            R.id.phEdit5 -> {
                val dialog5 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("PH5", String.valueOf(ph));
//                    myEdit.commit();
//                    ph5.setText(sharedPreferences.getString("PH5", ""));

//                    databaseHelper.updateBufferData(
//                        5,
//                        ph.toString(),
//                        minMV5.text.toString(),
//                        maxMV5.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog5.show(supportFragmentManager, null)
            }

            R.id.minMVEdit1 -> {
                val dialog6 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("minMV1", String.valueOf(ph));
//                    myEdit.commit();
                    minMV1.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.minMV1 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )

//                    databaseHelper.updateBufferData(
//                        1,
//                        ph1.text.toString(),
//                        ph.toString(),
//                        maxMV1.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog6.show(supportFragmentManager, null)
            }

            R.id.minMVEdit2 -> {
                val dialog7 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("minMV2", String.valueOf(ph));
//                    myEdit.commit();
//                    minMV2.setText(sharedPreferences.getString("minMV2", ""));
                    minMV2.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.minMV2 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


//
//                    databaseHelper.updateBufferData(
//                        2,
//                        ph2.text.toString(),
//                        ph.toString(),
//                        maxMV2.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog7.show(supportFragmentManager, null)
            }

            R.id.minMVEdit3 -> {
                val dialog8 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("minMV3", String.valueOf(ph));
//                    myEdit.commit();
//                    minMV3.setText(sharedPreferences.getString("minMV3", ""));
//                    Toast.makeText(this, ph + "", Toast.LENGTH_SHORT).show();

                    minMV3.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.minMV3 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


//                    databaseHelper.updateBufferData(
//                        3,
//                        ph3.text.toString(),
//                        ph.toString(),
//                        maxMV3.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog8.show(supportFragmentManager, null)
            }

            R.id.minMVEdit4 -> {
                val dialog9 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("minMV4", String.valueOf(ph));
//                    myEdit.commit();
//                    minMV4.setText(sharedPreferences.getString("minMV4", ""));

                    minMV4.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.minMV4 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


//                    databaseHelper.updateBufferData(
//                        4,
//                        ph4.text.toString(),
//                        ph.toString(),
//                        maxMV4.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog9.show(supportFragmentManager, null)
            }

            R.id.minMVEdit5 -> {
                val dialog10 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("minMV5", String.valueOf(ph));
//                    myEdit.commit();
//                    minMV5.setText(sharedPreferences.getString("minMV5", ""));

                    minMV5.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.minMV5 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


//                    databaseHelper.updateBufferData(
//                        5,
//                        ph5.text.toString(),
//                        ph.toString(),
//                        maxMV5.text.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog10.show(supportFragmentManager, null)
            }

            R.id.maxMVEdit1 -> {
                val dialog11 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("maxMV1", String.valueOf(ph));
//                    myEdit.commit();
//
//                    maxMV1.setText(sharedPreferences.getString("maxMV1", ""));
                    maxMV1.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.maxMV1 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


//                    databaseHelper.updateBufferData(
//                        1,
//                        ph1.text.toString(),
//                        minMV1.text.toString(),
//                        ph.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog11.show(supportFragmentManager, null)
            }

            R.id.maxMVEdit2 -> {
                val dialog12 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("maxMV2", String.valueOf(ph));
//                    myEdit.commit();
//
//                    maxMV2.setText(sharedPreferences.getString("maxMV2", ""));
                    maxMV2.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.maxMV2 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


//                    databaseHelper.updateBufferData(
//                        2,
//                        ph2.text.toString(),
//                        minMV2.text.toString(),
//                        ph.toString(),
//                        this@PhMvTable
//                    )
                }
                dialog12.show(supportFragmentManager, null)
            }

            R.id.maxMVEdit3 -> {
                val dialog13 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("maxMV3", String.valueOf(ph));
//                    myEdit.commit();
//
//                    maxMV3.setText(sharedPreferences.getString("maxMV3", ""));
//
//                    databaseHelper.updateBufferData(
//                        3,
//                        ph3.text.toString(),
//                        minMV3.text.toString(),
//                        ph.toString(),
//                        this@PhMvTable
//                    )
                    maxMV3.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.maxMV3 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


                }
                dialog13.show(supportFragmentManager, null)
            }

            R.id.maxMVEdit4 -> {
                val dialog14 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("maxMV4", String.valueOf(ph));
//                    myEdit.commit();
//
//                    maxMV4.setText(sharedPreferences.getString("maxMV4", ""));
//
//                    databaseHelper.updateBufferData(
//                        4,
//                        ph4.text.toString(),
//                        minMV4.text.toString(),
//                        ph.toString(),
//                        this@PhMvTable
//                    )
                    maxMV4.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.maxMV4 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


                }
                dialog14.show(supportFragmentManager, null)
            }

            R.id.maxMVEdit5 -> {
                val dialog15 = EditPhBufferDialog { ph: Float ->
//                    myEdit.putString("maxMV5", String.valueOf(ph));
//                    myEdit.commit();
//
//                    maxMV5.setText(sharedPreferences.getString("maxMV5", ""));

//                    databaseHelper.updateBufferData(
//                        5,
//                        ph5.text.toString(),
//                        minMV5.text.toString(),
//                        ph.toString(),
//                        this@PhMvTable
//                    )

                    maxMV5.setText(ph.toString());

                    SharedPref.saveData(
                        this@PhMvTable,
                        SharedKeys.maxMV5 + PhActivity.DEVICE_ID,
                        ph.toString()
                    )


                }
                dialog15.show(supportFragmentManager, null)
            }

            else -> {}
        }
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onStop() {

//        deviceRef.child("Data").child("AUTOLOG").setValue(0);
//        deviceRef.child("Data").child("LOG_INTERVAL").setValue(0);

        super.onStop()
    }

    public fun webSocketInit() {
        WebSocketManager.setMessageListener { message ->

//            runOnUiThread {
//                offlineModeSwitch.isChecked = true
//                offlineModeSwitch.text = "Connected"
//            }


            runOnUiThread {
                try {
                    jsonData = JSONObject(message)
                    Log.d("JSONReceived:PHFragment", "onMessage: $message")
                    if (jsonData.has("VOLT") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {
                        monitorValTxt.text = "Monitor Val : " + jsonData.getString("VOLT")
                    }
                    if (jsonData.has("A1") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {

                        binding.a1Txt.text = "A1 Value : " + jsonData.getString("A1")

                    }
                    if (jsonData.has("A0") && jsonData.getString("DEVICE_ID") == PhActivity.DEVICE_ID) {

                        binding.a0Txt.text = "A0 Value : " + jsonData.getString("A0")

                    }
                    } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }
    }


}